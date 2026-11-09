import xml.etree.ElementTree as ET

def build_drawio():
    mxfile = ET.Element('mxfile', {'host': 'app.diagrams.net', 'pages': '1', 'agent': 'OOAD-Cinema'})

    # =========================================================================
    # SINGLE PAGE: SoDoLop (Entity / Data Class Diagram)
    # Strictly Black and White, sharp rectangular corners, plain text UML
    # Clean 2D spatial layout radiating around central domain classes
    # Solely 17 Data/Entity Classes mapped to database entities (No Controller, No GUI)
    # =========================================================================
    diag1 = ET.SubElement(mxfile, 'diagram', {'id': 'diag_class', 'name': 'SoDoLop'})
    model1 = ET.SubElement(diag1, 'mxGraphModel', {
        'dx': '2000', 'dy': '1400', 'grid': '1', 'gridSize': '10', 'guides': '1',
        'tooltips': '1', 'connect': '1', 'arrows': '1', 'fold': '1', 'page': '1',
        'pageScale': '1', 'pageWidth': '2350', 'pageHeight': '1450', 'math': '0', 'shadow': '0'
    })
    root1 = ET.SubElement(model1, 'root')
    ET.SubElement(root1, 'mxCell', {'id': '0'})
    ET.SubElement(root1, 'mxCell', {'id': '1', 'parent': '0'})

    def add_class_box(root, cid, name, attrs, methods, x, y, w, h_attr, h_meth):
        start_size = 40 if '\n' in name else 28
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
    # 17 Domain / Entity Classes (2D Spatial Layout)
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

    # Edge helper for Class Diagram
    edge_cnt1 = 0
    def add_edge(root, source, target, s_mult='', t_mult='', label='', start_arrow='none', end_arrow='none', start_fill='0', end_fill='0', start_size='14', end_size='14', dashed='0', points=None):
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

    # 21 Structural Relationships (Orthogonal, Pure Black & White UML, 0 Collisions)
    # 1. VaiTro - TaiKhoan (Vertical straight)
    add_edge(root1, 'c_VaiTro', 'c_TaiKhoan', s_mult='1', t_mult='0..*', label='')

    # 2. NhanVien - TaiKhoan (Horizontal straight)
    add_edge(root1, 'c_NhanVien', 'c_TaiKhoan', s_mult='1', t_mult='0..1', label='')

    # 3. TheLoai - Phim (Horizontal straight)
    add_edge(root1, 'c_TheLoai', 'c_Phim', s_mult='1..*', t_mult='1..*', label='')

    # 4. LoaiPhong - PhongChieu (Horizontal straight)
    add_edge(root1, 'c_LoaiPhong', 'c_PhongChieu', s_mult='1', t_mult='0..*', label='')

    # 5. PhongChieu - Ghe (Aggregation: hollow diamond at PhongChieu)
    add_edge(root1, 'c_PhongChieu', 'c_Ghe', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='0', start_size='16')

    # 6. LoaiGhe - Ghe (Horizontal straight)
    add_edge(root1, 'c_LoaiGhe', 'c_Ghe', s_mult='1', t_mult='0..*', label='')

    # 7. NhanVien - GiaoDich (Drops to open corridor y=940 and turns right to GiaoDich)
    add_edge(root1, 'c_NhanVien', 'c_GiaoDich', s_mult='1', t_mult='0..*', label='', points=[(600, 940), (830, 940)])

    # 8. Phim - SuatChieu (Drops to open corridor y=420 and turns right to SuatChieu)
    add_edge(root1, 'c_Phim', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='', points=[(540, 420), (930, 420)])

    # 9. LoaiThoiDiem - SuatChieu (Drops down from LoaiThoiDiem to SuatChieu)
    add_edge(root1, 'c_LoaiThoiDiem', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='', points=[(890, 300), (990, 300)])

    # 10. SuatChieu - GheSuatChieu (Composition: solid black diamond at SuatChieu)
    add_edge(root1, 'c_SuatChieu', 'c_GheSuatChieu', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 11. PhongChieu - SuatChieu (Routed via corridor at y=400)
    add_edge(root1, 'c_PhongChieu', 'c_SuatChieu', s_mult='1', t_mult='0..*', label='', points=[(1500, 400), (1070, 400)])

    # 12. Ghe - GheSuatChieu (Routed via corridor at y=380)
    add_edge(root1, 'c_Ghe', 'c_GheSuatChieu', s_mult='1', t_mult='0..*', label='', points=[(1820, 380), (1420, 380)])

    # 13. LoaiPhong - BangGiaVe (Routed via corridor at y=360)
    add_edge(root1, 'c_LoaiPhong', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(1190, 360), (1740, 360)])

    # 14. LoaiGhe - BangGiaVe (Routed via corridor at y=350)
    add_edge(root1, 'c_LoaiGhe', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(2120, 350), (1890, 350)])

    # 15. LoaiThoiDiem - BangGiaVe (Routed via corridor at y=320)
    add_edge(root1, 'c_LoaiThoiDiem', 'c_BangGiaVe', s_mult='1', t_mult='0..*', label='', points=[(950, 320), (1810, 320)])

    # 16. BangGiaVe - GheSuatChieu (Horizontal straight)
    add_edge(root1, 'c_BangGiaVe', 'c_GheSuatChieu', s_mult='0..1', t_mult='0..*', label='')

    # 17. GiaoDich - ThanhToan (Composition: solid black diamond at GiaoDich)
    add_edge(root1, 'c_GiaoDich', 'c_ThanhToan', s_mult='1', t_mult='0..1', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 18. GiaoDich - Ve (Composition: solid black diamond at GiaoDich)
    add_edge(root1, 'c_GiaoDich', 'c_Ve', s_mult='1', t_mult='1..*', label='', start_arrow='diamondThin', start_fill='1', start_size='16')

    # 19. GheSuatChieu - Ve (Vertical routed via corridor y=750)
    add_edge(root1, 'c_GheSuatChieu', 'c_Ve', s_mult='1', t_mult='0..1', label='', points=[(1350, 750), (1300, 750)])

    # 20. GiaoDich - GheSuatChieu (Routed via corridor y=800)
    add_edge(root1, 'c_GiaoDich', 'c_GheSuatChieu', s_mult='0..1', t_mult='0..*', label='', points=[(950, 800), (1450, 800)])

    # 21. BangGiaVe - Ve (Routed via corridor y=880)
    add_edge(root1, 'c_BangGiaVe', 'c_Ve', s_mult='1', t_mult='0..*', label='', points=[(1810, 880), (1400, 880)])

    tree = ET.ElementTree(mxfile)
    ET.indent(tree, space='  ', level=0)
    return tree

if __name__ == '__main__':
    tree = build_drawio()
    out_path = r'docs/SoDoLop_RapChieuPhim.drawio'
    tree.write(out_path, encoding='utf-8', xml_declaration=True)
    print(f'Successfully wrote {out_path}')
