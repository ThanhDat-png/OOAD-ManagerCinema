# OOAD-ManagerCinema
Object-Oriented Analysis and Design (OOAD) course project developed using Java, focusing on UML modeling, object-oriented principles, software architecture, and system implementation.

## Chạy đồ án với JDK 17 và Maven (PowerShell)

Thiết lập MySQL/JDBC theo [UC01](docs/UC01.md) và [UC02](docs/UC02.md), áp dụng schema đến Database/010_san_pham.sql, rồi chạy:

~~~powershell
mvn compile exec:java
~~~

Máy hiện tại đã có scripts/Local-MySql.ps1 và scripts/Run-Desktop.ps1 dùng cấu hình công cụ cục bộ. Dự án dùng JDBC và ZXing qua Maven nên cần nạp các dependency khi chạy.

Đã triển khai UC01–UC08. Bước tiếp theo là UC09 theo báo cáo. Cuối mỗi phiên ghi nhận kết quả và kế hoạch; chỉ commit hoặc push khi chủ đồ án quyết định. Xem [danh mục đối chiếu](docs/doi-chieu.md).

## Tổ chức theo mẫu đồ án

Mở pom.xml trong IDE dưới dạng dự án Maven với JDK 17, chạy cinema.Main để mở cửa sổ Swing. Biên dịch bằng mvn compile để nạp đủ dependency.

Trong cinema, view hiển thị dữ liệu, controller điều phối các UC01–UC09, model mô tả đối tượng, dao thực hiện truy vấn và db quản lý kết nối JDBC. Các lớp cụ thể được bổ sung cùng nghiệp vụ tương ứng.

Database/001_tai_khoan.sql là phần đầu của schema MySQL 8, gồm ba bảng liên quan đến UC01 theo trang 01 của sơ đồ LuocDo_CSDL_RapChieuPhim (2).drawio. Chạy script một lần trong database trống đã chọn. Phiên triển khai UC01 cần đối chiếu thêm các quy tắc ở trang 02 trước khi hoàn thiện ràng buộc và kết nối DAO.

## UC01 đăng nhập

Xem [thiết lập và kiểm tra UC01](docs/UC01.md). Chạy bằng Maven để nạp MySQL JDBC driver.

## UC02 và MySQL cục bộ

Xem [hướng dẫn UC02](docs/UC02.md) để bật/tắt MySQL riêng của đồ án và chạy ứng dụng với các tài khoản demo.

## UC03 quản lý phim

Xem [chức năng và đối chiếu UC03](docs/UC03.md). Khi chuyển máy, mang theo data/posters cùng bản sao CSDL để giữ ảnh đã lưu.

## UC04 quản lý lịch chiếu

Xem [đối chiếu và hướng dẫn UC04](docs/UC04.md). Đã có phòng demo DEMO01. Cần khai báo ngày lễ trong config/calendar.properties trước khi sử dụng phân loại ngày lễ để tính giá.

## UC05 quản lý phòng và ghế

Xem [đối chiếu và hướng dẫn UC05](docs/UC05.md). Có màn hình sơ đồ ghế, danh sách và các thao tác theo vai trò. CSDL mới cần nhập đến script 009.

## UC06 quản lý giá vé và F&B

Xem [đối chiếu và hướng dẫn UC06](docs/UC06.md). Tiền dùng VND nguyên, nhập bằng chữ số. CSDL mới cần nhập đến script 010.

## UC07 kiểm tra và xác nhận vé

Xem [đối chiếu và hướng dẫn UC07](docs/UC07.md). Khoảng vào phòng đã thống nhất là 15 phút trước/sau giờ bắt đầu, cấu hình qua config/admission.properties. Có tra cứu mã vé/QR và đọc QR từ ảnh.

## UC08 quản lý bán vé

Xem [đối chiếu và hướng dẫn UC08](docs/UC08.md). Thời hạn giữ ghế đã thống nhất là 5 phút, cấu hình config/sales.properties. Có chọn/giữ ghế, chốt giá, xác nhận thanh toán tại quầy, phát hành vé QR, xem trước/in lại và kiểm tra giao dịch. Khi chưa rõ kết quả, kiểm tra mã giao dịch trước khi thực hiện lại.

## Tiến độ và phiên tiếp theo

Xem [bàn giao cuối phiên](docs/PHIEN_TIEP_THEO.md) để tiếp tục UC09 và tổng rà soát đồ án.
