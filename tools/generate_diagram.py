import xml.etree.ElementTree as ET

def build_drawio():
    mxfile = ET.Element('mxfile', {'host': 'app.diagrams.net', 'pages': '2', 'agent': 'OOAD-Cinema'})

    # =========================================================================
    # PAGE 1: SoDoLop_PhanTich (Analysis Class Diagram)
    # Strictly Black and White, sharp rectangular corners, plain text UML
    # Clean 2D spatial layout radiating around central domain classes
    # =========================================================================
    diag1 = ET.SubElement(mxfile, 'diagram', {'id': 'diag_analysis', 'name': 'SoDoLop_PhanTich'})
    model1 = ET.SubElement(diag1, 'mxGraphModel', {
        'dx': '2000', 'dy': '1400', 'grid': '1', 'gridSize': '10', 'guides': '1',
        'tooltips': '1', 'connect': '1', 'arrows': '1', 'fold': '1', 'page': '1',
        'pageScale': '1', 'pageWidth': '2350', 'pageHeight': '1450', 'math': '0', 'shadow': '0'
    })
    root1 = ET.SubElement(model1, 'root')
    ET.SubElement(root1, 'mxCell', {'id': '0'})
    ET.SubElement(root1, 'mxCell', {'id': '1', 'parent': '0'})


    def add_class_box(root, cid, name, attrs, methods, x, y, w, h_attr, h_meth):
        start_size = 38 if '\n' in name else 28
        total_h = start_size + h_attr + 8 + h_meth
        hdr_style = f"swimlane;fontStyle=1;align=center;verticalAlign=top;childLayout=stackLayout;horizontal=1;startSize={start_size};horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;marginBottom=0;html=0;whiteSpace=wrap;rounded=0;fillColor=#FFFFFF;strokeColor=#000000;strokeWidth=1;fontSize=12;fontColor=#000000;"
        cls_cell = ET.SubElement(root, 'mxCell', {
            'id': cid,
            'value': name,
            'style': hdr_style,
            'vertex': '1', 'parent': '1'
        })
        ET.SubElement(cls_cell, 'mxGeometry', {'x': str(x), 'y': str(y), 'width': str(w), 'height': str(total_h), 'as': 'geometry'})

        # Attributes Compartment
        attr_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_attrs',
            'value': '\n'.join(attrs),
            'style': 'text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=8;spacingRight=8;spacingTop=4;spacingBottom=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=0;fontSize=11;fontColor=#000000;',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(attr_cell, 'mxGeometry', {'y': str(start_size), 'width': str(w), 'height': str(h_attr), 'as': 'geometry'})

        # Separator line
        sep_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_sep',
            'value': '',
            'style': 'line;strokeWidth=1;fillColor=none;align=left;verticalAlign=middle;spacingTop=-1;spacingLeft=3;spacingRight=3;rotatable=0;labelPosition=right;points=[];portConstraint=eastwest;strokeColor=#000000;',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(sep_cell, 'mxGeometry', {'y': str(start_size + h_attr), 'width': str(w), 'height': '8', 'as': 'geometry'})

        # Methods Compartment
        meth_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_methods',
            'value': '\n'.join(methods),
            'style': 'text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=8;spacingRight=8;spacingTop=4;spacingBottom=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=0;fontSize=11;fontColor=#000000;',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(meth_cell, 'mxGeometry', {'y': str(start_size + 8 + h_attr), 'width': str(w), 'height': str(h_meth), 'as': 'geometry'})
        return cls_cell

    # =========================================================================
    # 17 Domain Classes on Page 1 (2D Spatial Layout)
    # Row 1 (North, y=80): TheLoai, Phim, LoaiThoiDiem, LoaiPhong, PhongChieu, Ghe, LoaiGhe
    # Row 2 (Middle, y=420..660): VaiTro, TaiKhoan, NhanVien, SuatChieu, GheSuatChieu, BangGiaVe
    # Row 3 (South, y=1020): SanPham, ThanhToan, GiaoDich, Ve
    # =========================================================================

    # North Row: Film & Theater Infrastructure
    add_class_box(root1, 'c_TheLoai', 'TheLoai',
                  ['- maTheLoai: int', '- tenTheLoai: String'],
                  ['+ getTen(): String'],
                  80, 80, 220, 45, 30)

    add_class_box(root1, 'c_Phim', 'Phim',
                  ['- maPhim: String', '- tenPhim: String', '- thoiLuong: int', '- daoDien: String',
                   '- ngayKhoiChieu: LocalDate', '- gioiHanTuoi: int', '- poster: String',
                   '- trangThai: String', '- dinhDangChieu: String'],
                  ['+ ngungChieu(): void', '+ dangChieu(): boolean', '+ withTheLoai(theLoai: List<TheLoai>): Phim'],
                  380, 80, 320, 170, 65)

    add_class_box(root1, 'c_LoaiThoiDiem', 'LoaiThoiDiem',
                  ['- maLoaiThoiDiem: int', '- tenLoaiThoiDiem: String'],
                  ['+ getTen(): String'],
                  780, 80, 220, 45, 30)

    add_class_box(root1, 'c_LoaiPhong', 'LoaiPhong',
                  ['- maLoaiPhong: int', '- tenLoaiPhong: String', '- dinhDangChieu: String'],
                  ['+ getDinhDang(): String'],
                  1080, 80, 220, 65, 30)

    add_class_box(root1, 'c_PhongChieu', 'PhongChieu',
                  ['- maPhong: String', '- tenPhong: String', '- thoiGianDonPhong: int', '- trangThai: String', '/ sucChua: int'],
                  ['+ tinhSucChua(): int', '+ dangHoatDong(): boolean'],
                  1380, 80, 250, 95, 45)

    add_class_box(root1, 'c_Ghe', 'Ghe',
                  ['- maGhe: int', '- hangGhe: String', '- soGhe: int', '- trangThai: String'],
                  ['+ sanSangSuDung(): boolean', '+ chuyenBaoTri(): void'],
                  1710, 80, 230, 75, 45)

    add_class_box(root1, 'c_LoaiGhe', 'LoaiGhe',
                  ['- maLoaiGhe: int', '- tenLoaiGhe: String'],
                  ['+ getTen(): String'],
                  2020, 80, 210, 45, 30)

    # Middle Row: Auth & Operational Core
    add_class_box(root1, 'c_VaiTro', '«enumeration»\nVaiTro',
                  ['- ADMIN: VaiTro', '- NHAN_VIEN: VaiTro', '- maVaiTro: int', '- tenVaiTro: String'],
                  ['+ getTen(): String', '+ fromDatabase(ten: String): VaiTro'],
                  80, 420, 260, 75, 45)

    add_class_box(root1, 'c_TaiKhoan', 'TaiKhoan',
                  ['- maTK: int', '- tenDangNhap: String', '- matKhauHash: String', '- trangThai: String'],
                  ['+ dangHoatDong(): boolean', '+ doiMatKhau(mkMoi: String): void', '+ kiemTraDangNhap(mk: String): boolean'],
                  80, 660, 260, 75, 65)

    add_class_box(root1, 'c_NhanVien', 'NhanVien',
                  ['- maNV: String', '- hoTen: String', '- ngaySinh: LocalDate', '- soDienThoai: String',
                   '- email: String', '- diaChi: String', '- chucVu: String'],
                  ['+ capNhatThongTin(hoTen, sdt, email, diaChi, chucVu): void', '+ getHoTen(): String'],
                  420, 660, 350, 135, 55)

    add_class_box(root1, 'c_SuatChieu', 'SuatChieu',
                  ['- maSuat: int', '- thoiGianBatDau: LocalDateTime', '- thoiGianTraPhong: LocalDateTime', '- trangThai: String'],
                  ['+ kiemTraKhungGio(batDau, ketThuc): boolean', '+ coTheBanVe(): boolean', '+ huySuat(): void'],
                  850, 480, 320, 80, 65)

    add_class_box(root1, 'c_GheSuatChieu', 'GheSuatChieu',
                  ['- trangThai: String', '- thoiGianHetHanGiu: LocalDateTime', '- giaVeChot: BigDecimal'],
                  ['+ kiemTraHetHanGiu(): boolean', '+ giuGhe(hanGiu: LocalDateTime): void', '+ xacNhanBan(): void', '+ giaiPhongGhe(): void'],
                  1270, 480, 300, 65, 80)

    add_class_box(root1, 'c_BangGiaVe', 'BangGiaVe',
                  ['- maGia: int', '- giaVe: BigDecimal'],
                  ['+ capNhatGia(giaMoi: BigDecimal): void'],
                  1670, 480, 290, 45, 35)

    # South Row: Sales, Financial Transactions, and Ticketing
    add_class_box(root1, 'c_SanPham', 'SanPham',
                  ['- maSP: String', '- tenSP: String', '- moTa: String', '- donViTinh: String',
                   '- giaBan: BigDecimal', '- soLuongTon: int', '- tinhTrangKinhDoanh: String'],
                  ['+ capNhatTonKho(soLuongThayDoi: int): void', '+ conHang(): boolean'],
                  80, 1020, 280, 135, 50)

    add_class_box(root1, 'c_ThanhToan', 'ThanhToan',
                  ['- maThanhToan: int', '- phuongThuc: String', '- soTien: BigDecimal', '- thoiGianThanhToan: LocalDateTime'],
                  ['+ xacNhanThanhToan(): boolean'],
                  420, 1020, 270, 80, 30)

    add_class_box(root1, 'c_GiaoDich', 'GiaoDich',
                  ['- maGiaoDich: String', '- thoiGianTao: LocalDateTime', '- tongTien: BigDecimal', '- trangThai: String'],
                  ['+ tinhTongTien(): BigDecimal', '+ hoanTatGiaoDich(): void', '+ huyGiaoDich(): void'],
                  770, 1020, 290, 80, 65)

    add_class_box(root1, 'c_Ve', 'Ve',
                  ['- maVe: String', '- maQR: String', '- giaVe: BigDecimal', '- trangThai: String',
                   '- thoiGianPhatHanh: LocalDateTime', '- hangGheLucPhatHanh: String',
                   '- soGheLucPhatHanh: int', '- tenLoaiGheLucPhatHanh: String'],
                  ['+ kiemTraHopLe(): boolean', '+ danhDauSuDung(): void'],
                  1200, 1020, 300, 155, 45)

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

    # Page 1 Relationships (Orthogonal, Pure Black & White UML, 0 Collisions)
    # 1. VaiTro - TaiKhoan (Vertical straight)
    add_p1_edge(root1, 'c_VaiTro', 'c_TaiKhoan', s_mult='1', t_mult='0..*', label='')

    # 2. NhanVien - TaiKhoan (Horizontal straight)
    add_p1_edge(root1, 'c_NhanVien', 'c_TaiKhoan', s_mult='1', t_mult='0..1', label='')

    # 3. TheLoai - Phim (Horizontal straight)
    add_p1_edge(root1, 'c_TheLoai', 'c_Phim', s_mult='1..*', t_mult='1..*', label='')

    # 4. LoaiPhong - PhongChieu (Horizontal straight)
    add_p1_edge(root1, 'c_LoaiPhong', 'c_PhongChieu', s_mult='1', t_mult='0..*', label='')

    # 5. PhongChieu - Ghe (Aggregation: hollow diamond at PhongChieu)
    add_p1_edge(root1, 'c_PhongChieu', 'c_Ghe', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='0', start_size='16')

    # 6. LoaiGhe - Ghe (Horizontal straight)
    add_p1_edge(root1, 'c_LoaiGhe', 'c_Ghe', s_mult='1', t_mult='0..*', label='')

    # 7. NhanVien - GiaoDich (Drops to open corridor y=940 and turns right to GiaoDich)
    add_p1_edge(root1, 'c_NhanVien', 'c_GiaoDich', s_mult='1', t_mult='0..*', label='', points=[(600, 940), (830, 940)])

    # 8. Phim - SuatChieu (Drops to open corridor y=420 and turns right to SuatChieu)
    add_p1_edge(root1, 'c_Phim', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='', points=[(540, 420), (930, 420)])

    # 9. LoaiThoiDiem - SuatChieu (Drops down from LoaiThoiDiem to SuatChieu)
    add_p1_edge(root1, 'c_LoaiThoiDiem', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='', points=[(890, 300), (990, 300)])

    # 10. SuatChieu - GheSuatChieu (Composition: solid black diamond at SuatChieu)
    add_p1_edge(root1, 'c_SuatChieu', 'c_GheSuatChieu', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 11. PhongChieu - SuatChieu (Routed via corridor at y=400)
    add_p1_edge(root1, 'c_PhongChieu', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='', points=[(1500, 400), (1070, 400)])

    # 12. Ghe - GheSuatChieu (Routed via corridor at y=380)
    add_p1_edge(root1, 'c_Ghe', 'c_GheSuatChieu', s_mult='1', t_mult='0..*', label='', points=[(1820, 380), (1420, 380)])

    # 13. LoaiPhong - BangGiaVe (Routed via corridor at y=360)
    add_p1_edge(root1, 'c_LoaiPhong', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(1190, 360), (1740, 360)])

    # 14. LoaiGhe - BangGiaVe (Routed via corridor at y=350)
    add_p1_edge(root1, 'c_LoaiGhe', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(2120, 350), (1890, 350)])

    # 15. LoaiThoiDiem - BangGiaVe (Routed via corridor at y=320)
    add_p1_edge(root1, 'c_LoaiThoiDiem', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(950, 320), (1810, 320)])

    # 16. BangGiaVe - GheSuatChieu (Horizontal straight)
    add_p1_edge(root1, 'c_BangGiaVe', 'c_GheSuatChieu', s_mult='0..1', t_mult='0..*', label='')

    # 17. GiaoDich - ThanhToan (Composition: solid black diamond at GiaoDich)
    add_p1_edge(root1, 'c_GiaoDich', 'c_ThanhToan', s_mult='1', t_mult='0..1', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 18. GiaoDich - Ve (Composition: solid black diamond at GiaoDich)
    add_p1_edge(root1, 'c_GiaoDich', 'c_Ve', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 19. GheSuatChieu - Ve (Vertical routed via corridor y=750)
    add_p1_edge(root1, 'c_GheSuatChieu', 'c_Ve', s_mult='1', t_mult='0..1', label='', points=[(1350, 750), (1300, 750)])

    # 20. GiaoDich - GheSuatChieu (Routed via corridor y=800)
    add_p1_edge(root1, 'c_GiaoDich', 'c_GheSuatChieu', s_mult='0..1', t_mult='0..*', label='', points=[(950, 800), (1450, 800)])

    # 21. BangGiaVe - Ve (Routed via corridor y=880)
    add_p1_edge(root1, 'c_BangGiaVe', 'c_Ve', s_mult='1', t_mult='0..*', label='', points=[(1810, 880), (1400, 880)])


    # =========================================================================
    # PAGE 2: SoDoLop_ThietKe_3Tang (Design Class Diagram - 3-Layer Architecture)
    # Strictly Black and White, sharp rectangular corners, plain text UML
    # ZERO HTML tags inside cell values, generous box sizes, spacious layout
    # =========================================================================
    diag2 = ET.SubElement(mxfile, 'diagram', {'id': 'diag_design_3tier', 'name': 'SoDoLop_ThietKe_3Tang'})
    model2 = ET.SubElement(diag2, 'mxGraphModel', {
        'dx': '2400', 'dy': '1800', 'grid': '1', 'gridSize': '10', 'guides': '1',
        'tooltips': '1', 'connect': '1', 'arrows': '1', 'fold': '1', 'page': '1',
        'pageScale': '1', 'pageWidth': '3900', 'pageHeight': '2400', 'math': '0', 'shadow': '0'
    })
    root2 = ET.SubElement(model2, 'root')
    ET.SubElement(root2, 'mxCell', {'id': '0'})
    ET.SubElement(root2, 'mxCell', {'id': '1', 'parent': '0'})

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
        hdr_val = f"«{stereotype}»\n{name}" if stereotype else name
        lines_count = len(hdr_val.split('\n'))
        start_size = 28 if lines_count == 1 else (38 if lines_count == 2 else 18 * lines_count)

        hdr_style = f"swimlane;fontStyle=1;align=center;verticalAlign=top;childLayout=stackLayout;horizontal=1;startSize={start_size};horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;marginBottom=0;html=0;whiteSpace=wrap;rounded=0;fillColor=#FFFFFF;strokeColor=#000000;strokeWidth=1;fontSize=12;fontColor=#000000;"
        cls_cell = ET.SubElement(root, 'mxCell', {
            'id': cid,
            'value': hdr_val,
            'style': hdr_style,
            'vertex': '1', 'parent': '1'
        })
        ET.SubElement(cls_cell, 'mxGeometry', {'x': str(x), 'y': str(y), 'width': str(w), 'height': str(h), 'as': 'geometry'})

        body_h = max(20, h - start_size)
        body_val = '\n'.join(methods) if methods else ''
        body_style = 'text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=8;spacingRight=8;spacingTop=4;spacingBottom=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=0;fontSize=11;fontColor=#000000;'
        body_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_body',
            'value': body_val,
            'style': body_style,
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(body_cell, 'mxGeometry', {'y': str(start_size), 'width': str(w), 'height': str(body_h), 'as': 'geometry'})
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

    # Column coordinates configuration (Generous width and spacious gaps)
    col_w = 280
    gap = 60
    step = col_w + gap  # 340
    def col_x(i):
        return 60 + i * step

    # Four Architectural Package Frames (Transparent, clean black borders)
    add_package_frame(root2, 'pkg_gui', 'TẦNG GIAO DIỆN - package cinema.view', 40, 80, 3760, 260)
    add_package_frame(root2, 'pkg_bus', 'TẦNG NGHIỆP VỤ - package cinema.controller.*', 40, 360, 3760, 500)
    add_package_frame(root2, 'pkg_dao', 'TẦNG TRUY XUẤT DỮ LIỆU - package cinema.dao và cinema.db', 40, 880, 3760, 480)
    add_package_frame(root2, 'pkg_dto', 'MÔ HÌNH THỰC THỂ DỮ LIỆU TRUYỀN TẢI - package cinema.model', 40, 1380, 3760, 460)

    # LAYER 1: Presentation Layer (cinema.view)
    y_gui = 130
    h_gui = 95
    add_design_box(root2, 'v_DangNhap', '', 'DangNhapWindow', ['+ DangNhapWindow(service)', '- authenticate(service): void'], col_x(0), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_NhanVien', '', 'NhanVienPanel', ['+ NhanVienPanel(session)', '- refresh(): void'], col_x(1), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_Phim', '', 'PhimPanel', ['+ PhimPanel(session)', '- refresh(): void'], col_x(2), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_LichChieu', '', 'LichChieuPanel', ['+ LichChieuPanel(session)', '- refresh(): void'], col_x(3), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_PhongChieu', '', 'PhongChieuPanel', ['+ PhongChieuPanel(session)', '- refresh(): void'], col_x(4), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_GiaVeFnB', '', 'GiaVeFnBPanel', ['+ GiaVeFnBPanel(session)', '- refresh(): void'], col_x(5), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_KiemTraVe', '', 'KiemTraVePanel', ['+ KiemTraVePanel(session)', '- check(): void'], col_x(7), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_BanVe', '', 'BanVePanel', ['+ BanVePanel(session)', '+ requestClose(): boolean'], col_x(8), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_BaoCao', '', 'BaoCaoPanel', ['+ BaoCaoPanel(session)', '- timeTab(): JPanel'], col_x(9), y_gui, col_w, h_gui)
    add_design_box(root2, 'v_Cinema', '', 'CinemaWindow', ['+ open(session): void'], col_x(10), 120, col_w, 85)
    add_design_box(root2, 'v_UIHelpers', '', 'cinema.view.ui.*', ['Tokens, Skin, Theme', 'SeatVisual, SalesViews', 'ReadOnlyViews, Forms'], col_x(10), 225, col_w, 90)

    # LAYER 2: Business Logic Layer (cinema.controller)
    # Row 1: BUS Classes (y=400, h=165)
    y_bus = 400
    h_bus = 165
    add_design_box(root2, 'b_DangNhap', '', 'DangNhapBUS', ['+ login(username, password): PhienDangNhap'], col_x(0), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_NhanVien', '', 'NhanVienBUS', ['+ search(keyword): List<NhanVien>', '+ create(input): NhanVien', '+ update(input): void', '+ delete(id): void', '+ detail(id): NhanVien'], col_x(1), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_Phim', '', 'PhimBUS', ['+ genres(): List<TheLoai>', '+ search(name, genre, status): List<Phim>', '+ create(input, image): Phim', '+ rename(id, name): void', '+ stop(id): void'], col_x(2), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_LichChieu', '', 'LichChieuBUS', ['+ list(date, room): List<SuatChieu>', '+ create(film, room, start): SuatChieu', '+ changeFilm(id, film): SuatChieu', '+ cancel(id): void'], col_x(3), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_PhongChieu', '', 'PhongChieuBUS', ['+ search(text, type, state): List<PhongChieu>', '+ create(name, type, clean, state): PhongChieu', '+ seats(room): List<Ghe>', '+ addSeat(room, row, num, type, state): Ghe'], col_x(4), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_GiaVe', '', 'GiaVeBUS', ['+ list(): List<BangGiaVe>', '+ create(seat, room, period, price): BangGiaVe', '+ updatePrice(id, price): BangGiaVe'], col_x(5), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_SanPham', '', 'SanPhamBUS', ['+ search(text, inStock): List<SanPham>', '+ create(input): void', '+ updateInfo(input): void', '+ updatePrice(id, amount): void'], col_x(6), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_Ve', '', 'VeBUS', ['+ rooms(): List<PhongChieu>', '+ shows(date, room): List<SuatChieu>', '+ check(ticket, type, room, show): KetQua', '+ confirm(ticket, room, show): ThongTin'], col_x(7), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_BanVe', '', 'BanVeBUS', ['+ shows(name, date, time): List<SuatBan>', '+ hold(show, seats, txId): GioHang', '+ pay(txId, method, amount): List<ThongTin>', '+ cancel(txId): void'], col_x(8), y_bus, col_w, h_bus)
    add_design_box(root2, 'b_BaoCao', '', 'BaoCaoBUS', ['+ theoThoiGian(range): TongHop', '+ theoPhim(range, film): List<TheoPhim>', '+ theoNhanVien(range, emp): List<TheoNhanVien>', '+ ve(range, film, show): ThongKeVe'], col_x(9), y_bus, col_w, h_bus)

    # Row 2: Helper Classes (y=630, h=105)
    y_hlp = 630
    h_hlp = 105
    add_design_box(root2, 'h_Auth', '', 'PasswordHasher\nPhienDangNhap', ['+ hash(password): String', '+ matches(password, stored): boolean', '+ laAdmin(): boolean'], col_x(0), y_hlp, col_w, h_hlp)
    add_design_box(root2, 'h_Film', '', 'PosterStore', ['+ importImage(source): String', '+ resolve(stored): Path', '+ discard(stored): void'], col_x(2), y_hlp, col_w, h_hlp)
    add_design_box(root2, 'h_Pricing', '', 'MoneyRules', ['+ positive(value, message): BigDecimal'], col_x(5), y_hlp, col_w, h_hlp)
    add_design_box(root2, 'h_Admission', '', 'DocMaQR\nThoiGianVaoPhong', ['+ read(file): String', '+ load(): ThoiGianVaoPhong'], col_x(7), y_hlp, col_w, h_hlp)
    add_design_box(root2, 'h_Sales', '', 'InVe', ['+ preview(info): BufferedImage', '+ print(g, format, page): int'], col_x(8), y_hlp, col_w, h_hlp)
    add_design_box(root2, 'h_Revenue', '', 'KhoangThongKe', ['+ ngay(date): KhoangThongKe', '+ thang(month): KhoangThongKe', '+ nam(year): KhoangThongKe'], col_x(9), y_hlp, col_w, h_hlp)

    # LAYER 3: Data Access Layer (cinema.dao & cinema.db)
    # Row 1: DAOs (y=900, h=160, d_BanVe h=185)
    y_dao = 900
    h_dao = 160
    add_design_box(root2, 'd_TaiKhoan', '', 'TaiKhoanDAO', ['+ findByUsername(user): Optional<TaiKhoan>'], col_x(0), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_NhanVien', '', 'NhanVienDAO', ['+ search(keyword): List<NhanVien>', '+ insert(emp, hash, role): void', '+ update(emp): void', '+ delete(id): void'], col_x(1), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_PhimTheLoai', '', 'PhimDAO\nTheLoaiDAO', ['+ search(name, genre, status): List<Phim>', '+ insert(film): void', '+ stop(id): void', '+ all(): List<TheLoai>'], col_x(2), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_SuatChieu', '', 'SuatChieuDAO', ['+ list(date, room): List<SuatChieu>', '+ create(film, room, start, holidays): SuatChieu', '+ modify(id, edit, holidays): SuatChieu', '+ cancel(id): void'], col_x(3), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_PhongGhe', '', 'PhongChieuDAO\nGheDAO', ['+ search(key, type, state): List<PhongChieu>', '+ insert(room): void', '+ list(room): List<Ghe>', '+ insert(room, row, num, type, state): Ghe'], col_x(4), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_BangGia', '', 'BangGiaVeDAO\nLoaiThoiDiemDAO', ['+ all(): List<BangGiaVe>', '+ insert(seat, room, period, price): int', '+ updatePrice(id, price): void'], col_x(5), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_SanPham', '', 'SanPhamDAO', ['+ search(key, inStock): List<SanPham>', '+ insert(product): void', '+ updateInfo(product): void', '+ updatePrice(id, price): void'], col_x(6), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_Ve', '', 'VeDAO', ['+ inspect(code, type): Optional<ThongTin>', '+ confirm(maVe, truoc, sau, val): ThongTin'], col_x(7), y_dao, col_w, h_dao)
    add_design_box(root2, 'd_BanVe', '', 'BanVeDAO\nGiuGheDAO\nGiaoDichDAO', ['+ search(name, date, time): List<SuatBan>', '+ hold(show, seats, staff, id, min): GioHang', '+ pay(id, method, amount): List<ThongTin>', '+ cancel(id): void'], col_x(8), y_dao, col_w, 185)
    add_design_box(root2, 'd_BaoCao', '', 'BaoCaoDAO', ['+ theoThoiGian(range): TongHop', '+ theoPhim(range, film): List<TheoPhim>', '+ theoNhanVien(range, emp): List<TheoNhanVien>', '+ ve(range, film, show): ThongKeVe'], col_x(9), y_dao, col_w, h_dao)

    # Database Utility centered under DAO row
    add_design_box(root2, 'd_Database', 'utility', 'Database',
                   ['+ connect(): Connection'],
                   1700, 1200, 400, 100)

    # LAYER 4: DTO / Domain Model Layer (cinema.model)
    y_m1 = 1430
    h_m1 = 140
    add_design_box(root2, 'm_TaiKhoan', 'record', 'TaiKhoan', ['maTK: int', 'tenDangNhap: String', 'matKhauHash: String', 'trangThai: String', 'vaiTro: VaiTro', 'nhanVien: NhanVien'], 60, y_m1, 290, h_m1)
    add_design_box(root2, 'm_VaiTro', 'enumeration', 'VaiTro', ['ADMIN', 'NHAN_VIEN', '+ fromDatabase(s): VaiTro'], 380, y_m1, 250, h_m1)
    add_design_box(root2, 'm_NhanVien', 'record', 'NhanVien', ['maNV: String, hoTen: String', 'ngaySinh: LocalDate', 'soDienThoai, email: String', 'diaChi, chucVu: String'], 660, y_m1, 290, h_m1)
    add_design_box(root2, 'm_Phim', 'record', 'Phim', ['maPhim, tenPhim: String', 'thoiLuong, gioiHanTuoi: int', 'daoDien, poster: String', 'ngayKhoiChieu: LocalDate', 'theLoai: List<TheLoai>'], 980, y_m1, 300, h_m1)
    add_design_box(root2, 'm_TheLoai', 'record', 'TheLoai', ['maTheLoai: int', 'tenTheLoai: String'], 1310, y_m1, 230, h_m1)
    add_design_box(root2, 'm_SuatChieu', 'record', 'SuatChieu', ['maSuat, maLoaiThoiDiem: int', 'maPhim, maPhong: String', 'thoiGianBatDau: LocalDateTime', 'thoiGianTraPhong: LocalDateTime', 'trangThai: String'], 1570, y_m1, 320, h_m1)
    add_design_box(root2, 'm_PhongChieu', 'record', 'PhongChieu', ['maPhong, tenPhong: String', 'loaiPhong: LoaiPhong', 'thoiGianDonPhong: int', 'trangThai: String'], 1920, y_m1, 280, h_m1)
    add_design_box(root2, 'm_LoaiPhong', 'record', 'LoaiPhong', ['maLoaiPhong: int', 'tenLoaiPhong: String', 'dinhDangChieu: String'], 2230, y_m1, 240, h_m1)
    add_design_box(root2, 'm_Ghe', 'record', 'Ghe', ['maGhe, soGhe: int', 'maPhong, hangGhe: String', 'loaiGhe: LoaiGhe', 'trangThai: String'], 2500, y_m1, 280, h_m1)
    add_design_box(root2, 'm_LoaiGhe', 'record', 'LoaiGhe', ['maLoaiGhe: int', 'tenLoaiGhe: String'], 2810, y_m1, 230, h_m1)

    y_m2 = 1610
    h_m2 = 150
    add_design_box(root2, 'm_GheSuatChieu', 'record', 'GheSuatChieu', ['maSuat: int, ghe: Ghe', 'maGiaoDich: String', 'trangThai: String', 'thoiGianHetHanGiu: LocalDateTime', 'maGia: Integer, giaVeChot: BigDecimal'], 60, y_m2, 360, h_m2)
    add_design_box(root2, 'm_LoaiThoiDiem', 'record', 'LoaiThoiDiem', ['maLoaiThoiDiem: int', 'tenLoaiThoiDiem: String'], 450, y_m2, 250, h_m2)
    add_design_box(root2, 'm_BangGiaVe', 'record', 'BangGiaVe', ['maGia: int, giaVe: BigDecimal', 'loaiGhe: LoaiGhe', 'loaiPhong: LoaiPhong', 'loaiThoiDiem: LoaiThoiDiem'], 730, y_m2, 300, h_m2)
    add_design_box(root2, 'm_GiaoDich', 'record', 'GiaoDich', ['maGiaoDich: String, maNV: String', 'thoiGianTao: LocalDateTime', 'tongTien: BigDecimal', 'trangThai: String'], 1060, y_m2, 320, h_m2)
    add_design_box(root2, 'm_ThanhToan', 'record', 'ThanhToan', ['maThanhToan: int', 'maGiaoDich: String, phuongThuc: String', 'soTien: BigDecimal', 'thoiGianThanhToan: LocalDateTime'], 1410, y_m2, 340, h_m2)
    add_design_box(root2, 'm_Ve', 'record', 'Ve', ['maVe, maGiaoDich: String', 'maSuat, maGhe, maGia: int', 'maQR: String, giaVe: BigDecimal', 'hangGheLucPhatHanh, tenLoaiGheLucPhatHanh: String', 'soGheLucPhatHanh: int, thoiGianPhatHanh: LocalDateTime'], 1780, y_m2, 400, h_m2)
    add_design_box(root2, 'm_SanPham', 'record', 'SanPham', ['maSP, tenSP, moTa: String', 'donViTinh, tinhTrangKinhDoanh: String', 'giaBan: BigDecimal, soLuongTon: int'], 2210, y_m2, 350, h_m2)

    # EDGES: Layer 1 (GUI) -> Layer 2 (BUS)
    # Direct vertical calls (aligned columns)
    add_p2_edge(root2, 'v_DangNhap', 'b_DangNhap', label='«call»')
    add_p2_edge(root2, 'v_NhanVien', 'b_NhanVien', label='«call»')
    add_p2_edge(root2, 'v_Phim', 'b_Phim', label='«call»')
    add_p2_edge(root2, 'v_LichChieu', 'b_LichChieu', label='«call»')
    add_p2_edge(root2, 'v_PhongChieu', 'b_PhongChieu', label='«call»')
    add_p2_edge(root2, 'v_GiaVeFnB', 'b_GiaVe', label='«call»')
    # v_GiaVeFnB -> b_SanPham routed in corridor between GUI and BUS
    add_p2_edge(root2, 'v_GiaVeFnB', 'b_SanPham', label='«call»', points=[(col_x(5) + col_w/2, 310), (col_x(6) + col_w/2, 310)])
    add_p2_edge(root2, 'v_KiemTraVe', 'b_Ve', label='«call»')
    add_p2_edge(root2, 'v_BanVe', 'b_BanVe', label='«call»')
    add_p2_edge(root2, 'v_BaoCao', 'b_BaoCao', label='«call»')

    # EDGES: Layer 2 (BUS) -> Layer 3 (DAO)
    # Direct vertical calls for columns without helpers
    add_p2_edge(root2, 'b_NhanVien', 'd_NhanVien', label='«call»')
    add_p2_edge(root2, 'b_LichChieu', 'd_SuatChieu', label='«call»')
    add_p2_edge(root2, 'b_PhongChieu', 'd_PhongGhe', label='«call»')
    add_p2_edge(root2, 'b_SanPham', 'd_SanPham', label='«call»')

    # Routed calls for columns with helpers via wide 60px inter-column gaps
    add_p2_edge(root2, 'b_DangNhap', 'd_TaiKhoan', label='«call»',
                points=[(col_x(0) + col_w/2, 590), (col_x(0) + col_w + 30, 590), (col_x(0) + col_w + 30, 850), (col_x(0) + col_w/2, 850)])

    add_p2_edge(root2, 'b_Phim', 'd_PhimTheLoai', label='«call»',
                points=[(col_x(2) + col_w/2, 590), (col_x(2) + col_w + 30, 590), (col_x(2) + col_w + 30, 850), (col_x(2) + col_w/2, 850)])

    add_p2_edge(root2, 'b_GiaVe', 'd_BangGia', label='«call»',
                points=[(col_x(5) + col_w/2, 590), (col_x(5) + col_w + 30, 590), (col_x(5) + col_w + 30, 850), (col_x(5) + col_w/2, 850)])

    add_p2_edge(root2, 'b_Ve', 'd_Ve', label='«call»',
                points=[(col_x(7) + col_w/2, 590), (col_x(7) + col_w + 30, 590), (col_x(7) + col_w + 30, 850), (col_x(7) + col_w/2, 850)])

    add_p2_edge(root2, 'b_BanVe', 'd_BanVe', label='«call»',
                points=[(col_x(8) + col_w/2, 590), (col_x(8) + col_w + 30, 590), (col_x(8) + col_w + 30, 850), (col_x(8) + col_w/2, 850)])

    add_p2_edge(root2, 'b_BaoCao', 'd_BaoCao', label='«call»',
                points=[(col_x(9) + col_w/2, 590), (col_x(9) + col_w + 30, 590), (col_x(9) + col_w + 30, 850), (col_x(9) + col_w/2, 850)])

    # EDGES: Layer 3 (DAO) -> Database Utility
    # Routed cleanly in the open corridor between DAO row and d_Database
    dao_sources = [
        ('d_TaiKhoan', col_x(0) + 140, 1120, 1720),
        ('d_NhanVien', col_x(1) + 140, 1130, 1750),
        ('d_PhimTheLoai', col_x(2) + 140, 1140, 1780),
        ('d_SuatChieu', col_x(3) + 140, 1150, 1810),
        ('d_PhongGhe', col_x(4) + 140, 1160, 1840),
        ('d_BangGia', col_x(5) + 140, 1160, 1870),
        ('d_SanPham', col_x(6) + 140, 1150, 1900),
        ('d_Ve', col_x(7) + 140, 1140, 1930),
        ('d_BanVe', col_x(8) + 140, 1130, 1960),
        ('d_BaoCao', col_x(9) + 140, 1120, 1990),
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
