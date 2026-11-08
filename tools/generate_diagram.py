import xml.etree.ElementTree as ET

def build_drawio():
    mxfile = ET.Element('mxfile', {'host': 'app.diagrams.net', 'pages': '2', 'agent': 'OOAD-Cinema'})

    # =========================================================================
    # PAGE 1: SoDoLop_PhanTich (Analysis Class Diagram)
    # Strictly Black and White, sharp rectangular corners, plain text UML
    # =========================================================================
    diag1 = ET.SubElement(mxfile, 'diagram', {'id': 'diag_analysis', 'name': 'SoDoLop_PhanTich'})
    model1 = ET.SubElement(diag1, 'mxGraphModel', {
        'dx': '2000', 'dy': '1400', 'grid': '1', 'gridSize': '10', 'guides': '1',
        'tooltips': '1', 'connect': '1', 'arrows': '1', 'fold': '1', 'page': '1',
        'pageScale': '1', 'pageWidth': '2600', 'pageHeight': '1400', 'math': '0', 'shadow': '0'
    })
    root1 = ET.SubElement(model1, 'root')
    ET.SubElement(root1, 'mxCell', {'id': '0'})
    ET.SubElement(root1, 'mxCell', {'id': '1', 'parent': '0'})

    # Title Banner for Page 1 (Simple, clean, black text, no colored banner)
    title_box1 = ET.SubElement(root1, 'mxCell', {
        'id': 'p1_title',
        'value': 'SƠ ĐỒ LỚP Ở MỨC PHÂN TÍCH - HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM',
        'style': 'text;html=0;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontSize=14;fontStyle=1;fontColor=#000000;',
        'vertex': '1', 'parent': '1'
    })
    ET.SubElement(title_box1, 'mxGeometry', {'x': '50', 'y': '20', 'width': '2450', 'height': '40', 'as': 'geometry'})

    def add_class_box(root, cid, name, attrs, methods, x, y, w, h_attr, h_meth):
        total_h = 28 + h_attr + 8 + h_meth
        hdr_style = "swimlane;fontStyle=1;align=center;verticalAlign=top;childLayout=stackLayout;horizontal=1;startSize=28;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;marginBottom=0;html=0;whiteSpace=wrap;rounded=0;fillColor=#FFFFFF;strokeColor=#000000;strokeWidth=1;fontSize=12;fontColor=#000000;"
        cls_cell = ET.SubElement(root, 'mxCell', {
            'id': cid,
            'value': name,
            'style': hdr_style,
            'vertex': '1', 'parent': '1'
        })
        ET.SubElement(cls_cell, 'mxGeometry', {'x': str(x), 'y': str(y), 'width': str(w), 'height': str(total_h), 'as': 'geometry'})

        # Attributes
        attr_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_attrs',
            'value': '\n'.join(attrs),
            'style': 'text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=8;spacingRight=8;spacingTop=4;spacingBottom=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=0;fontSize=11;fontColor=#000000;',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(attr_cell, 'mxGeometry', {'y': '28', 'width': str(w), 'height': str(h_attr), 'as': 'geometry'})

        # Separator line
        sep_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_sep',
            'value': '',
            'style': 'line;strokeWidth=1;fillColor=none;align=left;verticalAlign=middle;spacingTop=-1;spacingLeft=3;spacingRight=3;rotatable=0;labelPosition=right;points=[];portConstraint=eastwest;strokeColor=#000000;',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(sep_cell, 'mxGeometry', {'y': str(28 + h_attr), 'width': str(w), 'height': '8', 'as': 'geometry'})

        # Methods
        meth_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_methods',
            'value': '\n'.join(methods),
            'style': 'text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=8;spacingRight=8;spacingTop=4;spacingBottom=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=0;fontSize=11;fontColor=#000000;',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(meth_cell, 'mxGeometry', {'y': str(36 + h_attr), 'width': str(w), 'height': str(h_meth), 'as': 'geometry'})
        return cls_cell

    # Define 17 Domain Classes for Page 1
    # Row 1 (y=100): VaiTro, TaiKhoan, NhanVien, TheLoai, Phim, LoaiPhong, PhongChieu, Ghe, LoaiGhe
    add_class_box(root1, 'c_VaiTro', '«enumeration»\nVaiTro',
                  ['- ADMIN: VaiTro', '- NHAN_VIEN: VaiTro', '- maVaiTro: int', '- tenVaiTro: String'],
                  ['+ getTen(): String', '+ fromDatabase(ten: String): VaiTro'],
                  60, 100, 200, 75, 45)

    add_class_box(root1, 'c_TaiKhoan', 'TaiKhoan',
                  ['- maTK: int', '- tenDangNhap: String', '- matKhauHash: String', '- trangThai: String'],
                  ['+ dangHoatDong(): boolean', '+ doiMatKhau(mkMoi: String): void', '+ kiemTraDangNhap(mk: String): boolean'],
                  310, 100, 220, 75, 60)

    add_class_box(root1, 'c_NhanVien', 'NhanVien',
                  ['- maNV: String', '- hoTen: String', '- ngaySinh: LocalDate', '- soDienThoai: String', '- email: String', '- diaChi: String', '- chucVu: String'],
                  ['+ capNhatThongTin(hoTen, sdt, email, diaChi, chucVu): void', '+ getHoTen(): String'],
                  580, 100, 250, 130, 45)

    add_class_box(root1, 'c_TheLoai', 'TheLoai',
                  ['- maTheLoai: int', '- tenTheLoai: String'],
                  ['+ getTen(): String'],
                  900, 100, 180, 45, 30)

    add_class_box(root1, 'c_Phim', 'Phim',
                  ['- maPhim: String', '- tenPhim: String', '- thoiLuong: int', '- daoDien: String', '- ngayKhoiChieu: LocalDate', '- gioiHanTuoi: int', '- poster: String', '- trangThai: String', '- dinhDangChieu: String'],
                  ['+ ngungChieu(): void', '+ dangChieu(): boolean', '+ withTheLoai(theLoai: List<TheLoai>): Phim'],
                  1140, 100, 250, 160, 60)

    add_class_box(root1, 'c_LoaiPhong', 'LoaiPhong',
                  ['- maLoaiPhong: int', '- tenLoaiPhong: String', '- dinhDangChieu: String'],
                  ['+ getDinhDang(): String'],
                  1460, 100, 200, 60, 30)

    add_class_box(root1, 'c_PhongChieu', 'PhongChieu',
                  ['- maPhong: String', '- tenPhong: String', '- thoiGianDonPhong: int', '- trangThai: String', '/ sucChua: int'],
                  ['+ tinhSucChua(): int', '+ dangHoatDong(): boolean'],
                  1720, 100, 220, 95, 45)

    add_class_box(root1, 'c_Ghe', 'Ghe',
                  ['- maGhe: int', '- hangGhe: String', '- soGhe: int', '- trangThai: String'],
                  ['+ sanSangSuDung(): boolean', '+ chuyenBaoTri(): void'],
                  2000, 100, 200, 75, 45)

    add_class_box(root1, 'c_LoaiGhe', 'LoaiGhe',
                  ['- maLoaiGhe: int', '- tenLoaiGhe: String'],
                  ['+ getTen(): String'],
                  2260, 100, 180, 45, 30)

    # Row 2 (y=500): LoaiThoiDiem, SuatChieu, GheSuatChieu, BangGiaVe
    add_class_box(root1, 'c_LoaiThoiDiem', 'LoaiThoiDiem',
                  ['- maLoaiThoiDiem: int', '- tenLoaiThoiDiem: String'],
                  ['+ getTen(): String'],
                  900, 500, 180, 45, 30)

    add_class_box(root1, 'c_SuatChieu', 'SuatChieu',
                  ['- maSuat: int', '- thoiGianBatDau: LocalDateTime', '- thoiGianTraPhong: LocalDateTime', '- trangThai: String'],
                  ['+ kiemTraKhungGio(batDau, ketThuc): boolean', '+ coTheBanVe(): boolean', '+ huySuat(): void'],
                  1140, 500, 250, 75, 60)

    add_class_box(root1, 'c_GheSuatChieu', 'GheSuatChieu',
                  ['- trangThai: String', '- thoiGianHetHanGiu: LocalDateTime', '- giaVeChot: BigDecimal'],
                  ['+ kiemTraHetHanGiu(): boolean', '+ giuGhe(hanGiu: LocalDateTime): void', '+ xacNhanBan(): void', '+ giaiPhongGhe(): void'],
                  1460, 500, 260, 60, 75)

    add_class_box(root1, 'c_BangGiaVe', 'BangGiaVe',
                  ['- maGia: int', '- giaVe: BigDecimal'],
                  ['+ capNhatGia(giaMoi: BigDecimal): void'],
                  1880, 500, 220, 45, 30)

    # Row 3 (y=900): SanPham, ThanhToan, GiaoDich, Ve
    add_class_box(root1, 'c_SanPham', 'SanPham',
                  ['- maSP: String', '- tenSP: String', '- moTa: String', '- donViTinh: String', '- giaBan: BigDecimal', '- soLuongTon: int', '- tinhTrangKinhDoanh: String'],
                  ['+ capNhatTonKho(soLuongThayDoi: int): void', '+ conHang(): boolean'],
                  60, 900, 220, 130, 45)

    add_class_box(root1, 'c_ThanhToan', 'ThanhToan',
                  ['- maThanhToan: int', '- phuongThuc: String', '- soTien: BigDecimal', '- thoiGianThanhToan: LocalDateTime'],
                  ['+ xacNhanThanhToan(): boolean'],
                  330, 900, 220, 75, 30)

    add_class_box(root1, 'c_GiaoDich', 'GiaoDich',
                  ['- maGiaoDich: String', '- thoiGianTao: LocalDateTime', '- tongTien: BigDecimal', '- trangThai: String'],
                  ['+ tinhTongTien(): BigDecimal', '+ hoanTatGiaoDich(): void', '+ huyGiaoDich(): void'],
                  600, 900, 250, 75, 60)

    add_class_box(root1, 'c_Ve', 'Ve',
                  ['- maVe: String', '- maQR: String', '- giaVe: BigDecimal', '- trangThai: String', '- thoiGianPhatHanh: LocalDateTime', '- hangGheLucPhatHanh: String', '- soGheLucPhatHanh: int', '- tenLoaiGheLucPhatHanh: String'],
                  ['+ kiemTraHopLe(): boolean', '+ danhDauSuDung(): void'],
                  930, 900, 270, 150, 45)

    # Edge helper for Page 1
    edge_cnt1 = 0
    def add_p1_edge(root, source, target, s_mult='', t_mult='', label='', start_arrow='none', end_arrow='none', start_fill='0', end_fill='0', start_size='14', end_size='14', dashed='0', points=None):
        nonlocal edge_cnt1
        edge_cnt1 += 1
        eid = f'edge_{edge_cnt1}'
        style = f"edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=0;strokeWidth=1;strokeColor=#000000;dashed={dashed};startArrow={start_arrow};endArrow={end_arrow};startFill={start_fill};endFill={end_fill};startSize={start_size};endSize={end_size};"
        edge_cell = ET.SubElement(root, 'mxCell', {
            'id': eid,
            'value': label,
            'style': style,
            'edge': '1', 'parent': '1',
            'source': source, 'target': target
        })
        geom = ET.SubElement(edge_cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})
        if points:
            arr = ET.SubElement(geom, 'Array', {'as': 'points'})
            for px, py in points:
                ET.SubElement(arr, 'mxPoint', {'x': str(px), 'y': str(py)})

        if s_mult:
            lbl_s = ET.SubElement(root, 'mxCell', {
                'id': f'{eid}_s',
                'value': s_mult,
                'style': 'edgeLabel;html=0;align=center;verticalAlign=middle;resizable=0;points=[];fontSize=11;fontColor=#000000;',
                'vertex': '1', 'connectable': '0', 'parent': eid
            })
            ET.SubElement(lbl_s, 'mxGeometry', {'x': '-0.8', 'relative': '1', 'as': 'geometry'})

        if t_mult:
            lbl_t = ET.SubElement(root, 'mxCell', {
                'id': f'{eid}_t',
                'value': t_mult,
                'style': 'edgeLabel;html=0;align=center;verticalAlign=middle;resizable=0;points=[];fontSize=11;fontColor=#000000;',
                'vertex': '1', 'connectable': '0', 'parent': eid
            })
            ET.SubElement(lbl_t, 'mxGeometry', {'x': '0.8', 'relative': '1', 'as': 'geometry'})

        return edge_cell

    # Page 1 Relationships (Orthogonal, Black & White)
    # 1. VaiTro - TaiKhoan (Row 1 horizontal)
    add_p1_edge(root1, 'c_VaiTro', 'c_TaiKhoan', s_mult='1', t_mult='0..*', label='phanQuyen')

    # 2. NhanVien - TaiKhoan (Row 1 horizontal)
    add_p1_edge(root1, 'c_NhanVien', 'c_TaiKhoan', s_mult='1', t_mult='0..1', label='soHuu')

    # 3. TheLoai - Phim (Row 1 horizontal)
    add_p1_edge(root1, 'c_TheLoai', 'c_Phim', s_mult='1..*', t_mult='1..*', label='thuocTheLoai')

    # 4. LoaiPhong - PhongChieu (Row 1 horizontal)
    add_p1_edge(root1, 'c_LoaiPhong', 'c_PhongChieu', s_mult='1', t_mult='0..*', label='thuocLoai')

    # 5. PhongChieu - Ghe (Aggregation: hollow diamond at PhongChieu)
    add_p1_edge(root1, 'c_PhongChieu', 'c_Ghe', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='0', start_size='16')

    # 6. LoaiGhe - Ghe (Row 1 horizontal)
    add_p1_edge(root1, 'c_LoaiGhe', 'c_Ghe', s_mult='1', t_mult='0..*', label='dinhDanhLoai')

    # 7. NhanVien - GiaoDich (Vertical straight line through open corridor)
    add_p1_edge(root1, 'c_NhanVien', 'c_GiaoDich', s_mult='1', t_mult='0..*', label='thucHien')

    # 8. Phim - SuatChieu (Vertical straight line from Phim to SuatChieu)
    add_p1_edge(root1, 'c_Phim', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='chieuTrong')

    # 9. LoaiThoiDiem - SuatChieu (Row 2 horizontal)
    add_p1_edge(root1, 'c_LoaiThoiDiem', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='apDung')

    # 10. SuatChieu - GheSuatChieu (Composition: solid black diamond at SuatChieu)
    add_p1_edge(root1, 'c_SuatChieu', 'c_GheSuatChieu', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 11. PhongChieu - SuatChieu (Routed via Corridor 1 at y=430)
    add_p1_edge(root1, 'c_PhongChieu', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='dienRaTai', points=[(1780, 430), (1330, 430)])

    # 12. Ghe - GheSuatChieu (Routed via Corridor 1 at y=460)
    add_p1_edge(root1, 'c_Ghe', 'c_GheSuatChieu', s_mult='1', t_mult='0..*', label='thamGia', points=[(2060, 460), (1660, 460)])

    # 13. LoaiPhong - BangGiaVe (Routed via Corridor 1 at y=390)
    add_p1_edge(root1, 'c_LoaiPhong', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(1560, 390), (1940, 390)])

    # 14. LoaiGhe - BangGiaVe (Routed via Corridor 1 at y=410)
    add_p1_edge(root1, 'c_LoaiGhe', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(2310, 410), (2020, 410)])

    # 15. LoaiThoiDiem - BangGiaVe (Routed via Corridor 2 at y=710)
    add_p1_edge(root1, 'c_LoaiThoiDiem', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(990, 710), (1920, 710)])

    # 16. BangGiaVe - GheSuatChieu (Row 2 horizontal)
    add_p1_edge(root1, 'c_BangGiaVe', 'c_GheSuatChieu', s_mult='0..1', t_mult='0..*', label='chotGia')

    # 17. GiaoDich - ThanhToan (Composition: solid black diamond at GiaoDich)
    add_p1_edge(root1, 'c_GiaoDich', 'c_ThanhToan', s_mult='1', t_mult='0..1', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 18. GiaoDich - Ve (Composition: solid black diamond at GiaoDich)
    add_p1_edge(root1, 'c_GiaoDich', 'c_Ve', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 19. GheSuatChieu - Ve (Routed via Corridor 2 at y=750)
    add_p1_edge(root1, 'c_GheSuatChieu', 'c_Ve', s_mult='1', t_mult='0..1', label='phatHanh', points=[(1540, 750), (1080, 750)])

    # 20. GiaoDich - GheSuatChieu (Routed via Corridor 2 at y=800)
    add_p1_edge(root1, 'c_GiaoDich', 'c_GheSuatChieu', s_mult='0..1', t_mult='0..*', label='giuHoacBan', points=[(760, 800), (1620, 800)])

    # 21. BangGiaVe - Ve (Routed via Corridor 2 at y=730)
    add_p1_edge(root1, 'c_BangGiaVe', 'c_Ve', s_mult='1', t_mult='0..*', label='giaChuan', points=[(1970, 730), (1170, 730)])


    # =========================================================================
    # PAGE 2: SoDoLop_ThietKe_3Tang (Design Class Diagram - 3-Layer Architecture)
    # Strictly Black and White, sharp rectangular corners, plain text UML
    # ZERO HTML tags inside cell values, spacious collision-free layout
    # =========================================================================
    diag2 = ET.SubElement(mxfile, 'diagram', {'id': 'diag_design_3tier', 'name': 'SoDoLop_ThietKe_3Tang'})
    model2 = ET.SubElement(diag2, 'mxGraphModel', {
        'dx': '2400', 'dy': '1800', 'grid': '1', 'gridSize': '10', 'guides': '1',
        'tooltips': '1', 'connect': '1', 'arrows': '1', 'fold': '1', 'page': '1',
        'pageScale': '1', 'pageWidth': '3300', 'pageHeight': '2400', 'math': '0', 'shadow': '0'
    })
    root2 = ET.SubElement(model2, 'root')
    ET.SubElement(root2, 'mxCell', {'id': '0'})
    ET.SubElement(root2, 'mxCell', {'id': '1', 'parent': '0'})

    # Title Banner for Page 2 (Clean black text, no colored banner)
    p2_title = ET.SubElement(root2, 'mxCell', {
        'id': 'p2_title_box',
        'value': 'SƠ ĐỒ LỚP Ở MỨC THIẾT KẾ - KIẾN TRÚC BA TẦNG KẾT HỢP ĐỐI TƯỢNG TRUYỀN TẢI DỮ LIỆU',
        'style': 'text;html=0;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontSize=14;fontStyle=1;fontColor=#000000;',
        'vertex': '1', 'parent': '1'
    })
    ET.SubElement(p2_title, 'mxGeometry', {'x': '40', 'y': '20', 'width': '3200', 'height': '40', 'as': 'geometry'})

    def add_package_frame(root, pid, title, x, y, w, h):
        pkg = ET.SubElement(root, 'mxCell', {
            'id': pid,
            'value': title,
            'style': 'swimlane;fontStyle=1;align=left;spacingLeft=14;verticalAlign=top;childLayout=stackLayout;horizontal=1;startSize=28;horizontalStack=0;resizeParent=0;collapsible=0;html=0;whiteSpace=wrap;fillColor=none;strokeColor=#000000;strokeWidth=1;fontSize=12;fontColor=#000000;rounded=0;',
            'vertex': '1', 'parent': '1'
        })
        ET.SubElement(pkg, 'mxGeometry', {'x': str(x), 'y': str(y), 'width': str(w), 'height': str(h), 'as': 'geometry'})
        return pkg

    def add_design_box(root, cid, stereotype, name, methods, x, y, w, h, subtitle=''):
        lines = []
        if stereotype:
            lines.append(f"«{stereotype}»")
        lines.append(name)
        if subtitle:
            lines.append(f"({subtitle})")
        if methods:
            lines.append("────────────────────────────────────────")
            lines.extend(methods)
        val = "\n".join(lines)
        style = "rounded=0;whiteSpace=wrap;html=0;align=left;verticalAlign=top;fillColor=#FFFFFF;strokeColor=#000000;strokeWidth=1;fontSize=11;fontColor=#000000;spacingLeft=8;spacingRight=8;spacingTop=6;spacingBottom=6;"
        cls_cell = ET.SubElement(root, 'mxCell', {
            'id': cid,
            'value': val,
            'style': style,
            'vertex': '1', 'parent': '1'
        })
        ET.SubElement(cls_cell, 'mxGeometry', {'x': str(x), 'y': str(y), 'width': str(w), 'height': str(h), 'as': 'geometry'})
        return cls_cell

    edge_p2_cnt = 0
    def add_p2_edge(root, source, target, label='', points=None):
        nonlocal edge_p2_cnt
        edge_p2_cnt += 1
        eid = f'e2_{edge_p2_cnt}'
        edge_cell = ET.SubElement(root, 'mxCell', {
            'id': eid,
            'value': label,
            'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=0;dashed=1;strokeWidth=1;strokeColor=#000000;endArrow=open;endFill=0;fontSize=11;fontStyle=2;fontColor=#000000;',
            'edge': '1', 'parent': '1',
            'source': source, 'target': target
        })
        geom = ET.SubElement(edge_cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})
        if points:
            arr = ET.SubElement(geom, 'Array', {'as': 'points'})
            for px, py in points:
                ET.SubElement(arr, 'mxPoint', {'x': str(px), 'y': str(py)})
        return edge_cell

    # Column coordinates configuration
    col_w = 250
    gap = 40
    step = col_w + gap  # 290
    def col_x(i):
        return 60 + i * step

    # Four Clean Architectural Package Swimlanes (Transparent, crisp black borders)
    add_package_frame(root2, 'pkg_gui', 'TẦNG GIAO DIỆN - package cinema.view', 40, 80, 3200, 250)
    add_package_frame(root2, 'pkg_bus', 'TẦNG NGHIỆP VỤ - package cinema.controller.*', 40, 370, 3200, 480)
    add_package_frame(root2, 'pkg_dao', 'TẦNG TRUY XUẤT DỮ LIỆU - package cinema.dao và cinema.db', 40, 890, 3200, 430)
    add_package_frame(root2, 'pkg_dto', 'MÔ HÌNH THỰC THỂ DỮ LIỆU TRUYỀN TẢI - package cinema.model', 40, 1360, 3200, 460)

    # LAYER 1: Presentation Layer (cinema.view)
    y_gui = 130
    h_gui = 90
    add_design_box(root2, 'v_DangNhap', '', 'DangNhapWindow', ['+ DangNhapWindow(service)', '- authenticate(service): void'], col_x(0), y_gui, col_w, h_gui, subtitle='Màn hình đăng nhập UC01')
    add_design_box(root2, 'v_NhanVien', '', 'NhanVienPanel', ['+ NhanVienPanel(session)', '- refresh(): void'], col_x(1), y_gui, col_w, h_gui, subtitle='Quản lý nhân sự UC02')
    add_design_box(root2, 'v_Phim', '', 'PhimPanel', ['+ PhimPanel(session)', '- refresh(): void'], col_x(2), y_gui, col_w, h_gui, subtitle='Quản lý phim UC03')
    add_design_box(root2, 'v_LichChieu', '', 'LichChieuPanel', ['+ LichChieuPanel(session)', '- refresh(): void'], col_x(3), y_gui, col_w, h_gui, subtitle='Lập lịch chiếu UC04')
    add_design_box(root2, 'v_PhongChieu', '', 'PhongChieuPanel', ['+ PhongChieuPanel(session)', '- refresh(): void'], col_x(4), y_gui, col_w, h_gui, subtitle='Phòng và Ghế UC05')
    add_design_box(root2, 'v_GiaVeFnB', '', 'GiaVeFnBPanel', ['+ GiaVeFnBPanel(session)', '- refresh(): void'], col_x(5), y_gui, col_w, h_gui, subtitle='Giá vé và Sản phẩm UC06')
    add_design_box(root2, 'v_KiemTraVe', '', 'KiemTraVePanel', ['+ KiemTraVePanel(session)', '- check(): void'], col_x(7), y_gui, col_w, h_gui, subtitle='Soát vé QR UC07')
    add_design_box(root2, 'v_BanVe', '', 'BanVePanel', ['+ BanVePanel(session)', '+ requestClose(): boolean'], col_x(8), y_gui, col_w, h_gui, subtitle='Bán vé quầy UC08')
    add_design_box(root2, 'v_BaoCao', '', 'BaoCaoPanel', ['+ BaoCaoPanel(session)', '- timeTab(): JPanel'], col_x(9), y_gui, col_w, h_gui, subtitle='Thống kê báo cáo UC09')
    add_design_box(root2, 'v_Cinema', '', 'CinemaWindow', ['+ open(session): void'], col_x(10), 120, col_w, 80, subtitle='Khung chính ứng dụng')
    add_design_box(root2, 'v_UIHelpers', '', 'cinema.view.ui.*', ['Tokens, Skin, Theme', 'SeatVisual, SalesViews', 'ReadOnlyViews, Forms'], col_x(10), 220, col_w, 85, subtitle='Thư viện hỗ trợ giao diện')

    # LAYER 2: Business Logic Layer (cinema.controller)
    # Row 1: BUS Classes (y=420, h=145)
    y_bus = 420
    h_bus = 145
    add_design_box(root2, 'b_DangNhap', '', 'DangNhapBUS', ['+ login(username, password): PhienDangNhap'], col_x(0), y_bus, col_w, h_bus, subtitle='cinema.controller.auth')
    add_design_box(root2, 'b_NhanVien', '', 'NhanVienBUS', ['+ search(keyword): List<NhanVien>', '+ create(input): NhanVien', '+ update(input): void', '+ delete(id): void', '+ detail(id): NhanVien'], col_x(1), y_bus, col_w, h_bus, subtitle='cinema.controller.staff')
    add_design_box(root2, 'b_Phim', '', 'PhimBUS', ['+ genres(): List<TheLoai>', '+ search(name, genre, status): List<Phim>', '+ create(input, image): Phim', '+ rename(id, name): void', '+ stop(id): void'], col_x(2), y_bus, col_w, h_bus, subtitle='cinema.controller.film')
    add_design_box(root2, 'b_LichChieu', '', 'LichChieuBUS', ['+ list(date, room): List<SuatChieu>', '+ create(film, room, start): SuatChieu', '+ changeFilm(id, film): SuatChieu', '+ cancel(id): void'], col_x(3), y_bus, col_w, h_bus, subtitle='cinema.controller.schedule')
    add_design_box(root2, 'b_PhongChieu', '', 'PhongChieuBUS', ['+ search(text, type, state): List<PhongChieu>', '+ create(name, type, clean, state): PhongChieu', '+ seats(room): List<Ghe>', '+ addSeat(room, row, num, type, state): Ghe'], col_x(4), y_bus, col_w, h_bus, subtitle='cinema.controller.room')
    add_design_box(root2, 'b_GiaVe', '', 'GiaVeBUS', ['+ list(): List<BangGiaVe>', '+ create(seat, room, period, price): BangGiaVe', '+ updatePrice(id, price): BangGiaVe'], col_x(5), y_bus, col_w, h_bus, subtitle='cinema.controller.pricing')
    add_design_box(root2, 'b_SanPham', '', 'SanPhamBUS', ['+ search(text, inStock): List<SanPham>', '+ create(input): void', '+ updateInfo(input): void', '+ updatePrice(id, amount): void'], col_x(6), y_bus, col_w, h_bus, subtitle='cinema.controller.pricing')
    add_design_box(root2, 'b_Ve', '', 'VeBUS', ['+ rooms(): List<PhongChieu>', '+ shows(date, room): List<SuatChieu>', '+ check(ticket, type, room, show): KetQua', '+ confirm(ticket, room, show): ThongTin'], col_x(7), y_bus, col_w, h_bus, subtitle='cinema.controller.admission')
    add_design_box(root2, 'b_BanVe', '', 'BanVeBUS', ['+ shows(name, date, time): List<SuatBan>', '+ hold(show, seats, txId): GioHang', '+ pay(txId, method, amount): List<ThongTin>', '+ cancel(txId): void'], col_x(8), y_bus, col_w, h_bus, subtitle='cinema.controller.sales')
    add_design_box(root2, 'b_BaoCao', '', 'BaoCaoBUS', ['+ theoThoiGian(range): TongHop', '+ theoPhim(range, film): List<TheoPhim>', '+ theoNhanVien(range, emp): List<TheoNhanVien>', '+ ve(range, film, show): ThongKeVe'], col_x(9), y_bus, col_w, h_bus, subtitle='cinema.controller.revenue')

    # Row 2: Helper Classes (y=650, h=100)
    y_hlp = 650
    h_hlp = 100
    add_design_box(root2, 'h_Auth', '', 'PasswordHasher\nPhienDangNhap', ['+ hash(password): String', '+ matches(password, stored): boolean', '+ laAdmin(): boolean'], col_x(0), y_hlp, col_w, h_hlp, subtitle='Bảo mật đăng nhập')
    add_design_box(root2, 'h_Film', '', 'PosterStore', ['+ importImage(source): String', '+ resolve(stored): Path', '+ discard(stored): void'], col_x(2), y_hlp, col_w, h_hlp, subtitle='Lưu trữ tệp áp phích')
    add_design_box(root2, 'h_Pricing', '', 'MoneyRules', ['+ positive(value, message): BigDecimal'], col_x(5), y_hlp, col_w, h_hlp, subtitle='Quy tắc tiền tệ')
    add_design_box(root2, 'h_Admission', '', 'DocMaQR\nThoiGianVaoPhong', ['+ read(file): String', '+ load(): ThoiGianVaoPhong'], col_x(7), y_hlp, col_w, h_hlp, subtitle='Xác thực soát vé')
    add_design_box(root2, 'h_Sales', '', 'InVe', ['+ preview(info): BufferedImage', '+ print(g, format, page): int'], col_x(8), y_hlp, col_w, h_hlp, subtitle='Định dạng in vé')
    add_design_box(root2, 'h_Revenue', '', 'KhoangThongKe', ['+ ngay(date): KhoangThongKe', '+ thang(month): KhoangThongKe', '+ nam(year): KhoangThongKe'], col_x(9), y_hlp, col_w, h_hlp, subtitle='Lọc khoảng thời gian')

    # LAYER 3: Data Access Layer (cinema.dao & cinema.db)
    # Row 1: DAOs (y=930, h=135)
    y_dao = 930
    h_dao = 135
    add_design_box(root2, 'd_TaiKhoan', '', 'TaiKhoanDAO', ['+ findByUsername(user): Optional<TaiKhoan>'], col_x(0), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_NhanVien', '', 'NhanVienDAO', ['+ search(keyword): List<NhanVien>', '+ insert(emp, hash, role): void', '+ update(emp): void', '+ delete(id): void'], col_x(1), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_PhimTheLoai', '', 'PhimDAO\nTheLoaiDAO', ['+ search(name, genre, status): List<Phim>', '+ insert(film): void', '+ stop(id): void', '+ all(): List<TheLoai>'], col_x(2), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_SuatChieu', '', 'SuatChieuDAO', ['+ list(date, room): List<SuatChieu>', '+ create(film, room, start, holidays): SuatChieu', '+ modify(id, edit, holidays): SuatChieu', '+ cancel(id): void'], col_x(3), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_PhongGhe', '', 'PhongChieuDAO\nGheDAO', ['+ search(key, type, state): List<PhongChieu>', '+ insert(room): void', '+ list(room): List<Ghe>', '+ insert(room, row, num, type, state): Ghe'], col_x(4), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_BangGia', '', 'BangGiaVeDAO\nLoaiThoiDiemDAO', ['+ all(): List<BangGiaVe>', '+ insert(seat, room, period, price): int', '+ updatePrice(id, price): void'], col_x(5), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_SanPham', '', 'SanPhamDAO', ['+ search(key, inStock): List<SanPham>', '+ insert(product): void', '+ updateInfo(product): void', '+ updatePrice(id, price): void'], col_x(6), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_Ve', '', 'VeDAO', ['+ inspect(code, type): Optional<ThongTin>', '+ confirm(maVe, truoc, sau, val): ThongTin'], col_x(7), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_BanVe', '', 'BanVeDAO\nGiuGheDAO\nGiaoDichDAO', ['+ search(name, date, time): List<SuatBan>', '+ hold(show, seats, staff, id, min): GioHang', '+ pay(id, method, amount): List<ThongTin>', '+ cancel(id): void'], col_x(8), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_BaoCao', '', 'BaoCaoDAO', ['+ theoThoiGian(range): TongHop', '+ theoPhim(range, film): List<TheoPhim>', '+ theoNhanVien(range, emp): List<TheoNhanVien>', '+ ve(range, film, show): ThongKeVe'], col_x(9), y_dao, col_w, h_dao)

    # Database Utility centered under DAO row
    add_design_box(root2, 'd_Database', 'utility', 'Database',
                   ['+ connect(): Connection'],
                   1350, 1180, 380, 95, subtitle='config/database.properties')

    # LAYER 4: DTO / Domain Model Layer (cinema.model)
    y_m1 = 1410
    h_m1 = 135
    add_design_box(root2, 'm_TaiKhoan', 'record', 'TaiKhoan', ['maTK: int', 'tenDangNhap: String', 'matKhauHash: String', 'trangThai: String', 'vaiTro: VaiTro', 'nhanVien: NhanVien'], 60, y_m1, 280, h_m1)
    add_design_box(root2, 'm_VaiTro', 'enumeration', 'VaiTro', ['ADMIN', 'NHAN_VIEN', '+ fromDatabase(s): VaiTro'], 370, y_m1, 240, h_m1)
    add_design_box(root2, 'm_NhanVien', 'record', 'NhanVien', ['maNV: String, hoTen: String', 'ngaySinh: LocalDate', 'soDienThoai, email: String', 'diaChi, chucVu: String'], 640, y_m1, 270, h_m1)
    add_design_box(root2, 'm_Phim', 'record', 'Phim', ['maPhim, tenPhim: String', 'thoiLuong, gioiHanTuoi: int', 'daoDien, poster: String', 'ngayKhoiChieu: LocalDate', 'theLoai: List<TheLoai>'], 940, y_m1, 280, h_m1)
    add_design_box(root2, 'm_TheLoai', 'record', 'TheLoai', ['maTheLoai: int', 'tenTheLoai: String'], 1250, y_m1, 220, h_m1)
    add_design_box(root2, 'm_SuatChieu', 'record', 'SuatChieu', ['maSuat, maLoaiThoiDiem: int', 'maPhim, maPhong: String', 'thoiGianBatDau: LocalDateTime', 'thoiGianTraPhong: LocalDateTime', 'trangThai: String'], 1500, y_m1, 300, h_m1)
    add_design_box(root2, 'm_PhongChieu', 'record', 'PhongChieu', ['maPhong, tenPhong: String', 'loaiPhong: LoaiPhong', 'thoiGianDonPhong: int', 'trangThai: String'], 1830, y_m1, 260, h_m1)
    add_design_box(root2, 'm_LoaiPhong', 'record', 'LoaiPhong', ['maLoaiPhong: int', 'tenLoaiPhong: String', 'dinhDangChieu: String'], 2120, y_m1, 230, h_m1)
    add_design_box(root2, 'm_Ghe', 'record', 'Ghe', ['maGhe, soGhe: int', 'maPhong, hangGhe: String', 'loaiGhe: LoaiGhe', 'trangThai: String'], 2380, y_m1, 260, h_m1)
    add_design_box(root2, 'm_LoaiGhe', 'record', 'LoaiGhe', ['maLoaiGhe: int', 'tenLoaiGhe: String'], 2670, y_m1, 220, h_m1)

    y_m2 = 1580
    h_m2 = 145
    add_design_box(root2, 'm_GheSuatChieu', 'record', 'GheSuatChieu', ['maSuat: int, ghe: Ghe', 'maGiaoDich: String', 'trangThai: String', 'thoiGianHetHanGiu: LocalDateTime', 'maGia: Integer, giaVeChot: BigDecimal'], 60, y_m2, 350, h_m2)
    add_design_box(root2, 'm_LoaiThoiDiem', 'record', 'LoaiThoiDiem', ['maLoaiThoiDiem: int', 'tenLoaiThoiDiem: String'], 440, y_m2, 240, h_m2)
    add_design_box(root2, 'm_BangGiaVe', 'record', 'BangGiaVe', ['maGia: int, giaVe: BigDecimal', 'loaiGhe: LoaiGhe', 'loaiPhong: LoaiPhong', 'loaiThoiDiem: LoaiThoiDiem'], 710, y_m2, 280, h_m2)
    add_design_box(root2, 'm_GiaoDich', 'record', 'GiaoDich', ['maGiaoDich: String, maNV: String', 'thoiGianTao: LocalDateTime', 'tongTien: BigDecimal', 'trangThai: String'], 1020, y_m2, 300, h_m2)
    add_design_box(root2, 'm_ThanhToan', 'record', 'ThanhToan', ['maThanhToan: int', 'maGiaoDich: String, phuongThuc: String', 'soTien: BigDecimal', 'thoiGianThanhToan: LocalDateTime'], 1350, y_m2, 320, h_m2)
    add_design_box(root2, 'm_Ve', 'record', 'Ve', ['maVe, maGiaoDich: String', 'maSuat, maGhe, maGia: int', 'maQR: String, giaVe: BigDecimal', 'hangGheLucPhatHanh, tenLoaiGheLucPhatHanh: String', 'soGheLucPhatHanh: int, thoiGianPhatHanh: LocalDateTime'], 1700, y_m2, 390, h_m2)
    add_design_box(root2, 'm_SanPham', 'record', 'SanPham', ['maSP, tenSP, moTa: String', 'donViTinh, tinhTrangKinhDoanh: String', 'giaBan: BigDecimal, soLuongTon: int'], 2120, y_m2, 330, h_m2)

    # EDGES: Layer 1 (GUI) -> Layer 2 (BUS)
    # Direct vertical calls (aligned columns)
    add_p2_edge(root2, 'v_DangNhap', 'b_DangNhap', label='«call»')
    add_p2_edge(root2, 'v_NhanVien', 'b_NhanVien', label='«call»')
    add_p2_edge(root2, 'v_Phim', 'b_Phim', label='«call»')
    add_p2_edge(root2, 'v_LichChieu', 'b_LichChieu', label='«call»')
    add_p2_edge(root2, 'v_PhongChieu', 'b_PhongChieu', label='«call»')
    add_p2_edge(root2, 'v_GiaVeFnB', 'b_GiaVe', label='«call»')
    # v_GiaVeFnB -> b_SanPham routed horizontally in corridor between GUI and BUS
    add_p2_edge(root2, 'v_GiaVeFnB', 'b_SanPham', label='«call»', points=[(col_x(5) + 125, 290), (col_x(6) + 125, 290)])
    add_p2_edge(root2, 'v_KiemTraVe', 'b_Ve', label='«call»')
    add_p2_edge(root2, 'v_BanVe', 'b_BanVe', label='«call»')
    add_p2_edge(root2, 'v_BaoCao', 'b_BaoCao', label='«call»')

    # EDGES: Layer 2 (BUS) -> Layer 3 (DAO)
    # Direct vertical calls for columns without helpers
    add_p2_edge(root2, 'b_NhanVien', 'd_NhanVien', label='«call»')
    add_p2_edge(root2, 'b_LichChieu', 'd_SuatChieu', label='«call»')
    add_p2_edge(root2, 'b_PhongChieu', 'd_PhongGhe', label='«call»')
    add_p2_edge(root2, 'b_SanPham', 'd_SanPham', label='«call»')

    # Routed calls for columns with helpers via wide column gaps
    add_p2_edge(root2, 'b_DangNhap', 'd_TaiKhoan', label='«call»',
                points=[(col_x(0) + 200, 605), (330, 605), (330, 840), (col_x(0) + 200, 840)])

    add_p2_edge(root2, 'b_Phim', 'd_PhimTheLoai', label='«call»',
                points=[(col_x(2) + 200, 605), (910, 605), (910, 840), (col_x(2) + 200, 840)])

    add_p2_edge(root2, 'b_GiaVe', 'd_BangGia', label='«call»',
                points=[(col_x(5) + 200, 605), (1780, 605), (1780, 840), (col_x(5) + 200, 840)])

    add_p2_edge(root2, 'b_Ve', 'd_Ve', label='«call»',
                points=[(col_x(7) + 200, 605), (2360, 605), (2360, 840), (col_x(7) + 200, 840)])

    add_p2_edge(root2, 'b_BanVe', 'd_BanVe', label='«call»',
                points=[(col_x(8) + 200, 605), (2650, 605), (2650, 840), (col_x(8) + 200, 840)])

    add_p2_edge(root2, 'b_BaoCao', 'd_BaoCao', label='«call»',
                points=[(col_x(9) + 200, 605), (2940, 605), (2940, 840), (col_x(9) + 200, 840)])

    # EDGES: Layer 3 (DAO) -> Database Utility
    # Routed cleanly in the open corridor between DAO row and d_Database
    dao_sources = [
        ('d_TaiKhoan', col_x(0) + 125, 1100, 1370),
        ('d_NhanVien', col_x(1) + 125, 1110, 1410),
        ('d_PhimTheLoai', col_x(2) + 125, 1120, 1450),
        ('d_SuatChieu', col_x(3) + 125, 1130, 1490),
        ('d_PhongGhe', col_x(4) + 125, 1140, 1530),
        ('d_BangGia', col_x(5) + 125, 1140, 1570),
        ('d_SanPham', col_x(6) + 125, 1130, 1610),
        ('d_Ve', col_x(7) + 125, 1120, 1650),
        ('d_BanVe', col_x(8) + 125, 1110, 1690),
        ('d_BaoCao', col_x(9) + 125, 1100, 1710),
    ]
    for d_id, x_start, y_corr, x_target in dao_sources:
        add_p2_edge(root2, d_id, 'd_Database', label='', points=[(x_start, y_corr), (x_target, y_corr)])

    tree = ET.ElementTree(mxfile)
    ET.indent(tree, space='  ', level=0)
    return tree

if __name__ == '__main__':
    tree = build_drawio()
    out_path = r'docs/SoDoLop_RapChieuPhim.drawio'
    tree.write(out_path, encoding='utf-8', xml_declaration=True)
    print(f'Successfully wrote {out_path}')
