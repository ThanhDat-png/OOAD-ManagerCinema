import xml.etree.ElementTree as ET

def build_drawio():
    mxfile = ET.Element('mxfile', {'host': 'app.diagrams.net', 'pages': '2', 'agent': 'OOAD-Cinema'})

    # =========================================================================
    # PAGE 1: SoDoLop_PhanTich (Analysis Class Diagram)
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

    # Title Banner for Page 1
    title_box1 = ET.SubElement(root1, 'mxCell', {
        'id': 'p1_title',
        'value': 'SƠ ĐỒ LỚP Ở MỨC PHÂN TÍCH - HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM\n17 Lớp nghiệp vụ trích xuất từ 9 ca sử dụng và sơ đồ tuần tự theo chuẩn thiết kế hướng đối tượng',
        'style': 'text;html=1;strokeColor=#4a5568;fillColor=#edf2f7;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=1;fontSize=14;fontStyle=1;fontColor=#1a202c;',
        'vertex': '1', 'parent': '1'
    })
    ET.SubElement(title_box1, 'mxGeometry', {'x': '50', 'y': '20', 'width': '2450', 'height': '50', 'as': 'geometry'})

    def add_class_box(root, cid, name, attrs, methods, x, y, w, h_attr, h_meth, fill='#f8f9fa', stroke='#343a40'):
        total_h = 30 + h_attr + 8 + h_meth
        hdr_style = f"swimlane;fontStyle=1;align=center;verticalAlign=top;childLayout=stackLayout;horizontal=1;startSize=30;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;marginBottom=0;html=1;whiteSpace=wrap;fillColor={fill};strokeColor={stroke};fontSize=13;fontColor=#1a202c;"
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
            'style': 'text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=8;spacingRight=8;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=1;fontSize=11;fontColor=#2d3748;',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(attr_cell, 'mxGeometry', {'y': '30', 'width': str(w), 'height': str(h_attr), 'as': 'geometry'})

        # Separator line
        sep_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_sep',
            'value': '',
            'style': f'line;strokeWidth=1;fillColor=none;align=left;verticalAlign=middle;spacingTop=-1;spacingLeft=3;spacingRight=3;rotatable=0;labelPosition=right;points=[];portConstraint=eastwest;strokeColor={stroke};',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(sep_cell, 'mxGeometry', {'y': str(30 + h_attr), 'width': str(w), 'height': '8', 'as': 'geometry'})

        # Methods
        meth_cell = ET.SubElement(root, 'mxCell', {
            'id': f'{cid}_methods',
            'value': '\n'.join(methods),
            'style': 'text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=8;spacingRight=8;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=1;fontSize=11;fontColor=#2d3748;',
            'vertex': '1', 'parent': cid
        })
        ET.SubElement(meth_cell, 'mxGeometry', {'y': str(38 + h_attr), 'width': str(w), 'height': str(h_meth), 'as': 'geometry'})
        return cls_cell

    # Define 17 Classes for Page 1 with precise compartment heights
    # Row 1 (y=100): VaiTro, TaiKhoan, NhanVien, TheLoai, Phim, LoaiPhong, PhongChieu, Ghe, LoaiGhe
    add_class_box(root1, 'c_VaiTro', '«enumeration»\nVaiTro',
                  ['- ADMIN: VaiTro', '- NHAN_VIEN: VaiTro', '- maVaiTro: int', '- tenVaiTro: String'],
                  ['+ getTen(): String', '+ fromDatabase(ten: String): VaiTro'],
                  60, 100, 200, 75, 45, fill='#FEF3C7', stroke='#D97706')

    add_class_box(root1, 'c_TaiKhoan', 'TaiKhoan',
                  ['- maTK: int', '- tenDangNhap: String', '- matKhauHash: String', '- trangThai: String'],
                  ['+ dangHoatDong(): boolean', '+ doiMatKhau(mkMoi: String): void', '+ kiemTraDangNhap(mk: String): boolean'],
                  310, 100, 220, 75, 60, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_NhanVien', 'NhanVien',
                  ['- maNV: String', '- hoTen: String', '- ngaySinh: LocalDate', '- soDienThoai: String', '- email: String', '- diaChi: String', '- chucVu: String'],
                  ['+ capNhatThongTin(hoTen, sdt, email, diaChi, chucVu): void', '+ getHoTen(): String'],
                  580, 100, 250, 130, 45, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_TheLoai', 'TheLoai',
                  ['- maTheLoai: int', '- tenTheLoai: String'],
                  ['+ getTen(): String'],
                  900, 100, 180, 45, 30, fill='#FEF3C7', stroke='#D97706')

    add_class_box(root1, 'c_Phim', 'Phim',
                  ['- maPhim: String', '- tenPhim: String', '- thoiLuong: int', '- daoDien: String', '- ngayKhoiChieu: LocalDate', '- gioiHanTuoi: int', '- poster: String', '- trangThai: String', '- dinhDangChieu: String'],
                  ['+ ngungChieu(): void', '+ dangChieu(): boolean', '+ withTheLoai(theLoai: List<TheLoai>): Phim'],
                  1140, 100, 250, 160, 60, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_LoaiPhong', 'LoaiPhong',
                  ['- maLoaiPhong: int', '- tenLoaiPhong: String', '- dinhDangChieu: String'],
                  ['+ getDinhDang(): String'],
                  1460, 100, 200, 60, 30, fill='#FEF3C7', stroke='#D97706')

    add_class_box(root1, 'c_PhongChieu', 'PhongChieu',
                  ['- maPhong: String', '- tenPhong: String', '- thoiGianDonPhong: int', '- trangThai: String', '/ sucChua: int'],
                  ['+ tinhSucChua(): int', '+ dangHoatDong(): boolean'],
                  1720, 100, 220, 95, 45, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_Ghe', 'Ghe',
                  ['- maGhe: int', '- hangGhe: String', '- soGhe: int', '- trangThai: String'],
                  ['+ sanSangSuDung(): boolean', '+ chuyenBaoTri(): void'],
                  2000, 100, 200, 75, 45, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_LoaiGhe', 'LoaiGhe',
                  ['- maLoaiGhe: int', '- tenLoaiGhe: String'],
                  ['+ getTen(): String'],
                  2260, 100, 180, 45, 30, fill='#FEF3C7', stroke='#D97706')

    # Row 2 (y=500): LoaiThoiDiem, SuatChieu, GheSuatChieu, BangGiaVe
    add_class_box(root1, 'c_LoaiThoiDiem', 'LoaiThoiDiem',
                  ['- maLoaiThoiDiem: int', '- tenLoaiThoiDiem: String'],
                  ['+ getTen(): String'],
                  900, 500, 180, 45, 30, fill='#FEF3C7', stroke='#D97706')

    add_class_box(root1, 'c_SuatChieu', 'SuatChieu',
                  ['- maSuat: int', '- thoiGianBatDau: LocalDateTime', '- thoiGianTraPhong: LocalDateTime', '- trangThai: String'],
                  ['+ kiemTraKhungGio(batDau, ketThuc): boolean', '+ coTheBanVe(): boolean', '+ huySuat(): void'],
                  1140, 500, 250, 75, 60, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_GheSuatChieu', 'GheSuatChieu',
                  ['- trangThai: String', '- thoiGianHetHanGiu: LocalDateTime', '- giaVeChot: BigDecimal'],
                  ['+ kiemTraHetHanGiu(): boolean', '+ giuGhe(hanGiu: LocalDateTime): void', '+ xacNhanBan(): void', '+ giaiPhongGhe(): void'],
                  1460, 500, 260, 60, 75, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_BangGiaVe', 'BangGiaVe',
                  ['- maGia: int', '- giaVe: BigDecimal'],
                  ['+ capNhatGia(giaMoi: BigDecimal): void'],
                  1880, 500, 220, 45, 30, fill='#FEF3C7', stroke='#D97706')

    # Row 3 (y=900): SanPham, ThanhToan, GiaoDich, Ve
    add_class_box(root1, 'c_SanPham', 'SanPham',
                  ['- maSP: String', '- tenSP: String', '- moTa: String', '- donViTinh: String', '- giaBan: BigDecimal', '- soLuongTon: int', '- tinhTrangKinhDoanh: String'],
                  ['+ capNhatTonKho(soLuongThayDoi: int): void', '+ conHang(): boolean'],
                  60, 900, 220, 130, 45, fill='#FEF3C7', stroke='#D97706')

    add_class_box(root1, 'c_ThanhToan', 'ThanhToan',
                  ['- maThanhToan: int', '- phuongThuc: String', '- soTien: BigDecimal', '- thoiGianThanhToan: LocalDateTime'],
                  ['+ xacNhanThanhToan(): boolean'],
                  330, 900, 220, 75, 30, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_GiaoDich', 'GiaoDich',
                  ['- maGiaoDich: String', '- thoiGianTao: LocalDateTime', '- tongTien: BigDecimal', '- trangThai: String'],
                  ['+ tinhTongTien(): BigDecimal', '+ hoanTatGiaoDich(): void', '+ huyGiaoDich(): void'],
                  600, 900, 250, 75, 60, fill='#EBF8FF', stroke='#3182CE')

    add_class_box(root1, 'c_Ve', 'Ve',
                  ['- maVe: String', '- maQR: String', '- giaVe: BigDecimal', '- trangThai: String', '- thoiGianPhatHanh: LocalDateTime', '- hangGheLucPhatHanh: String', '- soGheLucPhatHanh: int', '- tenLoaiGheLucPhatHanh: String'],
                  ['+ kiemTraHopLe(): boolean', '+ danhDauSuDung(): void'],
                  930, 900, 270, 150, 45, fill='#EBF8FF', stroke='#3182CE')

    # Edge helper
    edge_cnt1 = 0
    def add_p1_edge(root, source, target, s_mult='', t_mult='', label='', start_arrow='none', end_arrow='none', start_fill='0', end_fill='0', start_size='14', end_size='14', dashed='0', points=None):
        nonlocal edge_cnt1
        edge_cnt1 += 1
        eid = f'edge_{edge_cnt1}'
        style = f"edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeWidth=1.5;strokeColor=#2d3748;dashed={dashed};startArrow={start_arrow};endArrow={end_arrow};startFill={start_fill};endFill={end_fill};startSize={start_size};endSize={end_size};"
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
                'style': 'edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontSize=11;fontStyle=1;',
                'vertex': '1', 'connectable': '0', 'parent': eid
            })
            ET.SubElement(lbl_s, 'mxGeometry', {'x': '-0.8', 'relative': '1', 'as': 'geometry'})

        if t_mult:
            lbl_t = ET.SubElement(root, 'mxCell', {
                'id': f'{eid}_t',
                'value': t_mult,
                'style': 'edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontSize=11;fontStyle=1;',
                'vertex': '1', 'connectable': '0', 'parent': eid
            })
            ET.SubElement(lbl_t, 'mxGeometry', {'x': '0.8', 'relative': '1', 'as': 'geometry'})

        return edge_cell

    # Define all 21 Edges with zero box collisions
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

    # 7. NhanVien - GiaoDich (Vertical straight line through open x=680..720)
    add_p1_edge(root1, 'c_NhanVien', 'c_GiaoDich', s_mult='1', t_mult='0..*', label='thucHien')

    # 8. Phim - SuatChieu (Vertical straight line from Phim to SuatChieu at x=1260)
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

    # Note on Analysis Class Diagram
    note_box1 = ET.SubElement(root1, 'mxCell', {
        'id': 'p1_notes',
        'value': 'GHI CHÚ THIẾT KẾ PHÂN TÍCH:\n'
                 '1. Sức chứa phòng chiếu /sucChua là thuộc tính dẫn xuất, tính từ tổng ghế vật lý sẵn sàng phục vụ.\n'
                 '2. Lớp Ve chứa 3 thuộc tính lưu vết hangGheLucPhatHanh, soGheLucPhatHanh, tenLoaiGheLucPhatHanh để bảo toàn lịch sử vé đã phát hành.\n'
                 '3. Lớp SanPham đứng độc lập trong phạm vi hiện tại do ca sử dụng 6 và 9 quản lý danh mục và tồn kho ăn uống độc lập với vé.\n'
                 '4. Các khóa ngoại cơ sở dữ liệu không lặp lại trong thuộc tính lớp vì đã được thể hiện bằng quan hệ kết hợp theo chuẩn thiết kế.',
        'style': 'shape=note;whiteSpace=wrap;html=1;backgroundOutline=1;darkOpacity=0.05;fillColor=#FFFBEB;strokeColor=#D97706;align=left;verticalAlign=top;spacingLeft=12;spacingTop=8;fontSize=11;fontColor=#92400E;',
        'vertex': '1', 'parent': '1'
    })
    ET.SubElement(note_box1, 'mxGeometry', {'x': '50', 'y': '1180', 'width': '2450', 'height': '110', 'as': 'geometry'})


    # =========================================================================
    # PAGE 2: SoDoLop_ThietKe_3Tang (Design Class Diagram - 3-Layer Architecture)
    # =========================================================================
    diag2 = ET.SubElement(mxfile, 'diagram', {'id': 'diag_design_3tier', 'name': 'SoDoLop_ThietKe_3Tang'})
    model2 = ET.SubElement(diag2, 'mxGraphModel', {
        'dx': '2400', 'dy': '1800', 'grid': '1', 'gridSize': '10', 'guides': '1',
        'tooltips': '1', 'connect': '1', 'arrows': '1', 'fold': '1', 'page': '1',
        'pageScale': '1', 'pageWidth': '2800', 'pageHeight': '2100', 'math': '0', 'shadow': '0'
    })
    root2 = ET.SubElement(model2, 'root')
    ET.SubElement(root2, 'mxCell', {'id': '0'})
    ET.SubElement(root2, 'mxCell', {'id': '1', 'parent': '0'})

    # Title Banner for Page 2
    p2_title = ET.SubElement(root2, 'mxCell', {
        'id': 'p2_title_box',
        'value': 'SƠ ĐỒ LỚP Ở MỨC THIẾT KẾ - KIẾN TRÚC BA TẦNG KẾT HỢP ĐỐI TƯỢNG TRUYỀN TẢI DỮ LIỆU\nTuân thủ bài giảng thiết kế hệ thống và mã nguồn src/main/java/cinema',
        'style': 'text;html=1;strokeColor=#2b6cb0;fillColor=#ebf8ff;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=1;fontSize=14;fontStyle=1;fontColor=#1a202c;',
        'vertex': '1', 'parent': '1'
    })
    ET.SubElement(p2_title, 'mxGeometry', {'x': '40', 'y': '20', 'width': '2720', 'height': '50', 'as': 'geometry'})

    def add_design_box(root, cid, stereotype, name, methods, x, y, w, h, fill='#ffffff', stroke='#343a40', subtitle=''):
        val = ""
        if stereotype:
            val += f"&lt;i style='font-size:10px;color:#4a5568;'&gt;«{stereotype}»&lt;/i&gt;&lt;br&gt;"
        val += f"&lt;b&gt;{name}&lt;/b&gt;"
        if subtitle:
            val += f"&lt;br&gt;&lt;span style='font-size:10px;color:#718096;'&gt;{subtitle}&lt;/span&gt;"
        if methods:
            val += "&lt;hr style='margin:4px 0;'&gt;&lt;div style='text-align:left;font-size:10px;padding:2px 4px;'&gt;" + "&lt;br&gt;".join(methods) + "&lt;/div&gt;"
        cls_cell = ET.SubElement(root, 'mxCell', {
            'id': cid,
            'value': val,
            'style': f"rounded=1;whiteSpace=wrap;html=1;align=center;verticalAlign=top;fillColor={fill};strokeColor={stroke};fontSize=11;spacingTop=6;",
            'vertex': '1', 'parent': '1'
        })
        ET.SubElement(cls_cell, 'mxGeometry', {'x': str(x), 'y': str(y), 'width': str(w), 'height': str(h), 'as': 'geometry'})
        return cls_cell

    def add_package_frame(root, pid, title, x, y, w, h, fill='#f7fafc', stroke='#cbd5e0'):
        pkg = ET.SubElement(root, 'mxCell', {
            'id': pid,
            'value': title,
            'style': f"swimlane;fontStyle=1;align=left;spacingLeft=12;verticalAlign=top;childLayout=stackLayout;horizontal=1;startSize=30;horizontalStack=0;resizeParent=0;collapsible=0;html=1;whiteSpace=wrap;fillColor={fill};strokeColor={stroke};fontSize=13;fontColor=#2d3748;",
            'vertex': '1', 'parent': '1'
        })
        ET.SubElement(pkg, 'mxGeometry', {'x': str(x), 'y': str(y), 'width': str(w), 'height': str(h), 'as': 'geometry'})
        return pkg

    # =========================================================================
    # LAYER 1: Presentation Layer (cinema.view)
    # =========================================================================
    add_package_frame(root2, 'pkg_gui', 'TẦNG GIAO DIỆN - package cinema.view', 40, 90, 2720, 230, fill='#F0FFF4', stroke='#38A169')

    add_design_box(root2, 'v_DangNhap', '', 'DangNhapWindow', ['+ DangNhapWindow(service)', '- authenticate(service): void'], 60, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Màn hình đăng nhập UC01')
    add_design_box(root2, 'v_Cinema', '', 'CinemaWindow', ['+ open(session): void'], 310, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Khung chính ứng dụng')
    add_design_box(root2, 'v_NhanVien', '', 'NhanVienPanel', ['+ NhanVienPanel(session)', '- refresh(): void'], 560, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Quản lý nhân sự UC02')
    add_design_box(root2, 'v_Phim', '', 'PhimPanel', ['+ PhimPanel(session)', '- refresh(): void'], 810, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Quản lý phim UC03')
    add_design_box(root2, 'v_LichChieu', '', 'LichChieuPanel', ['+ LichChieuPanel(session)', '- refresh(): void'], 1060, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Lập lịch chiếu UC04')
    add_design_box(root2, 'v_PhongChieu', '', 'PhongChieuPanel', ['+ PhongChieuPanel(session)', '- refresh(): void'], 1310, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Phòng và Ghế UC05')
    add_design_box(root2, 'v_GiaVeFnB', '', 'GiaVeFnBPanel', ['+ GiaVeFnBPanel(session)', '- refresh(): void'], 1560, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Giá vé và Sản phẩm UC06')
    add_design_box(root2, 'v_KiemTraVe', '', 'KiemTraVePanel', ['+ KiemTraVePanel(session)', '- check(): void'], 1810, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Soát vé QR UC07')
    add_design_box(root2, 'v_BanVe', '', 'BanVePanel', ['+ BanVePanel(session)', '+ requestClose(): boolean'], 2060, 130, 240, 85, fill='#ffffff', stroke='#38A169', subtitle='Bán vé quầy UC08')
    add_design_box(root2, 'v_BaoCao', '', 'BaoCaoPanel', ['+ BaoCaoPanel(session)', '- timeTab(): JPanel'], 2320, 130, 230, 85, fill='#ffffff', stroke='#38A169', subtitle='Thống kê báo cáo UC09')
    add_design_box(root2, 'v_UIHelpers', '', 'cinema.view.ui.*', ['Tokens, Skin, Theme', 'SeatVisual, SalesViews', 'ReadOnlyViews, Forms'], 2570, 130, 170, 85, fill='#F7FAFC', stroke='#718096', subtitle='Thư viện hỗ trợ giao diện')

    # =========================================================================
    # LAYER 2: Business Logic Layer (cinema.controller)
    # =========================================================================
    add_package_frame(root2, 'pkg_bus', 'TẦNG NGHIỆP VỤ - package cinema.controller.*', 40, 380, 2720, 310, fill='#EBF8FF', stroke='#3182CE')

    add_design_box(root2, 'b_DangNhap', '', 'DangNhapBUS', ['+ login(username, password): PhienDangNhap'], 60, 420, 230, 105, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.auth')
    add_design_box(root2, 'b_NhanVien', '', 'NhanVienBUS', ['+ search(keyword): List<NhanVien>', '+ create(input): NhanVien', '+ update(input): void', '+ delete(id): void', '+ detail(id): NhanVien'], 310, 420, 230, 125, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.staff')
    add_design_box(root2, 'b_Phim', '', 'PhimBUS', ['+ genres(): List<TheLoai>', '+ search(name, genre, status): List<Phim>', '+ create(input, image): Phim', '+ rename(id, name): void', '+ stop(id): void'], 560, 420, 230, 135, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.film')
    add_design_box(root2, 'b_LichChieu', '', 'LichChieuBUS', ['+ list(date, room): List<SuatChieu>', '+ create(film, room, start): SuatChieu', '+ changeFilm(id, film): SuatChieu', '+ cancel(id): void'], 810, 420, 230, 120, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.schedule')
    add_design_box(root2, 'b_PhongChieu', '', 'PhongChieuBUS', ['+ search(text, type, state): List<PhongChieu>', '+ create(name, type, clean, state): PhongChieu', '+ seats(room): List<Ghe>', '+ addSeat(room, row, num, type, state): Ghe'], 1060, 420, 230, 125, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.room')
    add_design_box(root2, 'b_GiaVe', '', 'GiaVeBUS', ['+ list(): List<BangGiaVe>', '+ create(seat, room, period, price): BangGiaVe', '+ updatePrice(id, price): BangGiaVe'], 1310, 420, 230, 110, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.pricing')
    add_design_box(root2, 'b_SanPham', '', 'SanPhamBUS', ['+ search(text, inStock): List<SanPham>', '+ create(input): void', '+ updateInfo(input): void', '+ updatePrice(id, amount): void'], 1560, 420, 230, 120, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.pricing')
    add_design_box(root2, 'b_Ve', '', 'VeBUS', ['+ rooms(): List<PhongChieu>', '+ shows(date, room): List<SuatChieu>', '+ check(ticket, type, room, show): KetQua', '+ confirm(ticket, room, show): ThongTin'], 1810, 420, 230, 120, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.admission')
    add_design_box(root2, 'b_BanVe', '', 'BanVeBUS', ['+ shows(name, date, time): List<SuatBan>', '+ hold(show, seats, txId): GioHang', '+ pay(txId, method, amount): List<ThongTin>', '+ cancel(txId): void'], 2060, 420, 240, 120, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.sales')
    add_design_box(root2, 'b_BaoCao', '', 'BaoCaoBUS', ['+ theoThoiGian(range): TongHop', '+ theoPhim(range, film): List<TheoPhim>', '+ theoNhanVien(range, emp): List<TheoNhanVien>', '+ ve(range, film, show): ThongKeVe'], 2320, 420, 230, 120, fill='#ffffff', stroke='#3182CE', subtitle='cinema.controller.revenue')

    # BUS Helpers
    add_design_box(root2, 'h_Auth', '', 'PasswordHasher\nPhienDangNhap', ['+ hash(password): String', '+ matches(password, stored): boolean', '+ laAdmin(): boolean'], 60, 570, 230, 90, fill='#F7FAFC', stroke='#4A5568', subtitle='Bảo mật đăng nhập')
    add_design_box(root2, 'h_Film', '', 'PosterStore', ['+ importImage(source): String', '+ resolve(stored): Path', '+ discard(stored): void'], 560, 570, 230, 90, fill='#F7FAFC', stroke='#4A5568', subtitle='Lưu trữ tệp áp phích')
    add_design_box(root2, 'h_Pricing', '', 'MoneyRules', ['+ positive(value, message): BigDecimal'], 1310, 570, 230, 75, fill='#F7FAFC', stroke='#4A5568', subtitle='Quy tắc tiền tệ')
    add_design_box(root2, 'h_Admission', '', 'DocMaQR\nThoiGianVaoPhong', ['+ read(file): String', '+ load(): ThoiGianVaoPhong'], 1810, 570, 230, 85, fill='#F7FAFC', stroke='#4A5568', subtitle='Xác thực soát vé')
    add_design_box(root2, 'h_Sales', '', 'InVe', ['+ preview(info): BufferedImage', '+ print(g, format, page): int'], 2060, 570, 240, 85, fill='#F7FAFC', stroke='#4A5568', subtitle='Định dạng in vé')
    add_design_box(root2, 'h_Revenue', '', 'KhoangThongKe', ['+ ngay(date): KhoangThongKe', '+ thang(month): KhoangThongKe', '+ nam(year): KhoangThongKe'], 2320, 570, 230, 90, fill='#F7FAFC', stroke='#4A5568', subtitle='Lọc khoảng thời gian')

    # =========================================================================
    # LAYER 3: Data Access Layer (cinema.dao & cinema.db)
    # =========================================================================
    add_package_frame(root2, 'pkg_dao', 'TẦNG TRUY XUẤT DỮ LIỆU - package cinema.dao và cinema.db', 40, 740, 2720, 290, fill='#FFF5F5', stroke='#E53E3E')

    add_design_box(root2, 'd_TaiKhoan', '', 'TaiKhoanDAO', ['+ findByUsername(user): Optional<TaiKhoan>'], 60, 780, 220, 85, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_NhanVien', '', 'NhanVienDAO', ['+ search(keyword): List<NhanVien>', '+ insert(emp, hash, role): void', '+ update(emp): void', '+ delete(id): void'], 300, 780, 220, 115, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_PhimTheLoai', '', 'PhimDAO\nTheLoaiDAO', ['+ search(name, genre, status): List<Phim>', '+ insert(film): void', '+ stop(id): void', '+ all(): List<TheLoai>'], 540, 780, 230, 115, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_SuatChieu', '', 'SuatChieuDAO', ['+ list(date, room): List<SuatChieu>', '+ create(film, room, start, holidays): SuatChieu', '+ modify(id, edit, holidays): SuatChieu', '+ cancel(id): void'], 790, 780, 220, 115, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_PhongGhe', '', 'PhongChieuDAO\nGheDAO', ['+ search(key, type, state): List<PhongChieu>', '+ insert(room): void', '+ list(room): List<Ghe>', '+ insert(room, row, num, type, state): Ghe'], 1030, 780, 220, 115, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_BangGia', '', 'BangGiaVeDAO\nLoaiThoiDiemDAO', ['+ all(): List<BangGiaVe>', '+ insert(seat, room, period, price): int', '+ updatePrice(id, price): void'], 1270, 780, 230, 105, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_SanPham', '', 'SanPhamDAO', ['+ search(key, inStock): List<SanPham>', '+ insert(product): void', '+ updateInfo(product): void', '+ updatePrice(id, price): void'], 1520, 780, 220, 115, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_Ve', '', 'VeDAO', ['+ inspect(code, type): Optional<ThongTin>', '+ confirm(maVe, truoc, sau, val): ThongTin'], 1760, 780, 220, 95, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_BanVe', '', 'BanVeDAO\nGiuGheDAO\nGiaoDichDAO', ['+ search(name, date, time): List<SuatBan>', '+ hold(show, seats, staff, id, min): GioHang', '+ pay(id, method, amount): List<ThongTin>', '+ cancel(id): void'], 2000, 780, 240, 115, fill='#ffffff', stroke='#E53E3E')
    add_design_box(root2, 'd_BaoCao', '', 'BaoCaoDAO', ['+ theoThoiGian(range): TongHop', '+ theoPhim(range, film): List<TheoPhim>', '+ theoNhanVien(range, emp): List<TheoNhanVien>', '+ ve(range, film, show): ThongKeVe'], 2260, 780, 220, 115, fill='#ffffff', stroke='#E53E3E')

    # Utility Database centered under DAO row
    add_design_box(root2, 'd_Database', 'utility', 'Database',
                   ['+ connect(): Connection'],
                   1180, 920, 340, 80, fill='#FEFCBF', stroke='#B7791F', subtitle='Kết nối MySQL qua JDBC từ config/database.properties')

    # =========================================================================
    # LAYER 4: DTO / Domain Model Layer (cinema.model)
    # =========================================================================
    add_package_frame(root2, 'pkg_dto', 'MÔ HÌNH THỰC THỂ DỮ LIỆU TRUYỀN TẢI - package cinema.model', 40, 1080, 2720, 520, fill='#FAF5FF', stroke='#805AD5')

    # Model Row 1 (10 classes)
    add_design_box(root2, 'm_TaiKhoan', 'record', 'TaiKhoan', ['maTK: int', 'tenDangNhap: String', 'matKhauHash: String', 'trangThai: String', 'vaiTro: VaiTro', 'nhanVien: NhanVien'], 60, 1120, 270, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_VaiTro', 'enumeration', 'VaiTro', ['ADMIN', 'NHAN_VIEN', '+ fromDatabase(s): VaiTro'], 350, 1120, 220, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_NhanVien', 'record', 'NhanVien', ['maNV: String, hoTen: String', 'ngaySinh: LocalDate', 'soDienThoai, email: String', 'diaChi, chucVu: String'], 590, 1120, 260, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_Phim', 'record', 'Phim', ['maPhim, tenPhim: String', 'thoiLuong, gioiHanTuoi: int', 'daoDien, poster: String', 'ngayKhoiChieu: LocalDate', 'theLoai: List<TheLoai>'], 870, 1120, 270, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_TheLoai', 'record', 'TheLoai', ['maTheLoai: int', 'tenTheLoai: String'], 1160, 1120, 200, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_SuatChieu', 'record', 'SuatChieu', ['maSuat, maLoaiThoiDiem: int', 'maPhim, maPhong: String', 'thoiGianBatDau: LocalDateTime', 'thoiGianTraPhong: LocalDateTime', 'trangThai: String'], 1380, 1120, 290, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_PhongChieu', 'record', 'PhongChieu', ['maPhong, tenPhong: String', 'loaiPhong: LoaiPhong', 'thoiGianDonPhong: int', 'trangThai: String'], 1690, 1120, 250, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_LoaiPhong', 'record', 'LoaiPhong', ['maLoaiPhong: int', 'tenLoaiPhong: String', 'dinhDangChieu: String'], 1960, 1120, 220, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_Ghe', 'record', 'Ghe', ['maGhe, soGhe: int', 'maPhong, hangGhe: String', 'loaiGhe: LoaiGhe', 'trangThai: String'], 2200, 1120, 240, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_LoaiGhe', 'record', 'LoaiGhe', ['maLoaiGhe: int', 'tenLoaiGhe: String'], 2460, 1120, 200, 130, fill='#ffffff', stroke='#805AD5')

    # Model Row 2 (7 classes)
    add_design_box(root2, 'm_GheSuatChieu', 'record', 'GheSuatChieu', ['maSuat: int, ghe: Ghe', 'maGiaoDich: String', 'trangThai: String', 'thoiGianHetHanGiu: LocalDateTime', 'maGia: Integer, giaVeChot: BigDecimal'], 60, 1280, 340, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_LoaiThoiDiem', 'record', 'LoaiThoiDiem', ['maLoaiThoiDiem: int', 'tenLoaiThoiDiem: String'], 420, 1280, 230, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_BangGiaVe', 'record', 'BangGiaVe', ['maGia: int, giaVe: BigDecimal', 'loaiGhe: LoaiGhe', 'loaiPhong: LoaiPhong', 'loaiThoiDiem: LoaiThoiDiem'], 670, 1280, 270, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_GiaoDich', 'record', 'GiaoDich', ['maGiaoDich: String, maNV: String', 'thoiGianTao: LocalDateTime', 'tongTien: BigDecimal', 'trangThai: String'], 960, 1280, 290, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_ThanhToan', 'record', 'ThanhToan', ['maThanhToan: int', 'maGiaoDich: String, phuongThuc: String', 'soTien: BigDecimal', 'thoiGianThanhToan: LocalDateTime'], 1270, 1280, 310, 130, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_Ve', 'record', 'Ve', ['maVe, maGiaoDich: String', 'maSuat, maGhe, maGia: int', 'maQR: String, giaVe: BigDecimal', 'hangGheLucPhatHanh, tenLoaiGheLucPhatHanh: String', 'soGheLucPhatHanh: int, thoiGianPhatHanh: LocalDateTime'], 1600, 1280, 380, 140, fill='#ffffff', stroke='#805AD5')
    add_design_box(root2, 'm_SanPham', 'record', 'SanPham', ['maSP, tenSP, moTa: String', 'donViTinh, tinhTrangKinhDoanh: String', 'giaBan: BigDecimal, soLuongTon: int'], 2000, 1280, 320, 130, fill='#ffffff', stroke='#805AD5')

    # Edge helper for Page 2
    edge_p2_cnt = 0
    def add_p2_edge(root, source, target, label='', points=None):
        nonlocal edge_p2_cnt
        edge_p2_cnt += 1
        eid = f'e2_{edge_p2_cnt}'
        edge_cell = ET.SubElement(root, 'mxCell', {
            'id': eid,
            'value': label,
            'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;dashed=1;strokeWidth=1.5;strokeColor=#4A5568;endArrow=open;endFill=0;fontSize=11;fontStyle=2;',
            'edge': '1', 'parent': '1',
            'source': source, 'target': target
        })
        geom = ET.SubElement(edge_cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})
        if points:
            arr = ET.SubElement(geom, 'Array', {'as': 'points'})
            for px, py in points:
                ET.SubElement(arr, 'mxPoint', {'x': str(px), 'y': str(py)})
        return edge_cell

    # Layer 1 (GUI) -> Layer 2 (BUS) calls
    add_p2_edge(root2, 'v_DangNhap', 'b_DangNhap', label='«call»')
    add_p2_edge(root2, 'v_NhanVien', 'b_NhanVien', label='«call»', points=[(675, 340), (425, 340)])
    add_p2_edge(root2, 'v_Phim', 'b_Phim', label='«call»', points=[(925, 345), (675, 345)])
    add_p2_edge(root2, 'v_LichChieu', 'b_LichChieu', label='«call»', points=[(1175, 350), (925, 350)])
    add_p2_edge(root2, 'v_PhongChieu', 'b_PhongChieu', label='«call»', points=[(1425, 355), (1175, 355)])
    add_p2_edge(root2, 'v_GiaVeFnB', 'b_GiaVe', label='«call»', points=[(1675, 360), (1425, 360)])
    add_p2_edge(root2, 'v_GiaVeFnB', 'b_SanPham', label='«call»')
    add_p2_edge(root2, 'v_KiemTraVe', 'b_Ve', label='«call»')
    add_p2_edge(root2, 'v_BanVe', 'b_BanVe', label='«call»')
    add_p2_edge(root2, 'v_BaoCao', 'b_BaoCao', label='«call»')

    # Layer 2 (BUS) -> Layer 3 (DAO) calls (routed via dedicated corridors to avoid helper boxes)
    add_p2_edge(root2, 'b_DangNhap', 'd_TaiKhoan', label='«call»', points=[(300, 480), (300, 720), (170, 720)])
    add_p2_edge(root2, 'b_NhanVien', 'd_NhanVien', label='«call»')
    add_p2_edge(root2, 'b_Phim', 'd_PhimTheLoai', label='«call»', points=[(800, 480), (800, 720), (655, 720)])
    add_p2_edge(root2, 'b_LichChieu', 'd_SuatChieu', label='«call»')
    add_p2_edge(root2, 'b_PhongChieu', 'd_PhongGhe', label='«call»')
    add_p2_edge(root2, 'b_GiaVe', 'd_BangGia', label='«call»', points=[(1550, 480), (1550, 720), (1385, 720)])
    add_p2_edge(root2, 'b_SanPham', 'd_SanPham', label='«call»')
    add_p2_edge(root2, 'b_Ve', 'd_Ve', label='«call»', points=[(2050, 480), (2050, 720), (1870, 720)])
    add_p2_edge(root2, 'b_BanVe', 'd_BanVe', label='«call»', points=[(2310, 480), (2310, 720), (2120, 720)])
    add_p2_edge(root2, 'b_BaoCao', 'd_BaoCao', label='«call»', points=[(2565, 480), (2565, 720), (2370, 720)])

    # Layer 3 (DAO) -> Database Singleton via dedicated routing in the open band (y=895)
    # DAOs to the left of Database (x <= 1180): drop to y=895, turn right to Database
    # DAOs to the right of Database (x >= 1520): drop to y=895, turn left to Database
    dao_sources = [
        ('d_TaiKhoan', 170, 895, 1220),
        ('d_NhanVien', 410, 895, 1250),
        ('d_PhimTheLoai', 655, 895, 1280),
        ('d_SuatChieu', 900, 895, 1310),
        ('d_PhongGhe', 1140, 895, 1340),
        ('d_BangGia', 1385, 895, 1360),
        ('d_SanPham', 1630, 895, 1400),
        ('d_Ve', 1870, 895, 1430),
        ('d_BanVe', 2120, 895, 1460),
        ('d_BaoCao', 2370, 895, 1490)
    ]
    for d_id, x_start, y_bus, x_end in dao_sources:
        add_p2_edge(root2, d_id, 'd_Database', label='', points=[(x_start, y_bus), (x_end, y_bus)])

    # DTO Annotation Box on Page 2
    dto_flow_box = ET.SubElement(root2, 'mxCell', {
        'id': 'p2_dto_note',
        'value': 'LUỒNG TRAO ĐỔI DỮ LIỆU QUA MÔ HÌNH ĐỐI TƯỢNG TRUYỀN TẢI DỮ LIỆU:\n'
                 '1. Các lớp trong package cinema.model được định nghĩa bằng cấu trúc bất biến record của Java 17 và enum VaiTro.\n'
                 '2. Đối tượng dữ liệu được dùng làm tham số đầu vào và kiểu trả về xuyên suốt giữa các tầng: Giao diện -> Nghiệp vụ -> Truy xuất dữ liệu.\n'
                 '3. Tầng giao diện chỉ nhận và hiển thị đối tượng dữ liệu, không truy cập trực tiếp tầng truy xuất hay cơ sở dữ liệu.\n'
                 '4. Tầng nghiệp vụ thực thi các quy tắc kiểm tra tính hợp lệ trước khi gọi tầng truy xuất dữ liệu.\n'
                 '5. Tầng truy xuất ánh xạ giữa các bảng cơ sở dữ liệu MySQL và các đối tượng dữ liệu qua câu lệnh SQL chuẩn bị sẵn.',
        'style': 'shape=note;whiteSpace=wrap;html=1;backgroundOutline=1;darkOpacity=0.05;fillColor=#FEFCBF;strokeColor=#B7791F;align=left;verticalAlign=top;spacingLeft=12;spacingTop=8;fontSize=11;fontColor=#744210;',
        'vertex': '1', 'parent': '1'
    })
    ET.SubElement(dto_flow_box, 'mxGeometry', {'x': '40', 'y': '1630', 'width': '2720', 'height': '120', 'as': 'geometry'})

    tree = ET.ElementTree(mxfile)
    ET.indent(tree, space='  ', level=0)
    return tree

if __name__ == '__main__':
    tree = build_drawio()
    out_path = r'c:\Users\giuag\.gemini\antigravity\scratch\OOAD-ManagerCinema\docs\SoDoLop_RapChieuPhim.drawio'
    tree.write(out_path, encoding='utf-8', xml_declaration=True)
    print(f'Successfully wrote {out_path}')
