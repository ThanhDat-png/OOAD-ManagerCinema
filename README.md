# OOAD-ManagerCinema
Object-Oriented Analysis and Design (OOAD) course project developed using Java, focusing on UML modeling, object-oriented principles, software architecture, and system implementation.

## Chạy khung đồ án với JDK 17 (PowerShell)

```powershell
$files = (Get-ChildItem src/main/java -Recurse -Filter *.java).FullName
javac -encoding UTF-8 -d out $files
java -cp out cinema.Main
```

Các package trong src/main/java/cinema tương ứng với mục 3.3 của báo cáo. Xem [danh mục đối chiếu](docs/doi-chieu.md). Phiên tiếp theo triển khai UC01 sau khi đối chiếu lớp, trình tự đăng nhập và các trường hợp từ chối đăng nhập. Cuối mỗi phiên ghi nhận ca sử dụng đã hoàn thành, kết quả kiểm tra và bước kế tiếp. Chỉ commit hoặc push khi chủ đồ án quyết định.

## Tổ chức theo mẫu đồ án

Mở pom.xml trong IDE dưới dạng dự án Maven với JDK 17, chạy cinema.Main để mở cửa sổ Swing. Có thể biên dịch bằng mvn compile hoặc lệnh javac ở trên.

Trong cinema, view hiển thị dữ liệu, controller điều phối các UC01–UC09, model mô tả đối tượng, dao thực hiện truy vấn và db quản lý kết nối JDBC. Các lớp cụ thể được bổ sung cùng nghiệp vụ tương ứng.

Database/001_tai_khoan.sql là phần đầu của schema MySQL 8, gồm ba bảng liên quan đến UC01 theo trang 01 của sơ đồ LuocDo_CSDL_RapChieuPhim (2).drawio. Chạy script một lần trong database trống đã chọn. Phiên triển khai UC01 cần đối chiếu thêm các quy tắc ở trang 02 trước khi hoàn thiện ràng buộc và kết nối DAO.
