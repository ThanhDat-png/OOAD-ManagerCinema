# Báo cáo kiểm tra mô hình lớp, lược đồ cơ sở dữ liệu và mã nguồn hệ thống

Tài liệu này ghi nhận kết quả kiểm tra danh sách 17 lớp nghiệp vụ dữ liệu / thực thể, lược đồ cơ sở dữ liệu, các lỗi trong tài liệu báo cáo và kết quả đối chiếu chéo với toàn bộ mã nguồn Java của dự án.

## 1. Kết quả kiểm tra danh sách 17 lớp nghiệp vụ dự kiến

### 1.1. Đánh giá tính hợp lệ
Danh sách 17 lớp nghiệp vụ đề xuất là hoàn toàn hợp lệ. Cả 17 lớp đều đại diện cho các thực thể thông tin cần thiết để vận hành đầy đủ 9 ca sử dụng của hệ thống quản lý rạp chiếu phim và được lưu trữ bền vững trong cơ sở dữ liệu.

Theo quy định của đồ án môn học, trong Sơ đồ lớp, sinh viên chỉ cần xây dựng các lớp thực thể dữ liệu tương ứng với các thực thể được lưu trữ trong cơ sở dữ liệu quan hệ, không cần xây dựng các lớp điều khiển Controller hay các lớp giao diện Boundary. Tên gọi tiếng Việt không dấu thống nhất với các mô hình phân tích và cơ sở dữ liệu. Sơ đồ lớp trong tệp `docs/SoDoLop_RapChieuPhim.drawio` gồm duy nhất một trang mang tên SoDoLop thể hiện 17 thực thể dữ liệu này.

### 1.2. Những điểm giữ lại
- Giữ nguyên toàn bộ 17 lớp đối tượng: TaiKhoan, VaiTro, NhanVien, Phim, TheLoai, SuatChieu, PhongChieu, LoaiPhong, Ghe, LoaiGhe, GheSuatChieu, LoaiThoiDiem, BangGiaVe, GiaoDich, ThanhToan, Ve, SanPham.
- Giữ nguyên phạm vi trách nhiệm nghiệp vụ cốt lõi của từng lớp đối tượng theo đúng 9 ca sử dụng.
- Giữ nguyên quyết định thiết kế không tạo thêm lớp PhimTheLoai trong sơ đồ lớp thực thể, vì đây là mối liên kết nhiều nhiều thuần túy không có thuộc tính nghiệp vụ phát sinh riêng.
- Giữ nguyên quyết định không tạo bảng BaoCao độc lập trong cơ sở dữ liệu, vì số liệu thống kê được tổng hợp động từ các giao dịch, vé và tồn kho.

### 1.3. Những điểm sửa đổi và chuẩn hóa
- Xóa dòng số 12 bị sao chép lặp lại hai lần cho lớp LoaiThoiDiem.
- Sửa lại căn cứ đối chiếu: Bản dự thảo trước đây trích dẫn tên các bảng cơ sở dữ liệu viết hoa và các mã quy tắc nội bộ để chứng minh cho sự ra đời của lớp. Theo phương pháp luận phân tích thiết kế hướng đối tượng, sơ đồ lớp thực thể được xây dựng trước từ ca sử dụng và các thông điệp trong sơ đồ tuần tự; sau đó lược đồ cơ sở dữ liệu mới được ánh xạ từ sơ đồ lớp. Do đó, căn cứ của các lớp đã được sửa lại thành các ca sử dụng từ UC01 đến UC09 và các sơ đồ tuần tự từ 3.4.1 đến 3.4.9.
- Bổ sung phân loại lớp theo đúng nội dung bài giảng sơ đồ lớp, bao gồm bốn nhóm: Lớp thực thể chính, Lớp giao dịch, Lớp kết hợp và Lớp danh mục.
- Bổ sung thuộc tính dẫn xuất sức chứa cho lớp PhongChieu và ba thuộc tính lưu vết nhãn ghế tại thời điểm phát hành cho lớp Ve.
- Bỏ các thuộc tính khóa ngoại mang tính kỹ thuật cơ sở dữ liệu ra khỏi danh sách thuộc tính của lớp khi giữa các lớp đã có đường nối quan hệ thể hiện liên kết.

---

## 2. Kết quả kiểm tra lược đồ cơ sở dữ liệu và tài liệu đặc tả

### 2.1. Đánh giá lược đồ cơ sở dữ liệu
Lược đồ cơ sở dữ liệu của hệ thống bao gồm 18 bảng:
- 17 bảng tương ứng với 17 lớp nghiệp vụ đã xác định.
- 1 bảng trung gian PHIM_THELOAI được sinh ra từ mối quan hệ nhiều nhiều giữa Phim và TheLoai theo đúng quy tắc chuyển đổi từ mô hình đối tượng sang mô hình quan hệ.

Các điểm cần hiệu chỉnh trên hình vẽ lược đồ:
- Bảng VE trên hình vẽ hiện tại chỉ hiển thị 9 cột, thiếu 3 cột lưu vết ghế gồm HangGheLucPhatHanh, SoGheLucPhatHanh và TenLoaiGheLucPhatHanh. Ba cột này đã có trong mã nguồn Ve.java và tệp cập nhật cơ sở dữ liệu 008_bao_toan_thong_tin_ghe.sql nhằm bảo toàn lịch sử in vé khi phòng chiếu thay đổi cách bố trí ghế.
- Đường nối giữa GHE_SUATCHIEU và VE có nhãn FK ghép RB03. Mã RB03 là mã quy tắc nội bộ chưa được định nghĩa trong văn bản báo cáo, cần chuyển thành nhãn diễn giải rõ ràng là khóa ngoại ghép gồm mã suất, mã ghế và mã giao dịch.
- Bảng SANPHAM đứng độc lập, không có khóa ngoại nối với GIAODICH hoặc VE. Thiết kế này là đúng với phạm vi ca sử dụng UC06.2 và UC09.4 hiện tại, vì hệ thống chỉ quản lý danh mục giá và theo dõi số lượng tồn kho sản phẩm ăn uống, chưa thực hiện bán gộp sản phẩm ăn uống trong cùng một giao dịch vé xem phim.

### 2.2. Năm lỗi trong tài liệu văn bản báo cáo
Kiểm tra các phần văn bản từ mục 3.5 đến 4.3 trong tệp đặc tả phát hiện 5 lỗi:
- Lỗi 1: Tiêu đề Class diagram ở dòng 1869 không có số thứ tự mục và nội dung bên dưới bị bỏ trống hoàn toàn.
- Lỗi 2: Đánh sai số thứ tự mục ở dòng 1939: Tiêu đề ghi là 4.3. Bảng mô tả chi tiết, trong khi toàn bộ phần cơ sở dữ liệu đang nằm ở mục 3.5 của Chương 3. Số thứ tự đúng phải là 3.5.3 nếu giữ nguyên cấu trúc, hoặc 3.6.3 nếu đưa Sơ đồ lớp vào mục 3.5.
- Lỗi 3: Bảng NHANVIEN ở dòng 2005 bị lỗi cú pháp gõ thiếu dấu đóng ngoặc đơn tại thuộc tính HoTen: ghi là NVARCHAR(100.
- Lỗi 4: Tiêu đề ghi là bảng THANHTOAN ở dòng 2058 nhưng nội dung bảng lại sao chép toàn bộ 6 cột của bảng GHE gồm MaGhe, MaPhong, MaLoaiGhe, HangGhe, SoGhe, TrangThai. Lỗi này làm mất sạch 5 thuộc tính thực tế của bảng THANHTOAN gồm MaThanhToan, MaGiaoDich, PhuongThuc, SoTien, ThoiGianThanhToan, đồng thời ghi chú sai MaPhong là khóa chính của ghế.
- Lỗi 5: Bảng GHE và toàn bộ 12 bảng tiếp theo từ dòng 2091 đến 2145 gồm GHE_SUATCHIEU, VE, BANGGIAVE, LOAIGHE, SUATCHIEU, LOAITHOIDIEM, PHONGCHIEU, LOAIPHONG, PHIM, PHIM_THELOAI, THELOAI, SANPHAM bị bỏ trống hoàn toàn, không có thông tin thuộc tính nào.

---

## 3. Bảng mô tả chi tiết 18 bảng cơ sở dữ liệu chuẩn hóa

Dưới đây là từ điển dữ liệu hoàn chỉnh của 18 bảng cơ sở dữ liệu đã được đối chiếu với các tệp lệnh SQL trong thư mục Database và mã nguồn Java, sẵn sàng đưa vào báo cáo.

### 3.1. Bảng VAITRO
Lưu trữ danh sách các vai trò người dùng trong hệ thống để phục vụ kiểm soát quyền truy cập.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaVaiTro | INT | Khóa chính | Không rỗng | Mã định danh duy nhất của vai trò |
| 2 | TenVaiTro | NVARCHAR(30) | Khóa duy nhất | Không rỗng, duy nhất | Tên vai trò người dùng gồm Admin hoặc Nhân viên |

### 3.2. Bảng NHANVIEN
Lưu trữ hồ sơ cá nhân và thông tin liên lạc của nhân viên làm việc tại rạp.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaNV | VARCHAR(10) | Khóa chính | Không rỗng | Mã định danh duy nhất của nhân viên |
| 2 | HoTen | NVARCHAR(100) | | Không rỗng | Họ và tên đầy đủ của nhân viên |
| 3 | NgaySinh | DATE | | Có thể rỗng | Ngày tháng năm sinh của nhân viên |
| 4 | SoDienThoai | VARCHAR(15) | | Có thể rỗng | Số điện thoại liên lạc của nhân viên |
| 5 | Email | VARCHAR(100) | | Có thể rỗng | Địa chỉ thư điện tử của nhân viên |
| 6 | DiaChi | NVARCHAR(200) | | Có thể rỗng | Địa chỉ nơi ở của nhân viên |
| 7 | ChucVu | NVARCHAR(50) | | Có thể rỗng | Vị trí công tác của nhân viên tại rạp |

### 3.3. Bảng TAIKHOAN
Lưu trữ thông tin tài khoản đăng nhập của nhân viên và liên kết phân quyền.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaTK | INT | Khóa chính | Không rỗng | Mã định danh duy nhất của tài khoản |
| 2 | MaVaiTro | INT | Khóa ngoại | Tham chiếu VAITRO | Xác định vai trò của tài khoản |
| 3 | MaNV | VARCHAR(10) | Khóa ngoại | Duy nhất, tham chiếu NHANVIEN | Nhân viên sở hữu tài khoản, mỗi người có tối đa một tài khoản |
| 4 | TenDangNhap | VARCHAR(50) | Khóa duy nhất | Không rỗng, duy nhất | Tên đăng nhập vào hệ thống |
| 5 | MatKhauHash | VARCHAR(255) | | Không rỗng | Chuỗi mật khẩu đã được băm an toàn |
| 6 | TrangThai | NVARCHAR(20) | | Mặc định Đang hoạt động | Trạng thái gồm Đang hoạt động hoặc Ngưng hoạt động |

### 3.4. Bảng THELOAI
Lưu danh mục thể loại phim dùng để phân loại và hỗ trợ tìm kiếm.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaTheLoai | INT | Khóa chính | Không rỗng | Mã định danh thể loại phim |
| 2 | TenTheLoai | NVARCHAR(50) | Khóa duy nhất | Không rỗng, duy nhất | Tên gọi thể loại như Hành động, Hoạt hình, Tâm lý |

### 3.5. Bảng PHIM
Lưu thông tin hồ sơ và trạng thái khai thác của các bộ phim chiếu tại rạp.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaPhim | VARCHAR(10) | Khóa chính | Không rỗng | Mã định danh duy nhất của phim |
| 2 | TenPhim | NVARCHAR(200) | | Không rỗng | Tên gọi của bộ phim |
| 3 | ThoiLuong | INT | | Lớn hơn 0 | Thời lượng chiếu phim tính theo phút |
| 4 | DaoDien | NVARCHAR(100) | | Không rỗng | Tên đạo diễn của bộ phim |
| 5 | NgayKhoiChieu | DATE | | Không rỗng | Ngày bắt đầu khởi chiếu phim tại rạp |
| 6 | GioiHanTuoi | INT | | Không âm | Độ tuổi tối thiểu được phép xem phim |
| 7 | Poster | VARCHAR(255) | | Không rỗng | Đường dẫn tệp ảnh áp phích của phim |
| 8 | TrangThai | NVARCHAR(20) | | Không rỗng | Trạng thái chiếu gồm Sắp chiếu, Đang chiếu hoặc Ngừng chiếu |
| 9 | DinhDangChieu | NVARCHAR(20) | | Không rỗng | Định dạng chiếu của phim gồm 2D hoặc 3D |

### 3.6. Bảng PHIM_THELOAI
Bảng trung gian thể hiện quan hệ nhiều nhiều giữa phim và thể loại.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaPhim | VARCHAR(10) | Khóa chính ghép, Khóa ngoại | Tham chiếu PHIM, không xóa dòng cha | Mã phim tham chiếu tới bảng PHIM |
| 2 | MaTheLoai | INT | Khóa chính ghép, Khóa ngoại | Tham chiếu THELOAI, không xóa dòng cha | Mã thể loại tham chiếu tới bảng THELOAI |

### 3.7. Bảng LOAIPHONG
Lưu danh mục loại phòng chiếu và chuẩn định dạng kỹ thuật hỗ trợ.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaLoaiPhong | INT | Khóa chính | Không rỗng | Mã định danh loại phòng chiếu |
| 2 | TenLoaiPhong | NVARCHAR(50) | Khóa duy nhất | Không rỗng, duy nhất | Tên loại phòng như Phòng 2D tiêu chuẩn, Phòng 3D |
| 3 | DinhDangChieu | NVARCHAR(20) | | Không rỗng | Định dạng hỗ trợ gồm 2D hoặc 3D |

### 3.8. Bảng PHONGCHIEU
Lưu thông tin phòng chiếu phim và thời gian dọn phòng sau mỗi suất.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaPhong | VARCHAR(10) | Khóa chính | Không rỗng | Mã định danh phòng chiếu như DEMO01, P01 |
| 2 | MaLoaiPhong | INT | Khóa ngoại | Tham chiếu LOAIPHONG | Xác định loại phòng và định dạng chiếu |
| 3 | TenPhong | NVARCHAR(50) | Khóa duy nhất | Không rỗng, duy nhất | Tên phòng chiếu hiển thị trên giao diện |
| 4 | ThoiGianDonPhong | INT | | Không âm | Thời gian dọn dẹp vệ sinh tính theo phút |
| 5 | TrangThai | NVARCHAR(30) | | Không rỗng | Trạng thái hoạt động gồm Đang hoạt động, Tạm ngừng hoạt động hoặc Bảo trì |

### 3.9. Bảng LOAIGHE
Lưu danh mục các loại ghế trong phòng chiếu.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaLoaiGhe | INT | Khóa chính | Không rỗng, tự tăng | Mã định danh loại ghế |
| 2 | TenLoaiGhe | NVARCHAR(50) | Khóa duy nhất | Không rỗng, duy nhất | Tên loại ghế gồm Ghế thường, Ghế VIP, Ghế đôi |

### 3.10. Bảng GHE
Lưu danh sách ghế vật lý gắn liền với từng phòng chiếu.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaGhe | INT | Khóa chính | Không rỗng, tự tăng | Mã định danh duy nhất của ghế |
| 2 | MaPhong | VARCHAR(10) | Khóa ngoại | Tham chiếu PHONGCHIEU | Phòng chiếu chứa ghế |
| 3 | MaLoaiGhe | INT | Khóa ngoại | Tham chiếu LOAIGHE | Loại ghế tương ứng |
| 4 | HangGhe | CHAR(2) | | Không rỗng | Ký hiệu hàng ghế như A, B, C |
| 5 | SoGhe | INT | | Lớn hơn 0 | Số thứ tự ghế trong hàng |
| 6 | TrangThai | NVARCHAR(30) | | Không rỗng | Trạng thái ghế gồm Sẵn sàng sử dụng hoặc Bảo trì |

### 3.11. Bảng LOAITHOIDIEM
Lưu danh mục các mốc thời điểm chiếu trong tuần để định giá vé.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaLoaiThoiDiem | INT | Khóa chính | Không rỗng | Mã định danh loại thời điểm |
| 2 | TenLoaiThoiDiem | NVARCHAR(30) | Khóa duy nhất | Không rỗng, duy nhất | Tên gọi gồm Ngày thường, Cuối tuần hoặc Ngày lễ |

### 3.12. Bảng SUATCHIEU
Lưu thông tin lịch chiếu của các phim tại từng phòng theo khung giờ.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaSuat | INT | Khóa chính | Không rỗng, tự tăng | Mã định danh suất chiếu |
| 2 | MaPhim | VARCHAR(10) | Khóa ngoại | Tham chiếu PHIM, không xóa cha | Phim được chiếu trong suất |
| 3 | MaPhong | VARCHAR(10) | Khóa ngoại | Tham chiếu PHONGCHIEU | Phòng tổ chức suất chiếu |
| 4 | MaLoaiThoiDiem | INT | Khóa ngoại | Tham chiếu LOAITHOIDIEM | Loại thời điểm để xác định giá |
| 5 | ThoiGianBatDau | DATETIME | | Không rỗng | Thời điểm bắt đầu phát phim |
| 6 | ThoiGianTraPhong | DATETIME | | Lớn hơn ThoiGianBatDau | Thời điểm kết thúc chiếu cộng thời gian dọn phòng |
| 7 | TrangThai | NVARCHAR(20) | | Không rỗng | Trạng thái suất gồm Sắp chiếu, Đang chiếu, Đã chiếu hoặc Đã hủy |

### 3.13. Bảng BANGGIAVE
Lưu định mức giá vé áp dụng cho từng tổ hợp ba tiêu chuẩn.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaGia | INT | Khóa chính | Không rỗng, tự tăng | Mã định danh mức giá vé |
| 2 | MaLoaiGhe | INT | Khóa ngoại | Tham chiếu LOAIGHE | Loại ghế áp dụng giá |
| 3 | MaLoaiPhong | INT | Khóa ngoại | Tham chiếu LOAIPHONG | Loại phòng áp dụng giá |
| 4 | MaLoaiThoiDiem | INT | Khóa ngoại | Tham chiếu LOAITHOIDIEM | Loại thời điểm áp dụng giá |
| 5 | GiaVe | DECIMAL(12,0) | | Lớn hơn 0 | Đơn giá vé niêm yết tính bằng đồng |

### 3.14. Bảng GIAODICH
Lưu các giao dịch bán vé do nhân viên thực hiện tại quầy.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaGiaoDich | VARCHAR(20) | Khóa chính | Không rỗng | Mã giao dịch gồm tiền tố GD và chuỗi thời gian |
| 2 | MaNV | VARCHAR(10) | Khóa ngoại | Tham chiếu NHANVIEN, không xóa cha | Nhân viên thực hiện giao dịch |
| 3 | ThoiGianTao | DATETIME | | Không rỗng | Thời điểm bắt đầu giao dịch |
| 4 | TongTien | DECIMAL(12,0) | | Không âm | Tổng số tiền cần thanh toán |
| 5 | TrangThai | NVARCHAR(40) | | Không rỗng | Trạng thái gồm Chờ thanh toán, Đang thanh toán, Đã thanh toán, Đã hủy hoặc Hết hạn |

### 3.15. Bảng THANHTOAN
Lưu thông tin thanh toán thành công của giao dịch.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaThanhToan | INT | Khóa chính | Không rỗng, tự tăng | Mã định danh bản ghi thanh toán |
| 2 | MaGiaoDich | VARCHAR(20) | Khóa ngoại | Duy nhất, tham chiếu GIAODICH | Giao dịch được thanh toán, mỗi giao dịch có tối đa một bản ghi |
| 3 | PhuongThuc | NVARCHAR(20) | | Không rỗng | Hình thức gồm Tiền mặt, Chuyển khoản hoặc Thẻ |
| 4 | SoTien | DECIMAL(12,0) | | Lớn hơn 0 | Số tiền đã thanh toán |
| 5 | ThoiGianThanhToan | DATETIME | | Không rỗng | Thời điểm thanh toán thành công |

### 3.16. Bảng GHE_SUATCHIEU
Lưu trạng thái và kiểm soát đặt chỗ của từng ghế trong từng suất chiếu.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaSuat | INT | Khóa chính ghép, Khóa ngoại | Tham chiếu SUATCHIEU | Suất chiếu chứa ghế |
| 2 | MaGhe | INT | Khóa chính ghép, Khóa ngoại | Tham chiếu GHE | Ghế vật lý trong phòng chiếu |
| 3 | MaGiaoDich | VARCHAR(20) | Khóa ngoại | Có thể rỗng, tham chiếu GIAODICH | Giao dịch đang giữ hoặc đã mua ghế |
| 4 | TrangThai | NVARCHAR(20) | | Không rỗng | Trạng thái gồm Trống, Đang giữ, Đã bán hoặc Không khả dụng |
| 5 | ThoiGianHetHanGiu | DATETIME | | Có thể rỗng | Thời điểm hết hạn khóa giữ chỗ tạm thời |
| 6 | MaGia | INT | Khóa ngoại | Có thể rỗng, tham chiếu BANGGIAVE | Mức giá vé áp dụng cho ghế |
| 7 | GiaVeChot | DECIMAL(12,0) | | Có thể rỗng, lớn hơn 0 | Đơn giá vé chốt tại thời điểm giữ hoặc bán |

### 3.17. Bảng VE
Lưu thông tin vé xem phim đã phát hành cho khách hàng.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaVe | VARCHAR(20) | Khóa chính | Không rỗng | Mã vé phát hành |
| 2 | MaGiaoDich | VARCHAR(20) | Khóa ngoại | Tham chiếu GIAODICH | Giao dịch mua vé |
| 3 | MaSuat | INT | Khóa ngoại | Tham chiếu GHE_SUATCHIEU | Suất chiếu của vé |
| 4 | MaGhe | INT | Khóa ngoại | Tham chiếu GHE_SUATCHIEU | Vị trí ghế của vé |
| 5 | MaGia | INT | Khóa ngoại | Tham chiếu BANGGIAVE | Mức giá vé áp dụng |
| 6 | MaQR | VARCHAR(100) | Khóa duy nhất | Không rỗng, duy nhất | Chuỗi mã phản hồi nhanh dùng để soát vé vào phòng |
| 7 | GiaVe | DECIMAL(12,0) | | Lớn hơn 0 | Giá tiền vé chính thức thu |
| 8 | TrangThai | NVARCHAR(20) | | Không rỗng | Trạng thái vé gồm Chưa sử dụng hoặc Đã sử dụng |
| 9 | ThoiGianPhatHanh | DATETIME | | Không rỗng | Thời điểm in và phát hành vé |
| 10 | HangGheLucPhatHanh | CHAR(2) | | Không rỗng | Ký hiệu hàng ghế tại thời điểm phát hành vé để bảo toàn lịch sử |
| 11 | SoGheLucPhatHanh | INT | | Không rỗng | Số thứ tự ghế tại thời điểm phát hành vé để bảo toàn lịch sử |
| 12 | TenLoaiGheLucPhatHanh | NVARCHAR(50) | | Không rỗng | Tên loại ghế tại thời điểm phát hành vé để bảo toàn lịch sử |

### 3.18. Bảng SANPHAM
Lưu danh mục sản phẩm đồ ăn thức uống và quản lý số lượng tồn kho.

| STT | Tên thuộc tính | Kiểu dữ liệu | Khóa | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- | --- |
| 1 | MaSP | VARCHAR(10) | Khóa chính | Không rỗng | Mã định danh sản phẩm ăn uống |
| 2 | TenSP | NVARCHAR(100) | | Không rỗng | Tên gọi sản phẩm |
| 3 | MoTa | NVARCHAR(255) | | Có thể rỗng | Mô tả quy cách, hương vị, thể tích |
| 4 | DonViTinh | NVARCHAR(20) | | Không rỗng | Đơn vị đóng gói như Ly, Hộp, Chai, Bịch |
| 5 | GiaBan | DECIMAL(12,0) | | Lớn hơn 0 | Giá bán lẻ niêm yết tính bằng đồng |
| 6 | SoLuongTon | INT | | Không âm | Số lượng sản phẩm còn thực tế trong kho |
| 7 | TinhTrangKinhDoanh | NVARCHAR(30) | | Không rỗng | Tình trạng gồm Đang kinh doanh hoặc Ngừng kinh doanh |

---

## 4. Kết quả kiểm tra chéo với mã nguồn Java

Hệ thống mã nguồn của dự án được tổ chức theo kiến trúc ba tầng kết hợp đối tượng truyền tải dữ liệu, phù hợp với nội dung bài giảng thiết kế hệ thống. Dưới đây là bảng đối chiếu chi tiết giữa tài liệu đặc tả và các lớp mã nguồn thực tế.

| Ca sử dụng | Sơ đồ tuần tự | Bảng CSDL | Tầng Giao diện Swing | Tầng Nghiệp vụ BUS | Tầng Truy xuất DAO | Tầng Mô hình DTO |
| --- | --- | --- | --- | --- | --- | --- |
| UC01 Đăng nhập | 3.4.1 | VAITRO, TAIKHOAN, NHANVIEN | DangNhapWindow | DangNhapBUS, PasswordHasher, PhienDangNhap | TaiKhoanDAO, NhanVienDAO | TaiKhoan, VaiTro, NhanVien |
| UC02 Quản lý nhân viên | 3.4.2 | NHANVIEN, TAIKHOAN, VAITRO | NhanVienPanel | NhanVienBUS | NhanVienDAO, TaiKhoanDAO | NhanVien, TaiKhoan, VaiTro |
| UC03 Quản lý phim | 3.4.3 | PHIM, THELOAI, PHIM_THELOAI | PhimPanel | PhimBUS, PosterStore | PhimDAO, TheLoaiDAO | Phim, TheLoai |
| UC04 Quản lý suất chiếu | 3.4.4 | SUATCHIEU, PHIM, PHONGCHIEU, LOAIPHONG, LOAITHOIDIEM | LichChieuPanel | LichChieuBUS | SuatChieuDAO, PhimDAO, PhongChieuDAO, LoaiThoiDiemDAO | SuatChieu, PhongChieu, LoaiThoiDiem |
| UC05 Quản lý phòng và ghế | 3.4.5 | PHONGCHIEU, LOAIPHONG, GHE, LOAIGHE | PhongChieuPanel | PhongChieuBUS | PhongChieuDAO, GheDAO | PhongChieu, LoaiPhong, Ghe, LoaiGhe |
| UC06 Quản lý giá vé và F&B | 3.4.6 | BANGGIAVE, LOAIGHE, LOAIPHONG, LOAITHOIDIEM, SANPHAM | GiaVeFnBPanel | GiaVeBUS, SanPhamBUS, MoneyRules | BangGiaVeDAO, SanPhamDAO, LoaiThoiDiemDAO | BangGiaVe, SanPham |
| UC07 Soát vé vào phòng | 3.4.7 | VE, GHE_SUATCHIEU, SUATCHIEU | KiemTraVePanel | VeBUS, DocMaQR, ThoiGianVaoPhong | VeDAO, SuatChieuDAO | Ve, SuatChieu |
| UC08 Bán vé tại quầy | 3.4.8 | GIAODICH, THANHTOAN, GHE_SUATCHIEU, VE, BANGGIAVE | BanVePanel | BanVeBUS, InVe | BanVeDAO, GiuGheDAO, GiaoDichDAO, VeDAO | GiaoDich, ThanhToan, GheSuatChieu, Ve |
| UC09 Báo cáo doanh thu | 3.4.9 | GIAODICH, THANHTOAN, VE, SANPHAM | BaoCaoPanel | BaoCaoBUS, KhoangThongKe | BaoCaoDAO | Các cấu trúc kết quả truy vấn gồm TongHop, TheoPhim, TheoNhanVien |

Toàn bộ các quy tắc nghiệp vụ trong tài liệu đặc tả đều được cài đặt và kiểm tra tự động bằng bộ thử nghiệm đơn vị trong thư mục kiểm thử của dự án.
