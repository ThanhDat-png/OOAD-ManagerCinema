# Đối chiếu S2_Nhom08_Lan3.docx

Mục 3.3 quy định luồng nghiệp vụ. Khi triển khai, kiểm tra thêm sơ đồ tuần tự ở mục 3.4, luồng thay thế và điều kiện kết thúc. Các mục dưới đây mới có package, chưa triển khai nghiệp vụ.

## UC01 · Đăng nhập · cinema.controller.auth

- UC01 — Đăng nhập (Admin, Nhân viên)

## UC02 · Quản lý nhân viên · cinema.controller.staff

- UC02.1.1 — Thêm nhân viên (Admin)
- UC02.1.2 — Sửa nhân viên (Admin)
- UC02.1.3 — Xóa nhân viên (Admin)
- UC02.2 — Xem thông tin chi tiết nhân viên (Admin, Nhân viên)
- UC02.3 — Tìm kiếm nhân viên (Admin)

## UC03 · Quản lý phim · cinema.controller.film

- UC03.1 — Thêm phim (Admin)
- UC03.2.1 — Sửa tên phim (Admin)
- UC03.2.2 — Cập nhật poster phim (Admin)
- UC03.2.3 — Cập nhật thể loại phim (Admin)
- UC03.3 — Ngừng chiếu phim (Admin)
- UC03.4 — Tra cứu phim (Admin, Nhân viên)

## UC04 · Quản lý lịch chiếu · cinema.controller.schedule

- UC04.1 — Tạo suất chiếu (Admin)
- UC04.2.1 — Cập nhật phim chiếu (Admin)
- UC04.2.2 — Thay đổi phòng chiếu (Admin)
- UC04.2.3 — Đổi ngày chiếu (Admin)
- UC04.2.4 — Đổi giờ chiếu (Admin)
- UC04.3 — Kiểm tra lịch trùng (Admin, thông qua thao tác tạo hoặc cập nhật suất chiếu.)
- UC04.4 — Hủy suất chiếu (Admin)

## UC05 · Quản lý phòng chiếu · cinema.controller.room

- UC05.1.1 — Thêm ghế (Admin)
- UC05.1.2 — Cập nhật thông tin ghế (Admin)
- UC05.1.3 — Phân loại ghế (Admin)
- UC05.2 — Thêm phòng chiếu (Admin)
- UC05.3 — Tra cứu phòng chiếu (Admin, Nhân viên)
- UC05.4 — Cập nhật thông tin phòng chiếu (Admin)
- UC05.5 — Cập nhật trạng thái phòng chiếu (Admin)

## UC06 · Quản lý giá vé và F&B · cinema.controller.pricing

- UC06.1.1 — Thiết lập giá vé (Admin)
- UC06.1.2 — Sửa giá vé (Admin)
- UC06.2.1 — Thêm sản phẩm (Admin)
- UC06.2.2 — Cập nhật sản phẩm (Admin)
- UC06.2.3 — Quản lý giá bán (Admin)
- UC06.2.4 — Kiểm tra tồn kho (Admin)

## UC07 · Kiểm tra, xác nhận vé · cinema.controller.admission

- UC07.1 — Tra cứu vé (Nhân viên)
- UC07.2 — Kiểm tra tính hợp lệ (Nhân viên)
- UC07.3 — Cập nhật trạng thái (Nhân viên)

## UC08 · Quản lý bán vé · cinema.controller.sales

- UC08.1 — Tìm suất chiếu (Nhân viên)
- UC08.2 — Kiểm tra ghế trống (Nhân viên)
- UC08.3 — Tính tiền (Nhân viên)
- UC08.4 — Thanh toán (Nhân viên)
- UC08.5 — Phát hành vé (Nhân viên)
- UC08.6 — Kiểm tra giao dịch (Nhân viên)

## UC09 · Báo cáo và thống kê doanh thu · cinema.controller.revenue

- UC09.1 — Doanh thu theo thời gian (Admin)
- UC09.2 — Doanh thu theo phim (Admin)
- UC09.3 — Doanh thu theo nhân viên (Admin)
- UC09.4 — Thống kê theo vé và F&B (Admin)
