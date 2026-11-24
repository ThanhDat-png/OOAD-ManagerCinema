# OOAD-ManagerCinema
Object-Oriented Analysis and Design (OOAD) course project developed using Java, focusing on UML modeling, object-oriented principles, software architecture, and system implementation.

## Chạy đồ án với JDK 17 và Maven (PowerShell)

Cài mới/chuyển máy theo [hướng dẫn cài đặt](docs/CAI_DAT.md): tạo database mới bằng tệp gộp [S2_Nhom08_DB.sql](Database/S2_Nhom08_DB.sql) (hoặc chạy `scripts/Import-Database.ps1` cho các tệp `001`–`012`), tạo tài khoản JDBC và cấu hình riêng, rồi chạy:

~~~powershell
mvn compile exec:java
~~~

Run-Desktop.ps1 dùng Maven trong PATH nếu chưa có cấu hình công cụ cục bộ. Local-MySql.ps1 chỉ dành cho MySQL ZIP riêng; máy dùng dịch vụ MySQL không cần script này. Dự án dùng JDBC, FlatLaf và ZXing qua Maven nên cần nạp các dependency khi chạy.

Hệ thống đã hoàn thiện đầy đủ **UC01–UC09**, sơ đồ lớp ([SoDoLop_RapChieuPhim.drawio](docs/SoDoLop_RapChieuPhim.drawio)), báo cáo ([S2_Nhom08_Lan4.docx](docs/S2_Nhom08_Lan4.docx)) và bộ dữ liệu demo chuẩn ([011_demo_chuan.sql](Database/011_demo_chuan.sql)). Xem [danh mục đối chiếu](docs/doi-chieu.md).

## Tổ chức theo mẫu đồ án

Mở pom.xml trong IDE dưới dạng dự án Maven với JDK 17, chạy cinema.Main để mở cửa sổ Swing. Biên dịch bằng mvn compile để nạp đủ dependency.

Trong cinema, view hiển thị dữ liệu, controller điều phối các UC01–UC09, model mô tả đối tượng, dao thực hiện truy vấn và db quản lý kết nối JDBC.

`Database/S2_Nhom08_DB.sql` là bản gộp chuẩn hóa từ `Database/001`–`012` (20 bảng, 6 trigger, 26 khóa ngoại kèm dữ liệu demo cho UC01–UC09 và luồng khách chọn ghế nhận mã tại quầy) để nhập nhanh bằng một tệp duy nhất. Có thể dùng `scripts/Import-Database.ps1` để nhập đúng thứ tự vào database chưa tồn tại theo [CAI_DAT.md](docs/CAI_DAT.md). Máy đã có sẵn CSDL bản `011` chỉ cần chạy thêm [012_chon_ghe_khach.sql](Database/012_chon_ghe_khach.sql). Không nhập lại toàn bộ lên database đang dùng.

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

## UC09 báo cáo và thống kê

Xem [đối chiếu và hướng dẫn UC09](docs/UC09.md). Admin xem doanh thu theo ngày/tháng/năm/khoảng ngày, phim, nhân viên; thống kê vé và tồn kho F&B. Có phân biệt tiền đã thu và vé chờ phát hành. Báo cáo chỉ đọc, dùng giá lịch sử.
