# Danh sách lớp nghiệp vụ dự kiến

**Tổng cộng 17 lớp nghiệp vụ.**

Căn cứ S2_Nhom08_Lan3.docx, mục 3.3, 3.4 và 3.5.2; đối chiếu thêm hai trang của LuocDo_CSDL_RapChieuPhim (2).drawio. Đây là đề xuất thiết kế trước triển khai. Tên lớp dùng tiếng Việt không dấu để thống nhất với thuật ngữ báo cáo.

| STT | Tên lớp dự kiến | Mô tả trách nhiệm | Căn cứ đối chiếu |
| --- | --- | --- | --- |
| 1 | TaiKhoan | Lưu tên đăng nhập, mật khẩu đã băm, trạng thái tài khoản và liên kết với nhân viên, vai trò. | UC01; TAIKHOAN |
| 2 | VaiTro | Biểu diễn vai trò Admin hoặc Nhân viên để xác định quyền truy cập. | UC01 và tác nhân UC02–UC09; VAITRO |
| 3 | NhanVien | Lưu hồ sơ nhân viên và xác định người thực hiện giao dịch bán vé. | UC02, UC08, UC09.3; NHANVIEN |
| 4 | Phim | Lưu thông tin phim, thời lượng, định dạng, poster, giới hạn tuổi và trạng thái chiếu. | UC03, UC04; PHIM |
| 5 | TheLoai | Biểu diễn danh mục thể loại dùng để phân loại và tra cứu phim. | UC03.2.3, UC03.4; THELOAI |
| 6 | SuatChieu | Biểu diễn lịch chiếu của phim tại một phòng, thời gian bắt đầu, trả phòng và trạng thái. | UC04, UC08.1; SUATCHIEU |
| 7 | PhongChieu | Lưu thông tin phòng, thời gian dọn phòng và trạng thái hoạt động; sức chứa được tính từ danh sách ghế. | UC05, UC04; PHONGCHIEU |
| 8 | LoaiPhong | Biểu diễn loại phòng và định dạng chiếu được hỗ trợ; làm căn cứ kiểm tra lịch chiếu và áp giá vé. | UC04, UC06; LOAIPHONG |
| 9 | Ghe | Biểu diễn vị trí ghế trong phòng, loại ghế và tình trạng sử dụng vật lý. | UC05.1.1–UC05.1.3; GHE |
| 10 | LoaiGhe | Biểu diễn danh mục loại ghế, phục vụ phân loại và xác định giá vé. | UC05.1.3, UC06.1; LOAIGHE |
| 11 | GheSuatChieu | Biểu diễn ghế trong từng suất chiếu, trạng thái trống/giữ/bán, hạn giữ, giao dịch giữ và giá vé đã chốt. | UC08.2–UC08.5; GHE_SUATCHIEU; RB02–RB04 |
| 12 | LoaiThoiDiem | Biểu diễn loại ngày thường, cuối tuần hoặc ngày lễ dùng để xác định giá vé của suất chiếu. | UC04, UC06.1; LOAITHOIDIEM; RB06 |
| 13 | BangGiaVe | Lưu mức giá áp dụng cho tổ hợp loại ghế, loại phòng và loại thời điểm. | UC06.1.1–UC06.1.2, UC08.3; BANGGIAVE |
| 14 | GiaoDich | Biểu diễn lần bán vé, nhân viên thực hiện, thời gian, tổng tiền và trạng thái xử lý. | UC08.2–UC08.6, UC09.1–UC09.3; GIAODICH |
| 15 | ThanhToan | Lưu phương thức, số tiền và thời điểm thanh toán thành công của một giao dịch. | UC08.4, UC08.6, UC09; THANHTOAN; RB04 |
| 16 | Ve | Biểu diễn vé phát hành cho một ghế trong một suất, giao dịch liên quan, mã QR, giá đã chốt và trạng thái sử dụng. | UC07, UC08.5, UC09; VE |
| 17 | SanPham | Lưu tên sản phẩm F&B, đơn vị tính, giá bán, số lượng tồn kho và tình trạng kinh doanh. | UC06.2.1–UC06.2.4, UC09.4; SANPHAM |

## Các quyết định và điểm cần rà soát

- PHIM_THELOAI được biểu diễn bằng quan hệ nhiều–nhiều giữa Phim và TheLoai. Dự kiến Phim chứa danh sách thể loại. Bảng liên kết vẫn tồn tại trong CSDL; hiện chưa có thuộc tính nghiệp vụ riêng để cần một lớp PhimTheLoai.
- Ghe và GheSuatChieu có trách nhiệm riêng. Một lớp biểu diễn ghế vật lý, lớp còn lại lưu tình trạng ghế theo từng suất, thời hạn giữ và giá đã chốt.
- GiaoDich và ThanhToan được tách theo báo cáo. Một giao dịch có thể đang chờ hoặc bị hủy; ThanhToan lưu kết quả đã xác nhận thành công, tối đa một bản ghi cho mỗi giao dịch.
- Kết quả UC09 được tính từ giao dịch, thanh toán, vé và sản phẩm. Lớp chứa kết quả thống kê sẽ được xác định khi triển khai UC09 và thống kê riêng nếu phát sinh. Báo cáo hiện chưa yêu cầu một thực thể BaoCao lưu độc lập.
- UC09.4 chỉ thống kê vé và tồn kho F&B. Phạm vi hiện tại chưa có luồng bán sản phẩm F&B, doanh thu F&B, hồ sơ khách hàng hoặc hoàn tiền vé.
- Mục 3.5.3 của báo cáo còn thiếu mô tả nhiều bảng. Phần mang tiêu đề THANHTOAN đang ghi thuộc tính của GHE. Khi xác định thuộc tính lớp, cần đối chiếu lược đồ và quy tắc cùng đặc tả ca sử dụng, rồi chỉnh phần mô tả báo cáo.
- 17 lớp là các lớp đối tượng nghiệp vụ dự kiến đưa vào phần danh sách lớp. Các lớp Swing, DAO, Controller/BUS và lớp kỹ thuật sẽ được thống kê riêng sau triển khai.
- Bước tiếp theo là xác định thuộc tính và quan hệ của TaiKhoan, VaiTro, NhanVien cho UC01, đối chiếu luồng đăng nhập rồi triển khai. Class diagram cuối cùng được dựng từ code đã kiểm tra.
