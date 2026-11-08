import xml.etree.ElementTree as ET
import sys
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
    diag = [d for d in tree.findall('.//diagram') if d.attrib.get('name') == diag_name][0]
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
    assert "SoDoLop_PhanTich" in pages, "Page 1 missing"
    assert "SoDoLop_ThietKe_3Tang" in pages, "Page 2 missing"

    p2 = pages["SoDoLop_ThietKe_3Tang"]
    p2_root = p2.find(".//root")
    d_db = None
    for cell in p2_root.findall("mxCell"):
        if cell.attrib.get("id") == "d_Database":
            d_db = cell
            break
    assert d_db is not None, "d_Database not found on Page 2"
    val = d_db.attrib.get("value", "")
    assert "«utility»" in val, f"Expected «utility» in d_Database, got: {val}"
    assert "+ connect(): Connection" in val, f"Expected + connect(): Connection, got: {val}"
    assert "config/database.properties" in val, f"Expected config/database.properties, got: {val}"
    print("  ✓ d_Database stereotype, method, and subtitle are correct")

    expected_snippets = {
        "v_DangNhap": ["DangNhapWindow(service)", "authenticate(service): void"],
        "v_Cinema": ["open(session): void"],
        "b_DangNhap": ["login(username, password): PhienDangNhap"],
        "b_NhanVien": ["search(keyword): List<NhanVien>", "delete(id): void"],
        "b_Phim": ["genres(): List<TheLoai>", "search(name, genre, status): List<Phim>", "create(input, image): Phim", "rename(id, name): void", "stop(id): void"],
        "b_LichChieu": ["list(date, room): List<SuatChieu>", "create(film, room, start): SuatChieu", "changeFilm(id, film): SuatChieu", "cancel(id): void"],
        "b_PhongChieu": ["search(text, type, state): List<PhongChieu>", "create(name, type, clean, state): PhongChieu", "seats(room): List<Ghe>", "addSeat(room, row, num, type, state): Ghe"],
        "b_GiaVe": ["list(): List<BangGiaVe>", "create(seat, room, period, price): BangGiaVe", "updatePrice(id, price): BangGiaVe"],
        "b_SanPham": ["search(text, inStock): List<SanPham>", "create(input): void", "updateInfo(input): void", "updatePrice(id, amount): void"],
        "b_Ve": ["rooms(): List<PhongChieu>", "shows(date, room): List<SuatChieu>", "check(ticket, type, room, show): KetQua", "confirm(ticket, room, show): ThongTin"],
        "b_BanVe": ["shows(name, date, time): List<SuatBan>", "hold(show, seats, txId): GioHang", "pay(txId, method, amount): List<ThongTin>", "cancel(txId): void"],
        "b_BaoCao": ["theoThoiGian(range): TongHop", "theoPhim(range, film): List<TheoPhim>", "theoNhanVien(range, emp): List<TheoNhanVien>", "ve(range, film, show): ThongKeVe"],
        "h_Auth": ["hash(password): String", "matches(password, stored): boolean", "laAdmin(): boolean"],
        "h_Film": ["importImage(source): String", "resolve(stored): Path", "discard(stored): void"],
        "h_Pricing": ["positive(value, message): BigDecimal"],
        "h_Admission": ["read(file): String", "load(): ThoiGianVaoPhong"],
        "h_Sales": ["preview(info): BufferedImage", "print(g, format, page): int"],
        "h_Revenue": ["ngay(date): KhoangThongKe", "thang(month): KhoangThongKe", "nam(year): KhoangThongKe"],
        "d_TaiKhoan": ["findByUsername(user): Optional<TaiKhoan>"],
        "d_NhanVien": ["search(keyword): List<NhanVien>", "insert(emp, hash, role): void", "update(emp): void", "delete(id): void"],
        "d_PhimTheLoai": ["search(name, genre, status): List<Phim>", "insert(film): void", "stop(id): void", "all(): List<TheLoai>"],
        "d_SuatChieu": ["list(date, room): List<SuatChieu>", "create(film, room, start, holidays): SuatChieu", "modify(id, edit, holidays): SuatChieu", "cancel(id): void"],
        "d_PhongGhe": ["search(key, type, state): List<PhongChieu>", "insert(room): void", "list(room): List<Ghe>", "insert(room, row, num, type, state): Ghe"],
        "d_BangGia": ["all(): List<BangGiaVe>", "insert(seat, room, period, price): int", "updatePrice(id, price): void"],
        "d_SanPham": ["search(key, inStock): List<SanPham>", "insert(product): void", "updateInfo(product): void", "updatePrice(id, price): void"],
        "d_Ve": ["inspect(code, type): Optional<ThongTin>", "confirm(maVe, truoc, sau, val): ThongTin"],
        "d_BanVe": ["search(name, date, time): List<SuatBan>", "hold(show, seats, staff, id, min): GioHang", "pay(id, method, amount): List<ThongTin>", "cancel(id): void"],
        "d_BaoCao": ["theoThoiGian(range): TongHop", "theoPhim(range, film): List<TheoPhim>", "theoNhanVien(range, emp): List<TheoNhanVien>", "ve(range, film, show): ThongKeVe"]
    }

    cells_by_id = {c.attrib.get("id"): c for c in p2_root.findall("mxCell")}
    for cid, snips in expected_snippets.items():
        assert cid in cells_by_id, f"Cell {cid} not found"
        cell_val = cells_by_id[cid].attrib.get("value", "")
        for s in snips:
            assert s in cell_val, f"Snippet '{s}' not found in {cid}: {cell_val}"
    print("  ✓ All exact Java method names on Page 2 verified")

    c1 = check_collisions('SoDoLop_PhanTich')
    c2 = check_collisions('SoDoLop_ThietKe_3Tang')
    assert len(c1) == 0, f"Page 1 has {len(c1)} collisions: {c1}"
    assert len(c2) == 0, f"Page 2 has {len(c2)} collisions: {c2}"
    print("  ✓ Geometry check: 0 box-edge intersections on Page 1 and Page 2")

    import re
    with open("docs/SoDoLop_RapChieuPhim.drawio", encoding="utf-8") as f:
        content = f.read()
    html_tags = re.findall(r'&lt;/?(b|i|br|span|div|hr|font)\b[^&]*?&gt;', content, re.IGNORECASE)
    assert len(html_tags) == 0, f"Found HTML tags in diagram: {set(html_tags)}"
    print("  ✓ Zero HTML tag leakage verified across all pages")

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
