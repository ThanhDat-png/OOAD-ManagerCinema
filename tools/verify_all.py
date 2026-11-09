import xml.etree.ElementTree as ET
import sys
import re
sys.stdout.reconfigure(encoding='utf-8')

def parse_style(style_str):
    props = {}
    for part in style_str.split(';'):
        if '=' in part:
            k, v = part.split('=', 1)
            props[k.strip()] = v.strip()
    return props

def get_full_path(edge, boxes):
    src = boxes[edge['src']]
    tgt = boxes[edge['tgt']]
    pts = list(edge['points'])
    style = parse_style(edge.get('style', ''))

    if 'exitX' in style and 'exitY' in style:
        ex = float(style['exitX'])
        ey = float(style['exitY'])
        p_start = (round(src['x'] + ex * src['w'], 2), round(src['y'] + ey * src['h'], 2))
    else:
        s_cx = src['x'] + src['w']/2
        s_cy = src['y'] + src['h']/2
        p_start = (s_cx, s_cy)

    if 'entryX' in style and 'entryY' in style:
        enx = float(style['entryX'])
        eny = float(style['entryY'])
        p_end = (round(tgt['x'] + enx * tgt['w'], 2), round(tgt['y'] + eny * tgt['h'], 2))
    else:
        t_cx = tgt['x'] + tgt['w']/2
        t_cy = tgt['y'] + tgt['h']/2
        p_end = (t_cx, t_cy)

    if not pts:
        if abs(p_start[0] - p_end[0]) < 1e-2 or abs(p_start[1] - p_end[1]) < 1e-2:
            return [p_start, p_end]
        else:
            mid_x = round((p_start[0] + p_end[0]) / 2, 2)
            return [p_start, (mid_x, p_start[1]), (mid_x, p_end[1]), p_end]
    else:
        return [p_start] + pts + [p_end]

def line_intersects_box(p1, p2, box, tolerance=1e-5):
    bx1, by1 = box['x'], box['y']
    bx2, by2 = box['x'] + box['w'], box['y'] + box['h']
    x1, y1 = p1
    x2, y2 = p2
    if abs(x1 - x2) < tolerance:
        x = x1
        if bx1 + tolerance < x < bx2 - tolerance:
            min_y, max_y = min(y1, y2), max(y1, y2)
            if max(min_y, by1) < min(max_y, by2) - tolerance:
                return True
    elif abs(y1 - y2) < tolerance:
        y = y1
        if by1 + tolerance < y < by2 - tolerance:
            min_x, max_x = min(x1, x2), max(x1, x2)
            if max(min_x, bx1) < min(max_x, bx2) - tolerance:
                return True
    return False

def check_collisions(diag_name):
    tree = ET.parse('docs/SoDoLop_RapChieuPhim.drawio')
    diags = [d for d in tree.findall('.//diagram') if d.attrib.get('name') == diag_name]
    assert len(diags) > 0, f"Diagram {diag_name} not found"
    diag = diags[0]
    root = diag.find('.//root')
    boxes = {}
    edges = []
    for cell in root.findall('mxCell'):
        cid = cell.attrib.get('id')
        parent = cell.attrib.get('parent')
        geom = cell.find('mxGeometry')
        if geom is None:
            continue
        if cell.attrib.get('vertex') == '1' and parent == '1':
            x = float(geom.attrib.get('x', 0))
            y = float(geom.attrib.get('y', 0))
            w = float(geom.attrib.get('width', 0))
            h = float(geom.attrib.get('height', 0))
            style = cell.attrib.get('style', '')
            if 'swimlane' in style and (w > 1000 or h > 500):
                continue
            boxes[cid] = {'x': x, 'y': y, 'w': w, 'h': h, 'id': cid}
        elif cell.attrib.get('edge') == '1':
            src = cell.attrib.get('source')
            tgt = cell.attrib.get('target')
            pts = []
            arr = geom.find('Array')
            if arr is not None:
                for pt in arr.findall('mxPoint'):
                    pts.append((float(pt.attrib.get('x')), float(pt.attrib.get('y'))))
            edges.append({'id': cid, 'src': src, 'tgt': tgt, 'points': pts, 'style': cell.attrib.get('style', '')})

    collisions = []
    non_ortho = []
    for edge in edges:
        full_pts = get_full_path(edge, boxes)
        for i in range(len(full_pts) - 1):
            p1 = full_pts[i]
            p2 = full_pts[i+1]
            dx = abs(p1[0] - p2[0])
            dy = abs(p1[1] - p2[1])
            if dx > 0.5 and dy > 0.5:
                non_ortho.append((edge['id'], p1, p2))
            for bid, box in boxes.items():
                if bid == edge['src'] or bid == edge['tgt']:
                    continue
                if line_intersects_box(p1, p2, box):
                    collisions.append((edge['id'], edge['src'], edge['tgt'], bid))
    return collisions, non_ortho

def verify_diagram():
    print("=== 1. VERIFYING DIAGRAM ===")
    tree = ET.parse("docs/SoDoLop_RapChieuPhim.drawio")
    pages = {d.attrib.get("name"): d for d in tree.findall(".//diagram")}
    assert "SoDoLop" in pages, "Page 'SoDoLop' missing"
    assert len(pages) == 1, f"Expected exactly 1 diagram page for Data/Entity classes, found {len(pages)}"

    # Verify single page (SoDoLop) structure & UML compliance
    p = pages["SoDoLop"]
    p_root = p.find(".//root")
    cells = {c.attrib.get("id"): c for c in p_root.findall("mxCell")}

    expected_classes = [
        'c_VaiTro', 'c_TaiKhoan', 'c_NhanVien', 'c_TheLoai', 'c_Phim',
        'c_LoaiPhong', 'c_PhongChieu', 'c_Ghe', 'c_LoaiGhe', 'c_LoaiThoiDiem',
        'c_SuatChieu', 'c_GheSuatChieu', 'c_BangGiaVe', 'c_SanPham',
        'c_ThanhToan', 'c_GiaoDich', 'c_Ve'
    ]
    for cid in expected_classes:
        assert cid in cells, f"Class diagram missing class {cid}"
        cls_cell = cells[cid]
        c_style = cls_cell.attrib.get("style", "")
        assert "swimlane" in c_style, f"{cid} must be a UML swimlane"
        assert "fillColor=#FFFFFF" in c_style, f"{cid} must have fillColor=#FFFFFF"
        assert "strokeColor=#000000" in c_style, f"{cid} must have strokeColor=#000000"
        assert "fontColor=#000000" in c_style, f"{cid} must have fontColor=#000000"
        assert "rounded=0" in c_style, f"{cid} must have rounded=0 (sharp corners)"
        assert "html=0" in c_style, f"{cid} must have html=0"

        # Check startSize accommodating multi-line headers
        m = re.search(r'startSize=(\d+)', c_style)
        assert m, f"startSize missing in {cid}"
        start_size = int(m.group(1))
        hdr_lines = len(cls_cell.attrib.get("value", "").split("\n"))
        if hdr_lines == 1:
            assert start_size >= 28, f"{cid} startSize {start_size} < 28 for single-line header"
        else:
            assert start_size >= 40, f"{cid} startSize {start_size} < 40 for multi-line header"

        # Check child compartments: attrs, sep, methods
        assert f"{cid}_attrs" in cells, f"{cid} missing attributes compartment"
        assert f"{cid}_sep" in cells, f"{cid} missing separator line"
        assert f"{cid}_methods" in cells, f"{cid} missing methods compartment"

        # Check attrs compartment styling and height clearance
        attrs_cell = cells[f"{cid}_attrs"]
        a_style = attrs_cell.attrib.get("style", "")
        assert "align=left" in a_style, f"{cid}_attrs must have align=left"
        assert "spacingLeft=8" in a_style, f"{cid}_attrs must have spacingLeft=8"
        assert "html=0" in a_style, f"{cid}_attrs must have html=0"
        a_geom = attrs_cell.find("mxGeometry")
        a_h = float(a_geom.attrib.get("height", 0))
        a_lines = len([x for x in attrs_cell.attrib.get("value", "").split("\n") if x.strip()])
        assert a_h >= a_lines * 15, f"{cid}_attrs height {a_h} is insufficient for {a_lines} lines"

        # Check methods compartment styling and height clearance
        meth_cell = cells[f"{cid}_methods"]
        m_style = meth_cell.attrib.get("style", "")
        assert "align=left" in m_style, f"{cid}_methods must have align=left"
        assert "spacingLeft=8" in m_style, f"{cid}_methods must have spacingLeft=8"
        assert "html=0" in m_style, f"{cid}_methods must have html=0"
        m_geom = meth_cell.find("mxGeometry")
        m_h = float(m_geom.attrib.get("height", 0))
        m_lines = len([x for x in meth_cell.attrib.get("value", "").split("\n") if x.strip()])
        assert m_h >= m_lines * 15, f"{cid}_methods height {m_h} is insufficient for {m_lines} lines"

        # Check generous bounding box dimensions so no text overflows
        geom = cls_cell.find("mxGeometry")
        w = float(geom.attrib.get("width", 0))
        h = float(geom.attrib.get("height", 0))
        assert w >= 210, f"{cid} width {w} is too narrow"
        assert h >= 100, f"{cid} height {h} is too short"

    # Title must not be on canvas
    assert "p1_title" not in cells, "p1_title must not be present on canvas"

    # Verify relationships (Aggregation & Composition markers, empty middle labels)
    edges = [c for c in p_root.findall("mxCell") if c.attrib.get("edge") == "1"]
    assert len(edges) == 21, f"Expected 21 edges on Class Diagram, found {len(edges)}"
    for e in edges:
        assert e.attrib.get("value", "") == "", f"Edge {e.attrib.get('id')} has non-empty label: {e.attrib.get('value')}"

    # Check composition, aggregation, and directed association (user arrow style)
    edge_map = {(e.attrib.get("source"), e.attrib.get("target")): e for e in edges}
    # Aggregation: PhongChieu ◇--- Ghe (hollow diamond at source, no arrow at target)
    assert ('c_PhongChieu', 'c_Ghe') in edge_map
    e_agg = edge_map[('c_PhongChieu', 'c_Ghe')].attrib.get("style", "")
    assert "startArrow=diamondThin" in e_agg and "startFill=0" in e_agg and "endArrow=none" in e_agg, "PhongChieu-Ghe must be aggregation (hollow diamond, no end arrow)"
    # Composition: SuatChieu ◆--- GheSuatChieu (solid diamond at source, no arrow at target)
    assert ('c_SuatChieu', 'c_GheSuatChieu') in edge_map
    e_comp1 = edge_map[('c_SuatChieu', 'c_GheSuatChieu')].attrib.get("style", "")
    assert "startArrow=diamondThin" in e_comp1 and "startFill=1" in e_comp1 and "endArrow=none" in e_comp1, "SuatChieu-GheSuatChieu must be composition (solid diamond, no end arrow)"
    # Composition: GiaoDich ◆--- ThanhToan
    assert ('c_GiaoDich', 'c_ThanhToan') in edge_map
    e_comp2 = edge_map[('c_GiaoDich', 'c_ThanhToan')].attrib.get("style", "")
    assert "startArrow=diamondThin" in e_comp2 and "startFill=1" in e_comp2 and "endArrow=none" in e_comp2, "GiaoDich-ThanhToan must be composition (solid diamond, no end arrow)"
    # Composition: GiaoDich ◆--- Ve
    assert ('c_GiaoDich', 'c_Ve') in edge_map
    e_comp3 = edge_map[('c_GiaoDich', 'c_Ve')].attrib.get("style", "")
    assert "startArrow=diamondThin" in e_comp3 and "startFill=1" in e_comp3 and "endArrow=none" in e_comp3, "GiaoDich-Ve must be composition (solid diamond, no end arrow)"

    # Association: All other 17 edges must have directed open arrow head (endArrow=open)
    special_edges = {('c_PhongChieu', 'c_Ghe'), ('c_SuatChieu', 'c_GheSuatChieu'), ('c_GiaoDich', 'c_ThanhToan'), ('c_GiaoDich', 'c_Ve')}
    for (src, tgt), e in edge_map.items():
        if (src, tgt) not in special_edges:
            style = e.attrib.get("style", "")
            assert "startArrow=none" in style, f"Association {src}->{tgt} must have startArrow=none"
            assert "endArrow=open" in style, f"Association {src}->{tgt} must have endArrow=open"
            assert "endFill=0" in style, f"Association {src}->{tgt} must have endFill=0"

    # Verify source and target multiplicities on all 21 edges
    expected_multiplicities = {
        ('c_VaiTro', 'c_TaiKhoan'): ('1', '0..*'),
        ('c_NhanVien', 'c_TaiKhoan'): ('1', '0..1'),
        ('c_TheLoai', 'c_Phim'): ('1..*', '1..*'),
        ('c_LoaiPhong', 'c_PhongChieu'): ('1', '0..*'),
        ('c_PhongChieu', 'c_Ghe'): ('1', '1..*'),
        ('c_LoaiGhe', 'c_Ghe'): ('1', '0..*'),
        ('c_NhanVien', 'c_GiaoDich'): ('1', '0..*'),
        ('c_Phim', 'c_SuatChieu'): ('1', '0..*'),
        ('c_LoaiThoiDiem', 'c_SuatChieu'): ('1', '0..*'),
        ('c_SuatChieu', 'c_GheSuatChieu'): ('1', '1..*'),
        ('c_PhongChieu', 'c_SuatChieu'): ('1', '0..*'),
        ('c_Ghe', 'c_GheSuatChieu'): ('1', '0..*'),
        ('c_LoaiPhong', 'c_BangGiaVe'): ('1', '0..*'),
        ('c_LoaiGhe', 'c_BangGiaVe'): ('1', '0..*'),
        ('c_LoaiThoiDiem', 'c_BangGiaVe'): ('1', '0..*'),
        ('c_BangGiaVe', 'c_GheSuatChieu'): ('0..1', '0..*'),
        ('c_GiaoDich', 'c_ThanhToan'): ('1', '0..1'),
        ('c_GiaoDich', 'c_Ve'): ('1', '1..*'),
        ('c_GheSuatChieu', 'c_Ve'): ('1', '0..1'),
        ('c_GiaoDich', 'c_GheSuatChieu'): ('0..1', '0..*'),
        ('c_BangGiaVe', 'c_Ve'): ('1', '0..*'),
    }
    for (src, tgt), (s_exp, t_exp) in expected_multiplicities.items():
        assert (src, tgt) in edge_map, f"Edge from {src} to {tgt} missing"
        e = edge_map[(src, tgt)]
        eid = e.attrib.get("id")
        s_lbl = cells.get(f"{eid}_s")
        t_lbl = cells.get(f"{eid}_t")
        assert s_lbl is not None and s_lbl.attrib.get("value") == s_exp, f"Edge {eid} source mult {s_lbl.attrib.get('value') if s_lbl else None} != {s_exp}"
        assert t_lbl is not None and t_lbl.attrib.get("value") == t_exp, f"Edge {eid} target mult {t_lbl.attrib.get('value') if t_lbl else None} != {t_exp}"
    print("  ✓ Single page standard 3-compartment UML classes, dimensions, empty edge labels, multiplicities, and relationships verified")

    collisions, non_ortho = check_collisions('SoDoLop')
    assert len(non_ortho) == 0, f"Page SoDoLop has non-orthogonal segments: {non_ortho}"
    assert len(collisions) == 0, f"Page SoDoLop has {len(collisions)} collisions: {collisions}"
    print("  ✓ Geometry check: 0 box-edge intersections and 100% strictly orthogonal segments on SoDoLop (21 edges)")

    with open("docs/SoDoLop_RapChieuPhim.drawio", encoding="utf-8") as f:
        content = f.read()

    # Assert that no fake Unicode dashes and no subtitle parentheses appear in diagram
    assert "─" not in content, "Found fake Unicode dash line '─' in diagram"
    assert re.search(r'(?:&#10;|\n)\s*\([^)]+\)', content) is None, "Found subtitle in parentheses in diagram"
    assert re.search(r'\([^)]*(?:UC\d\d|cinema\.controller|database\.properties|Quản lý|Màn hình|Lập lịch|Phòng và|Giá vé|Soát vé|Bán vé|Thống kê|Khung chính|Thư viện|Bảo mật|Lưu trữ|Quy tắc|Xác thực|Định dạng|Lọc)[^)]*\)', content) is None, "Found subtitle keyword pattern in diagram"
    for kw in ['(Màn hình', '(Quản lý', '(Lập lịch', '(Phòng và', '(Giá vé', '(Soát vé', '(Bán vé', '(Thống kê', '(Khung chính', '(Thư viện', '(cinema.controller', '(Bảo mật', '(Lưu trữ', '(Quy tắc', '(Xác thực', '(Định dạng', '(Lọc', '(config/database.properties']:
        assert kw not in content, f"Found subtitle keyword '{kw}' in diagram"
    print("  ✓ Zero fake Unicode dashes '─' and zero subtitle parentheses verified across diagram")

    html_tags = re.findall(r'&lt;/?(b|i|br|span|div|hr|font)\b[^&]*?&gt;', content, re.IGNORECASE)
    assert len(html_tags) == 0, f"Found HTML tags in diagram: {set(html_tags)}"
    print("  ✓ Zero HTML tag leakage verified across diagram")

def verify_statuses():
    print("\n=== 2. VERIFYING STATUS VALUES IN DOCS ===")
    status_rules = [
        ("TAIKHOAN.TrangThai", ["Đang hoạt động", "Ngưng hoạt động"]),
        ("PHIM.TrangThai", ["Sắp chiếu", "Đang chiếu", "Ngừng chiếu"]),
        ("PHONGCHIEU.TrangThai", ["Đang hoạt động", "Tạm ngừng hoạt động", "Bảo trì"]),
        ("GHE.TrangThai", ["Sẵn sàng sử dụng", "Bảo trì"]),
        ("SUATCHIEU.TrangThai", ["Sắp chiếu", "Đang chiếu", "Đã chiếu", "Đã hủy"]),
        ("GHE_SUATCHIEU.TrangThai", ["Trống", "Đang giữ", "Đã bán", "Không khả dụng"]),
        ("GIAODICH.TrangThai", ["Chờ thanh toán", "Đang thanh toán", "Đã thanh toán", "Đã hủy", "Hết hạn"]),
        ("VE.TrangThai", ["Chưa sử dụng", "Đã sử dụng"]),
        ("SANPHAM.TinhTrangKinhDoanh", ["Đang kinh doanh", "Ngừng kinh doanh"]),
    ]

    for doc_path in ["docs/danh-sach-lop-nghiep-vu-du-kien.md", "docs/kiem-tra-erd-codebase-va-mo-ta-csdl.md"]:
        content = open(doc_path, encoding="utf-8").read()
        for field, vals in status_rules:
            for val in vals:
                assert val in content, f"Status value '{val}' for {field} not found in {doc_path}"
        print(f"  ✓ {doc_path}: ALL 9 STATUS VALUE SETS MATCH")

if __name__ == "__main__":
    verify_diagram()
    verify_statuses()
    print("\nALL VERIFICATIONS PASSED SUCCESSFULLY!")
