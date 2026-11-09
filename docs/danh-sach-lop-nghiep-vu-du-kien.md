# Danh sách và đặc tả các lớp thực thể dữ liệu

Theo quy định của đồ án môn học Phân tích và Thiết kế Hướng đối tượng, trong Sơ đồ lớp, sinh viên chỉ cần xây dựng các lớp thực thể dữ liệu tương ứng với các thực thể được lưu trữ trong cơ sở dữ liệu quan hệ, không cần xây dựng các lớp xử lý điều khiển Controller hay các lớp giao diện Boundary.

Sơ đồ lớp của hệ thống quản lý rạp chiếu phim tập trung duy nhất vào 17 lớp thực thể dữ liệu được trích xuất từ 9 ca sử dụng và các sơ đồ tuần tự trong tài liệu đặc tả, tương ứng với các bảng thực thể được lưu trữ trong cơ sở dữ liệu. Dưới đây là bảng phân loại lớp, bảng đặc tả chi tiết thuộc tính và phương thức nghiệp vụ nội tại của thực thể, cùng bảng các mối quan hệ cấu trúc giữa 17 lớp thực thể này.

## 1. Bảng phân loại 17 lớp nghiệp vụ

| STT | Tên lớp | Phân loại lớp | Trách nhiệm nghiệp vụ | Căn cứ ca sử dụng và tuần tự |
| --- | --- | --- | --- | --- |
| 1 | TaiKhoan | Lớp thực thể chính | Lưu tên đăng nhập, mật khẩu đã băm, trạng thái hoạt động; liên kết với nhân viên và vai trò để xác thực quyền truy cập | UC01, Sơ đồ 3.4.1 |
| 2 | VaiTro | Lớp danh mục | Biểu diễn hai vai trò Admin và Nhân viên để phân quyền chức năng | UC01, Tác nhân hệ thống, Sơ đồ 3.4.1 |
| 3 | NhanVien | Lớp thực thể chính | Lưu hồ sơ thông tin cá nhân của nhân viên và xác định người phụ trách các giao dịch bán vé | UC02, UC08, Sơ đồ 3.4.2, 3.4.8 |
| 4 | Phim | Lớp thực thể chính | Lưu thông tin phim, thời lượng, định dạng, ảnh áp phích, đạo diễn, ngày khởi chiếu, giới hạn độ tuổi và trạng thái chiếu | UC03, UC04, Sơ đồ 3.4.3, 3.4.4 |
| 5 | TheLoai | Lớp danh mục | Biểu diễn danh mục thể loại phim phục vụ phân loại và tra cứu | UC03, Sơ đồ 3.4.3 |
| 6 | SuatChieu | Lớp thực thể chính | Biểu diễn lịch chiếu của một phim tại một phòng, thời gian bắt đầu, thời gian dọn phòng xong để trả phòng và trạng thái suất | UC04, UC08, Sơ đồ 3.4.4, 3.4.8 |
| 7 | PhongChieu | Lớp thực thể chính | Lưu thông tin phòng chiếu, định dạng phòng, thời gian dọn phòng và trạng thái hoạt động; sức chứa được tính từ danh sách ghế | UC04, UC05, Sơ đồ 3.4.4, 3.4.5 |
| 8 | LoaiPhong | Lớp danh mục | Biểu diễn danh mục phân loại phòng và định dạng chiếu hỗ trợ gồm 2D hoặc 3D | UC04, UC05, UC06, Sơ đồ 3.4.4, 3.4.5 |
| 9 | Ghe | Lớp thực thể thành phần | Biểu diễn vị trí ghế vật lý trong phòng chiếu, thuộc phòng nào, loại ghế nào và trạng thái sẵn sàng hay bảo trì | UC05, Sơ đồ 3.4.5 |
| 10 | LoaiGhe | Lớp danh mục | Biểu diễn danh mục loại ghế gồm ghế thường, ghế VIP, ghế đôi để phân loại vị trí và tính giá vé | UC05, UC06, Sơ đồ 3.4.5, 3.4.6 |
| 11 | GheSuatChieu | Lớp kết hợp | Biểu diễn trạng thái của ghế trong từng suất chiếu cụ thể gồm Trống, Đang giữ, Đã bán hoặc Không khả dụng, thời hạn giữ tạm và giá vé đã chốt | UC08, Sơ đồ 3.4.8 |
| 12 | LoaiThoiDiem | Lớp danh mục | Biểu diễn danh mục thời điểm chiếu gồm ngày thường, cuối tuần, ngày lễ dùng để áp bảng giá vé | UC04, UC06, Sơ đồ 3.4.4, 3.4.6 |
| 13 | BangGiaVe | Lớp tham số bảng giá | Lưu mức giá vé quy định cho từng tổ hợp ba yếu tố gồm loại ghế, loại phòng và loại thời điểm chiếu | UC06.1, UC08, Sơ đồ 3.4.6, 3.4.8 |
| 14 | GiaoDich | Lớp giao dịch chính | Biểu diễn một lần bán vé tại quầy do nhân viên thực hiện, thời gian tạo, tổng số tiền và trạng thái xử lý | UC08, UC09, Sơ đồ 3.4.8, 3.4.9 |
| 15 | ThanhToan | Lớp giao dịch thành phần | Lưu thông tin kết quả thanh toán thành công của một giao dịch gồm phương thức thanh toán, số tiền và thời điểm hoàn tất | UC08, UC09, Sơ đồ 3.4.8, 3.4.9 |
| 16 | Ve | Lớp giao dịch kết quả | Biểu diễn vé phát hành cho một ghế trong một suất chiếu, mã phản hồi nhanh, giá vé đã chốt, trạng thái vé và dữ liệu hàng số ghế lưu vết | UC07, UC08, Sơ đồ 3.4.7, 3.4.8 |
| 17 | SanPham | Lớp thực thể danh mục và kho | Lưu thông tin sản phẩm ăn uống, đơn vị tính, giá bán niêm yết, số lượng tồn kho và tình trạng kinh doanh | UC06.2, UC09.4, Sơ đồ 3.4.6, 3.4.9 |

## 2. Đặc tả chi tiết thuộc tính và phương thức của 17 lớp

Theo quy định thiết kế sơ đồ lớp thực thể dữ liệu, các thuộc tính khóa ngoại dùng để liên kết kỹ thuật không được lặp lại trong phần thuộc tính của lớp nếu giữa các lớp đó đã có đường kết hợp thể hiện mối quan hệ. Dưới đây là đặc tả chi tiết của từng lớp.

### 2.1. Lớp TaiKhoan
- Thuộc tính:
  - `- maTK: int`: Mã định danh tài khoản.
  - `- tenDangNhap: String`: Tên đăng nhập duy nhất của tài khoản.
  - `- matKhauHash: String`: Chuỗi mật khẩu đã băm bằng thuật toán an toàn.
  - `- trangThai: String`: Trạng thái hoạt động của tài khoản gồm Đang hoạt động hoặc Ngưng hoạt động.
- Phương thức nghiệp vụ:
  - `+ dangHoatDong(): boolean`: Kiểm tra tài khoản có ở trạng thái được phép đăng nhập hay không.
  - `+ doiMatKhau(mkMoi: String): void`: Cập nhật mật khẩu mới đã băm cho tài khoản.
  - `+ kiemTraDangNhap(matKhau: String): boolean`: Xác thực mật khẩu nhập vào với mã băm lưu trữ.

### 2.2. Lớp VaiTro
- Thuộc tính:
  - `- maVaiTro: int`: Mã định danh vai trò.
  - `- tenVaiTro: String`: Tên vai trò gồm Admin hoặc Nhân viên.
- Phương thức nghiệp vụ:
  - `+ getTen(): String`: Lấy tên vai trò.
  - `+ fromDatabase(ten: String): VaiTro`: Chuyển đổi tên vai trò từ cơ sở dữ liệu sang đối tượng vai trò.

### 2.3. Lớp NhanVien
- Thuộc tính:
  - `- maNV: String`: Mã nhân viên gồm ký tự NV và ba chữ số.
  - `- hoTen: String`: Họ và tên đầy đủ của nhân viên.
  - `- ngaySinh: LocalDate`: Ngày tháng năm sinh của nhân viên.
  - `- soDienThoai: String`: Số điện thoại liên lạc.
  - `- email: String`: Hòm thư điện tử.
  - `- diaChi: String`: Địa chỉ nơi ở của nhân viên.
  - `- chucVu: String`: Vị trí công tác của nhân viên tại rạp.
- Phương thức nghiệp vụ:
  - `+ capNhatThongTin(hoTen: String, sdt: String, email: String, diaChi: String, chucVu: String): void`: Cập nhật hồ sơ nhân viên.
  - `+ getHoTen(): String`: Lấy họ tên nhân viên phục vụ hiển thị.

### 2.4. Lớp Phim
- Thuộc tính:
  - `- maPhim: String`: Mã phim duy nhất.
  - `- tenPhim: String`: Tên gọi của bộ phim.
  - `- thoiLuong: int`: Thời lượng chiếu phim tính theo phút, phải lớn hơn 0.
  - `- daoDien: String`: Tên đạo diễn thực hiện phim.
  - `- ngayKhoiChieu: LocalDate`: Ngày bắt đầu khởi chiếu phim tại rạp.
  - `- gioiHanTuoi: int`: Độ tuổi tối thiểu của khán giả được phép xem phim.
  - `- poster: String`: Đường dẫn lưu tệp áp phích của phim.
  - `- trangThai: String`: Trạng thái chiếu gồm Sắp chiếu, Đang chiếu hoặc Ngừng chiếu.
  - `- dinhDangChieu: String`: Định dạng của phim gồm 2D hoặc 3D.
- Phương thức nghiệp vụ:
  - `+ ngungChieu(): void`: Chuyển trạng thái phim sang ngừng chiếu khi hết hạn khai thác.
  - `+ dangChieu(): boolean`: Kiểm tra phim có đang trong giai đoạn được xếp lịch chiếu hay không.
  - `+ withTheLoai(theLoai: List<TheLoai>): Phim`: Gán danh sách các thể loại phim cho đối tượng phim.

### 2.5. Lớp TheLoai
- Thuộc tính:
  - `- maTheLoai: int`: Mã định danh thể loại.
  - `- tenTheLoai: String`: Tên thể loại phim như Hành động, Hoạt hình, Tình cảm.
- Phương thức nghiệp vụ:
  - `+ getTen(): String`: Lấy tên thể loại phim.

### 2.6. Lớp SuatChieu
- Thuộc tính:
  - `- maSuat: int`: Mã định danh suất chiếu tự tăng.
  - `- thoiGianBatDau: LocalDateTime`: Thời điểm bắt đầu chiếu phim.
  - `- thoiGianTraPhong: LocalDateTime`: Thời điểm kết thúc chiếu cộng thời gian dọn phòng để hoàn trả phòng.
  - `- trangThai: String`: Trạng thái suất chiếu gồm Sắp chiếu, Đang chiếu, Đã chiếu hoặc Đã hủy.
- Phương thức nghiệp vụ:
  - `+ kiemTraKhungGio(batDau: LocalDateTime, ketThuc: LocalDateTime): boolean`: Kiểm tra khoảng thời gian chiếu có hợp lệ hay không.
  - `+ coTheBanVe(): boolean`: Kiểm tra suất chiếu có đang ở trạng thái cho phép mở bán vé hay không.
  - `+ huySuat(): void`: Hủy bỏ suất chiếu khi có sự cố kỹ thuật hoặc thay đổi lịch.

### 2.7. Lớp PhongChieu
- Thuộc tính:
  - `- maPhong: String`: Mã định danh phòng chiếu.
  - `- tenPhong: String`: Tên phòng chiếu như Phòng 01, Phòng 02.
  - `- thoiGianDonPhong: int`: Số phút cần thiết để dọn dẹp phòng sau mỗi ca chiếu.
  - `- trangThai: String`: Trạng thái phòng gồm Đang hoạt động, Tạm ngừng hoạt động hoặc Bảo trì.
  - `/ sucChua: int`: Thuộc tính dẫn xuất thể hiện sức chứa của phòng, tính bằng tổng số ghế vật lý ở trạng thái sẵn sàng sử dụng.
- Phương thức nghiệp vụ:
  - `+ tinhSucChua(): int`: Tính toán tổng số lượng ghế hoạt động của phòng.
  - `+ dangHoatDong(): boolean`: Kiểm tra phòng có đang sẵn sàng tiếp nhận suất chiếu mới hay không.

### 2.8. Lớp LoaiPhong
- Thuộc tính:
  - `- maLoaiPhong: int`: Mã định danh loại phòng.
  - `- tenLoaiPhong: String`: Tên loại phòng như Phòng 2D tiêu chuẩn, Phòng 3D kỹ thuật số.
  - `- dinhDangChieu: String`: Chuẩn định dạng phòng hỗ trợ gồm 2D hoặc 3D.
- Phương thức nghiệp vụ:
  - `+ getDinhDang(): String`: Lấy chuẩn định dạng hỗ trợ của loại phòng.

### 2.9. Lớp Ghe
- Thuộc tính:
  - `- maGhe: int`: Mã định danh của ghế vật lý trong phòng.
  - `- hangGhe: String`: Ký hiệu chữ cái của hàng ghế như A, B, C.
  - `- soGhe: int`: Số thứ tự của ghế trong hàng như 1, 2, 3.
  - `- trangThai: String`: Trạng thái vật lý của ghế gồm Sẵn sàng sử dụng hoặc Bảo trì.
- Phương thức nghiệp vụ:
  - `+ sanSangSuDung(): boolean`: Kiểm tra ghế có đủ điều kiện vật lý để xếp chỗ cho khách hay không.
  - `+ chuyenBaoTri(): void`: Đánh dấu chuyển ghế sang trạng thái hỏng hóc cần bảo trì.

### 2.10. Lớp LoaiGhe
- Thuộc tính:
  - `- maLoaiGhe: int`: Mã định danh loại ghế.
  - `- tenLoaiGhe: String`: Tên loại ghế gồm Ghế thường, Ghế VIP, Ghế đôi.
- Phương thức nghiệp vụ:
  - `+ getTen(): String`: Lấy tên loại ghế.

### 2.11. Lớp GheSuatChieu
- Thuộc tính:
  - `- trangThai: String`: Trạng thái của vị trí ghế trong suất chiếu gồm Trống, Đang giữ, Đã bán hoặc Không khả dụng.
  - `- thoiGianHetHanGiu: LocalDateTime`: Thời điểm hết hạn khóa giữ chỗ tạm thời khi nhân viên đang thao tác bán.
  - `- giaVeChot: BigDecimal`: Giá tiền vé được chốt tại thời điểm đưa ghế vào giao dịch bán.
- Phương thức nghiệp vụ:
  - `+ kiemTraHetHanGiu(): boolean`: Kiểm tra thời hạn giữ chỗ tạm thời đã trôi qua hay chưa.
  - `+ giuGhe(hanGiu: LocalDateTime): void`: Khóa giữ chỗ tạm thời ghế cho một giao dịch.
  - `+ xacNhanBan(): void`: Chuyển trạng thái ghế sang đã bán sau khi thanh toán thành công.
  - `+ giaiPhongGhe(): void`: Xóa trạng thái giữ để trả ghế về trạng thái trống khi hủy giao dịch hoặc hết giờ giữ.

### 2.12. Lớp LoaiThoiDiem
- Thuộc tính:
  - `- maLoaiThoiDiem: int`: Mã định danh loại thời điểm.
  - `- tenLoaiThoiDiem: String`: Tên loại thời điểm gồm Ngày thường, Cuối tuần hoặc Ngày lễ.
- Phương thức nghiệp vụ:
  - `+ getTen(): String`: Lấy tên loại thời điểm.

### 2.13. Lớp BangGiaVe
- Thuộc tính:
  - `- maGia: int`: Mã định danh của mức giá quy định.
  - `- giaVe: BigDecimal`: Mức tiền vé niêm yết tính bằng đồng Việt Nam, phải lớn hơn 0.
- Phương thức nghiệp vụ:
  - `+ capNhatGia(giaMoi: BigDecimal): void`: Thay đổi đơn giá vé áp dụng cho tổ hợp loại ghế, phòng và thời điểm tương ứng.

### 2.14. Lớp GiaoDich
- Thuộc tính:
  - `- maGiaoDich: String`: Mã định danh giao dịch gồm tiền tố GD và chuỗi số thời gian.
  - `- thoiGianTao: LocalDateTime`: Thời điểm tạo giao dịch tại quầy.
  - `- tongTien: BigDecimal`: Tổng số tiền thanh toán của các vé trong giao dịch.
  - `- trangThai: String`: Trạng thái xử lý gồm Chờ thanh toán, Đang thanh toán, Đã thanh toán, Đã hủy hoặc Hết hạn.
- Phương thức nghiệp vụ:
  - `+ tinhTongTien(): BigDecimal`: Tính tổng tiền từ các vé hoặc ghế đã chọn trong giao dịch.
  - `+ hoanTatGiaoDich(): void`: Đánh dấu giao dịch đã thanh toán thành công.
  - `+ huyGiaoDich(): void`: Hủy bỏ giao dịch khi khách đổi ý hoặc quá thời hạn thanh toán.

### 2.15. Lớp ThanhToan
- Thuộc tính:
  - `- maThanhToan: int`: Mã định danh bản ghi thanh toán.
  - `- phuongThuc: String`: Hình thức thanh toán gồm Tiền mặt, Chuyển khoản hoặc Thẻ.
  - `- soTien: BigDecimal`: Số tiền đã thu của khách hàng, phải đúng bằng tổng tiền giao dịch.
  - `- thoiGianThanhToan: LocalDateTime`: Thời điểm xác nhận tiền vào hệ thống thành công.
- Phương thức nghiệp vụ:
  - `+ xacNhanThanhToan(): boolean`: Kiểm tra tính hợp lệ của số tiền thu và thời điểm thanh toán.

### 2.16. Lớp Ve
- Thuộc tính:
  - `- maVe: String`: Mã định danh vé phát hành.
  - `- maQR: String`: Chuỗi mã hóa phản hồi nhanh in trên vé để quét vào phòng chiếu.
  - `- giaVe: BigDecimal`: Mức giá vé chính thức thu tiền tại thời điểm in vé.
  - `- trangThai: String`: Trạng thái vé gồm Chưa sử dụng hoặc Đã sử dụng.
  - `- thoiGianPhatHanh: LocalDateTime`: Thời điểm in vé trao cho khách hàng.
  - `- hangGheLucPhatHanh: String`: Ký hiệu hàng ghế tại thời điểm phát hành vé để lưu vết lịch sử.
  - `- soGheLucPhatHanh: int`: Số thứ tự ghế tại thời điểm phát hành vé để lưu vết lịch sử.
  - `- tenLoaiGheLucPhatHanh: String`: Tên loại ghế tại thời điểm phát hành vé để lưu vết lịch sử.
- Phương thức nghiệp vụ:
  - `+ kiemTraHopLe(): boolean`: Kiểm tra vé có đúng suất chiếu, đúng phòng và chưa qua cửa soát hay không.
  - `+ danhDauSuDung(): void`: Đổi trạng thái vé sang Đã sử dụng khi khách hàng đi qua cửa vào phòng.

### 2.17. Lớp SanPham
- Thuộc tính:
  - `- maSP: String`: Mã sản phẩm đồ ăn thức uống.
  - `- tenSP: String`: Tên gọi sản phẩm như Bắp rang bơ phô mai, Nước ngọt có ga.
  - `- moTa: String`: Mô tả chi tiết quy cách đóng gói và dung tích sản phẩm.
  - `- donViTinh: String`: Đơn vị tính như Ly, Hộp, Chai, Bịch.
  - `- giaBan: BigDecimal`: Giá bán lẻ niêm yết của sản phẩm, phải lớn hơn 0.
  - `- soLuongTon: int`: Số lượng hàng hóa thực tế còn trong kho, không được nhỏ hơn 0.
  - `- tinhTrangKinhDoanh: String`: Tình trạng bán gồm Đang kinh doanh hoặc Ngừng kinh doanh.
- Phương thức nghiệp vụ:
  - `+ capNhatTonKho(soLuongThayDoi: int): void`: Điều chỉnh tăng hoặc giảm số lượng tồn kho khi nhập hàng hoặc kiểm kê.
  - `+ conHang(): boolean`: Kiểm tra sản phẩm còn tồn kho lớn hơn 0 để sẵn sàng bán hay không.

## 3. Bảng các mối quan hệ và quy tắc chuyển đổi sang cơ sở dữ liệu quan hệ

Bảng dưới đây tổng hợp tất cả các mối quan hệ giữa 17 lớp nghiệp vụ trong sơ đồ lớp, kèm theo bản số hai đầu và quy tắc ánh xạ tương ứng sang 18 bảng cơ sở dữ liệu theo tài liệu thiết kế hướng đối tượng.

| STT | Lớp nguồn | Lớp đích | Loại quan hệ | Bản số nguồn | Bản số đích | Ý nghĩa nghiệp vụ | Quy tắc ánh xạ sang CSDL |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | VaiTro | TaiKhoan | Liên kết một nhiều | 1 | 0..* | Một vai trò được cấp cho nhiều tài khoản người dùng; mỗi tài khoản thuộc đúng một vai trò | Quy tắc số 3: Đưa khóa chính MaVaiTro vào bảng TAIKHOAN làm khóa ngoại |
| 2 | NhanVien | TaiKhoan | Liên kết một một | 1 | 0..1 | Mỗi nhân viên có tối đa một tài khoản đăng nhập; một tài khoản gắn liền với đúng một nhân viên | Quy tắc số 2: Đưa MaNV vào bảng TAIKHOAN làm khóa ngoại có ràng buộc duy nhất |
| 3 | NhanVien | GiaoDich | Liên kết một nhiều | 1 | 0..* | Một nhân viên chịu trách nhiệm thực hiện nhiều giao dịch bán vé; mỗi giao dịch do một nhân viên xử lý | Quy tắc số 3: Đưa MaNV vào bảng GIAODICH làm khóa ngoại |
| 4 | TheLoai | Phim | Liên kết nhiều nhiều | 1..* | 1..* | Một phim thuộc về một hoặc nhiều thể loại; một thể loại có nhiều phim khác nhau | Quy tắc số 4: Tạo bảng trung gian PHIM_THELOAI có khóa chính ghép gồm MaPhim và MaTheLoai |
| 5 | Phim | SuatChieu | Liên kết một nhiều | 1 | 0..* | Một bộ phim có thể được xếp vào nhiều suất chiếu; mỗi suất chiếu phục vụ chiếu đúng một phim | Quy tắc số 3: Đưa MaPhim vào bảng SUATCHIEU làm khóa ngoại |
| 6 | LoaiPhong | PhongChieu | Liên kết một nhiều | 1 | 0..* | Một loại phòng phân loại cho nhiều phòng chiếu; mỗi phòng chiếu thuộc đúng một loại phòng | Quy tắc số 8 và số 3: Đưa MaLoaiPhong vào bảng PHONGCHIEU làm khóa ngoại |
| 7 | PhongChieu | SuatChieu | Liên kết một nhiều | 1 | 0..* | Một phòng chiếu tổ chức nhiều suất chiếu theo các khung giờ khác nhau; mỗi suất diễn ra tại một phòng | Quy tắc số 3: Đưa MaPhong vào bảng SUATCHIEU làm khóa ngoại |
| 8 | PhongChieu | Ghe | Thu nạp một nhiều | 1 | 1..* | Một phòng chiếu chứa nhiều ghế vật lý; ghế thuộc phòng chiếu nhưng nếu phòng tạm ngừng thì ghế vẫn tồn tại | Quy tắc số 3: Đưa MaPhong vào bảng GHE làm khóa ngoại |
| 9 | LoaiGhe | Ghe | Liên kết một nhiều | 1 | 0..* | Một loại ghế phân loại cho nhiều ghế vật lý; mỗi ghế thuộc đúng một loại ghế | Quy tắc số 8 và số 3: Đưa MaLoaiGhe vào bảng GHE làm khóa ngoại |
| 10 | SuatChieu | GheSuatChieu | Hợp thành một nhiều | 1 | 1..* | Suất chiếu bao gồm các trạng thái ghế trong suất; nếu suất chiếu bị hủy thì các ghế suất chiếu bị hủy theo | Quy tắc số 7: Bảng GHE_SUATCHIEU có khóa chính ghép gồm MaSuat và MaGhe |
| 11 | Ghe | GheSuatChieu | Liên kết một nhiều | 1 | 0..* | Một ghế vật lý tham gia vào nhiều suất chiếu khác nhau qua thực thể trạng thái ghế suất chiếu | Quy tắc số 3: Đưa MaGhe vào bảng GHE_SUATCHIEU tham gia khóa chính và khóa ngoại |
| 12 | LoaiThoiDiem | SuatChieu | Liên kết một nhiều | 1 | 0..* | Mỗi suất chiếu được gắn với một loại thời điểm chiếu để làm căn cứ tính giá vé | Quy tắc số 8 và số 3: Đưa MaLoaiThoiDiem vào bảng SUATCHIEU làm khóa ngoại |
| 13 | LoaiGhe | BangGiaVe | Liên kết một nhiều | 1 | 0..* | Loại ghế là một tiêu chí cấu thành nên mức giá vé | Quy tắc số 3: Đưa MaLoaiGhe vào bảng BANGGIAVE làm khóa ngoại |
| 14 | LoaiPhong | BangGiaVe | Liên kết một nhiều | 1 | 0..* | Loại phòng chiếu là một tiêu chí cấu thành nên mức giá vé | Quy tắc số 3: Đưa MaLoaiPhong vào bảng BANGGIAVE làm khóa ngoại |
| 15 | LoaiThoiDiem | BangGiaVe | Liên kết một nhiều | 1 | 0..* | Loại thời điểm ngày thường hay lễ là tiêu chí cấu thành nên mức giá vé | Quy tắc số 3: Đưa MaLoaiThoiDiem vào bảng BANGGIAVE làm khóa ngoại |
| 16 | BangGiaVe | GheSuatChieu | Liên kết không bắt buộc | 0..1 | 0..* | Giá vé chuẩn áp dụng cho ghế khi được nhân viên đưa vào thao tác giữ hoặc bán | Đưa MaGia vào bảng GHE_SUATCHIEU làm khóa ngoại cho phép rỗng khi ghế đang trống |
| 17 | BangGiaVe | Ve | Liên kết một nhiều | 1 | 0..* | Mỗi vé phát hành được căn cứ từ một mức giá chuẩn quy định trong bảng giá | Quy tắc số 3: Đưa MaGia vào bảng VE làm khóa ngoại |
| 18 | GiaoDich | ThanhToan | Hợp thành một một | 1 | 0..1 | Mỗi giao dịch có tối đa một bản ghi xác nhận thanh toán thành công; bản ghi thanh toán không tồn tại độc lập | Quy tắc số 2: Đưa MaGiaoDich vào bảng THANHTOAN làm khóa ngoại duy nhất |
| 19 | GiaoDich | Ve | Hợp thành một nhiều | 1 | 1..* | Giao dịch phát hành ra một hoặc nhiều vé xem phim cho khách hàng | Quy tắc số 3 và số 7: Đưa MaGiaoDich vào bảng VE làm khóa ngoại |
| 20 | GiaoDich | GheSuatChieu | Liên kết không bắt buộc | 0..1 | 0..* | Ghế suất chiếu được giữ hoặc bán trong một giao dịch cụ thể | Đưa MaGiaoDich vào bảng GHE_SUATCHIEU làm khóa ngoại cho phép rỗng khi ghế chưa ai chọn |
| 21 | GheSuatChieu | Ve | Liên kết một một | 1 | 0..1 | Một ghế trong một suất chiếu khi thanh toán thành công phát hành ra đúng một vé; một vé ứng với một ghế suất | Đưa khóa ghép MaSuat, MaGhe, MaGiaoDich vào bảng VE làm khóa ngoại tham chiếu |
| 22 | SanPham | Không liên kết | Thực thể độc lập | 1 | 0 | Quản lý danh mục và tồn kho hàng hóa độc lập, chưa bán tích hợp trong giao dịch vé ở phạm vi hiện tại | Quy tắc số 1: Ánh xạ thành bảng SANPHAM độc lập trong cơ sở dữ liệu |
