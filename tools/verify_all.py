import xml.etree.ElementTree as ET
import sys
import re
sys.stdout.reconfigure(encoding='utf-8')

def get_full_path(edge, boxes):
    src = boxes[edge['src']]
    tgt = boxes[edge['tgt']]
    pts = list(edge['points'])
    if not pts:
        s_cx = src['x'] + src['w']/2
        s_cy = src['y'] + src['h']/2
        t_cx = tgt['x'] + tgt['w']/2
        t_cy = tgt['y'] + tgt['h']/2
        if abs(s_cx - t_cx) < 1:
            if s_cy < t_cy:
                p_start = (s_cx, src['y'] + src['h'])
                p_end = (t_cx, tgt['y'])
            else:
                p_start = (s_cx, src['y'])
                p_end = (t_cx, tgt['y'] + tgt['h'])
        else:
            if s_cx < t_cx:
                p_start = (src['x'] + src['w'], s_cy)
                p_end = (tgt['x'], t_cy)
            else:
                p_start = (src['x'], s_cy)
                p_end = (tgt['x'] + tgt['w'], t_cy)
        return [p_start, p_end]
    else:
        p1 = pts[0]
        s_cx = src['x'] + src['w']/2
        s_cy = src['y'] + src['h']/2
        if p1[1] > src['y'] + src['h']:
            p_start = (p1[0] if src['x'] <= p1[0] <= src['x'] + src['w'] else (src['x'] + src['w'] if p1[0] > src['x'] else src['x']), src['y'] + src['h'] if src['x'] <= p1[0] <= src['x'] + src['w'] else s_cy)
        elif p1[0] > src['x'] + src['w']:
            p_start = (src['x'] + src['w'], p1[1] if src['y'] <= p1[1] <= src['y'] + src['h'] else s_cy)
        elif p1[0] < src['x']:
            p_start = (src['x'], p1[1] if src['y'] <= p1[1] <= src['y'] + src['h'] else s_cy)
        else:
            p_start = (s_cx, src['y'])

        pn = pts[-1]
        t_cx = tgt['x'] + tgt['w']/2
        t_cy = tgt['y'] + tgt['h']/2
        if pn[1] < tgt['y']:
            p_end = (pn[0] if tgt['x'] <= pn[0] <= tgt['x'] + tgt['w'] else (tgt['x'] + tgt['w'] if pn[0] > tgt['x'] else tgt['x']), tgt['y'] if tgt['x'] <= pn[0] <= tgt['x'] + tgt['w'] else t_cy)
        elif pn[0] < tgt['x']:
            p_end = (tgt['x'], pn[1] if tgt['y'] <= pn[1] <= tgt['y'] + tgt['h'] else t_cy)
        elif pn[0] > tgt['x'] + tgt['w']:
            p_end = (tgt['x'] + tgt['w'], pn[1] if tgt['y'] <= pn[1] <= tgt['y'] + tgt['h'] else t_cy)
        else:
            p_end = (t_cx, tgt['y'] + tgt['h'])

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
            edges.append({'id': cid, 'src': src, 'tgt': tgt, 'points': pts})

    collisions = []
    for edge in edges:
        full_pts = get_full_path(edge, boxes)
        for i in range(len(full_pts) - 1):
            p1 = full_pts[i]
            p2 = full_pts[i+1]
            for bid, box in boxes.items():
                if bid == edge['src'] or bid == edge['tgt']:
                    continue
                if line_intersects_box(p1, p2, box):
                    collisions.append((edge['id'], edge['src'], edge['tgt'], bid))
    return collisions

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

        # Check child compartments: attrs, sep, methods
        assert f"{cid}_attrs" in cells, f"{cid} missing attributes compartment"
        assert f"{cid}_sep" in cells, f"{cid} missing separator line"
        assert f"{cid}_methods" in cells, f"{cid} missing methods compartment"

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

    # Check composition & aggregation
    edge_map = {(e.attrib.get("source"), e.attrib.get("target")): e for e in edges}
    # Aggregation: PhongChieu ◇--- Ghe
    assert ('c_PhongChieu', 'c_Ghe') in edge_map
    e_agg = edge_map[('c_PhongChieu', 'c_Ghe')].attrib.get("style", "")
    assert "startArrow=diamondThin" in e_agg and "startFill=0" in e_agg, "PhongChieu-Ghe must be aggregation (hollow diamond)"
    # Composition: SuatChieu ◆--- GheSuatChieu
    assert ('c_SuatChieu', 'c_GheSuatChieu') in edge_map
    e_comp1 = edge_map[('c_SuatChieu', 'c_GheSuatChieu')].attrib.get("style", "")
    assert "startArrow=diamondThin" in e_comp1 and "startFill=1" in e_comp1, "SuatChieu-GheSuatChieu must be composition (solid diamond)"
    # Composition: GiaoDich ◆--- ThanhToan
    assert ('c_GiaoDich', 'c_ThanhToan') in edge_map
    e_comp2 = edge_map[('c_GiaoDich', 'c_ThanhToan')].attrib.get("style", "")
    assert "startArrow=diamondThin" in e_comp2 and "startFill=1" in e_comp2, "GiaoDich-ThanhToan must be composition (solid diamond)"
    # Composition: GiaoDich ◆--- Ve
    assert ('c_GiaoDich', 'c_Ve') in edge_map
    e_comp3 = edge_map[('c_GiaoDich', 'c_Ve')].attrib.get("style", "")
    assert "startArrow=diamondThin" in e_comp3 and "startFill=1" in e_comp3, "GiaoDich-Ve must be composition (solid diamond)"
    print("  ✓ Single page standard 3-compartment UML classes, dimensions, empty edge labels, and relationships verified")

    c = check_collisions('SoDoLop')
    assert len(c) == 0, f"Page SoDoLop has {len(c)} collisions: {c}"
    print("  ✓ Geometry check: 0 box-edge intersections on SoDoLop (21 edges)")

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
