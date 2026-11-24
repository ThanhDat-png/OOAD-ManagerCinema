# Cinema Workspace trên trình duyệt

Website dùng Java 17, Spring MVC, Thymeleaf và phân quyền bằng Spring Security. Bản web và bản Swing có hai điểm khởi chạy. Bản Swing tiếp tục dùng MySQL. Bản web chạy SQL tương thích libSQL trên Turso hoặc SQLite khi phát triển cục bộ.

## Chạy thử

Khách mở `/cinema` để duyệt phim, xem giờ chiếu, chọn ghế và nhận mã mang đến quầy. Giá hiển thị là giá tạm tính. Mã có hiệu lực tối đa 30 phút và hết hạn khi suất chiếu bắt đầu. Ghế vẫn có thể được người khác mua trước khi quầy tiếp nhận.

Để diễn thử với hai vai trò, mở giao diện khách trong một cửa sổ và đăng nhập `staff_demo` tại `/login` ở cửa sổ khác. Sau khi khách gửi lựa chọn, mở lại **Quầy bán vé**, mở **Nhận mã chọn ghế của khách**, rồi nhận yêu cầu từ danh sách hoặc nhập mã. Hệ thống kiểm tra lại ghế, giữ ghế đang trống và tính giá hiện hành. Nhân viên hoàn tất thanh toán tại quầy để phát hành vé QR và kiểm tra vé khi vào rạp.

Từ thư mục dự án trên Windows, chạy `./scripts/Run-Web.ps1`. Mở [localhost:8080](http://localhost:8080). Có thể chọn cổng với `./scripts/Run-Web.ps1 -Port 8081`. Trên máy có Maven, lệnh tương đương là `mvn spring-boot:run`. Để đóng gói, chạy `mvn package` rồi `java -jar target/manager-cinema-1.0-SNAPSHOT.jar`.

Ở lần chạy cục bộ đầu tiên, chương trình khởi tạo `data/cinema-web.sqlite`, tài khoản demo, ba phim có poster, hai phòng, bảng giá, tồn kho và lịch chiếu trong 14 ngày tính từ hôm khởi tạo. Khi hết lịch demo, Admin có thể lập suất mới cho ngày trình diễn.

| Vai trò | Tên đăng nhập cục bộ | Mật khẩu cục bộ |
| --- | --- | --- |
| Admin | `admin_demo` | `Cinema@123` |
| Nhân viên | `staff_demo` | `Cinema@123` |

Bắt đầu ở **Lịch chiếu** với Admin và **Quầy bán vé** với Nhân viên. Khi thanh toán chưa rõ kết quả, tra cứu mã giao dịch trước khi thao tác tiếp. In lại vé từ chi tiết giao dịch. Để thử xác nhận khách vào rạp, chọn đúng phòng, suất chiếu và thời điểm trong khoảng 15 phút trước hoặc sau giờ chiếu.

## Triển khai lên Render với Turso

1. Dùng một cơ sở dữ liệu Turso riêng cho bản trình diễn. Lấy URL cơ sở dữ liệu và token có quyền đọc, ghi, tạo schema trên cơ sở dữ liệu đó.
2. Triển khai Blueprint từ nhánh chứa bản web, dùng `render.yaml` ở thư mục gốc. Render dựng Docker image và kiểm tra `/health`. Khai báo `TURSO_DATABASE_URL` và `TURSO_AUTH_TOKEN` trong phần biến môi trường bảo mật.
3. Blueprint chọn dịch vụ Free, đặt cookie HTTPS và sinh riêng `CINEMA_ADMIN_PASSWORD`, `CINEMA_STAFF_PASSWORD`. Khi khởi tạo dữ liệu demo trên Turso, dịch vụ sử dụng hai mật khẩu này. Xem giá trị trong tài khoản Render để đăng nhập và cung cấp riêng cho người trình diễn. Việc đổi giá trị khởi tạo sau khi đã tạo tài khoản không cập nhật mật khẩu đã lưu.
4. Mở địa chỉ HTTPS Render cấp, đăng nhập từng vai trò, thực hiện một giao dịch, kiểm tra báo cáo và quét lại QR sau khi triển khai lại. Báo cáo dùng múi giờ Việt Nam. Dịch vụ Free có thể mất khoảng một phút để khởi động lại sau khi ngủ. [Tài liệu Render](https://render.com/docs/free)

Dữ liệu giao dịch, vé và nội dung poster nằm trong cơ sở dữ liệu. Phiên trình duyệt do máy chủ quản lý. Sau khi máy chủ khởi động lại, nhân viên đăng nhập lại và tìm giao dịch qua mã để tiếp tục theo trạng thái đã lưu. Mẫu cấu hình chỉ chứa tên biến môi trường.

## Sao lưu và khôi phục

Cài Turso CLI và xác thực cho tài khoản sở hữu cơ sở dữ liệu. Chạy lệnh sao lưu vào một thư mục riêng. Mỗi bản xuất được kiểm tra bằng cách phục hồi tạm thời và kiểm tra toàn vẹn dữ liệu. Có thể đặt lịch chạy lệnh đầu bằng Windows Task Scheduler.

```powershell
python scripts/web_db_backup.py backup --turso TEN_DATABASE --output-dir C:/CinemaBackups
python scripts/web_db_backup.py restore C:/CinemaBackups/BAN_SAO.sql --sqlite C:/CinemaBackups/khoi-phuc.sqlite
turso db create TEN_DATABASE_MOI --from-file C:/CinemaBackups/khoi-phuc.sqlite
```

Khi diễn tập với dữ liệu cục bộ, dùng `backup --sqlite data/cinema-web.sqlite`. Dùng tên mới khi khôi phục và kiểm tra số giao dịch, vé, doanh thu và nội dung poster trước khi chuyển ứng dụng sang bản phục hồi.

## Kiểm thử

[Ma trận 45 use case](WEB_USE_CASES.md) lấy thứ tự và luồng thay thế từ Google Doc lần 4. `mvn test` chạy kiểm thử web và các bài kiểm thử Java của dự án. Các bài tích hợp MySQL của ứng dụng Swing cần cấu hình cơ sở dữ liệu riêng theo tài liệu cũ. Bản web có kiểm thử SQLite, mô phỏng giao thức HTTP của Turso, xác thực/phân quyền, chỉnh sửa dữ liệu, tranh chấp giữ ghế, thanh toán, phát hành và soát vé. Bài kiểm thử giao thức mô phỏng không thay thế cho kiểm tra trên cơ sở dữ liệu Turso thật.

Để chạy lại hành trình trình duyệt trên Windows, dùng Python có gói `playwright`, cài Microsoft Edge, chạy `mvn package`, sau đó chạy `python tools/browser_acceptance.py`. Công cụ khởi động JAR ở một cổng trống, tạo cơ sở dữ liệu thử riêng, rồi ghi ảnh chụp và vé PDF vào `target/web-qa`.

Ngày 19/11/2026, Maven ghi nhận 67 bài đạt, gồm cả 18 bài web. Bộ 113 bài tích hợp MySQL cũ được bỏ qua ở cấu hình mặc định. Sau khi khởi động, đo năm yêu cầu HTTP liên tiếp cho mỗi trang bằng tài khoản Admin trên localhost với SQLite. Trung vị là 54 ms cho thư viện phim, 46 ms cho phòng chiếu và 42 ms cho nhân viên. Thời gian lớn nhất trong 15 lần đo là 277 ms. Phép đo bao gồm thời gian nhận toàn bộ HTML trên máy cục bộ.
