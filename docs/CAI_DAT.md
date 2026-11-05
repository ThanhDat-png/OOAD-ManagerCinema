# Cài mới và chuyển máy

## 1. Chuẩn bị

Dự án dùng JDK 17, Maven 3.9 và MySQL 8. Máy phải có giao diện desktop để chạy Swing/kiểm thử desktop. Cài JDK/Maven vào PATH, cài và khởi động MySQL, tải mã nguồn từ GitHub rồi mở PowerShell tại thư mục có pom.xml. Lần đầu Maven cần kết nối kho dependency nếu máy chưa có bộ nhớ đệm.

~~~powershell
java -version
mvn --version
~~~

JAVA_HOME phải trỏ vào thư mục JDK. Tên database, tài khoản, mật khẩu và cổng dưới đây do bạn thiết lập trên máy mới; cổng mẫu là 3306. Máy đang phát triển dùng MySQL ZIP riêng ở cổng 3307, không phải cấu hình bắt buộc của dự án.

## 2. Nhập CSDL mới

Sao chép config/mysql-client.cnf.example thành config/mysql-client.cnf, điền host/port và tài khoản quản trị MySQL có quyền tạo database, bảng, trigger và tài khoản ứng dụng. File này được Git bỏ qua. Không cần đặt mật khẩu quản trị vào lệnh hoặc cấu hình JDBC.

~~~powershell
Copy-Item config/mysql-client.cnf.example config/mysql-client.cnf
# Sửa file trước khi chạy bước tiếp theo.
$mysqlClient='C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
$adminFile=(Resolve-Path config/mysql-client.cnf).Path
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Import-Database.ps1 -MySqlExe $mysqlClient -ClientConfig $adminFile -DatabaseName manager_cinema
~~~

Điều chỉnh đường dẫn mysql.exe theo máy cài đặt thực tế. Script tạo database utf8mb4 rồi nhập đủ SQL theo thứ tự 001–010, bao gồm tài khoản/danh mục demo. Script đọc file bằng UTF-8 và dùng MySQL client để xử lý DELIMITER của trigger, không dùng toán tử chuyển hướng đầu vào của PowerShell.

Script từ chối database đã tồn tại, không có tùy chọn ghi đè/xóa và không tự chạy lại migration. Nếu nhập bị lỗi, kiểm tra lỗi và dùng database thử có tên mới hoặc xử lý riêng database nhập dở; không nhập lại lên database đang dùng. Không chạy riêng 001–010 lần nữa sau khi script thành công.

Kết quả đầy đủ: **18 bảng, 6 trigger, 22 khóa ngoại**. Có tài khoản Admin/Nhân viên và phòng DEMO01 với 5 ghế sẵn sàng, 1 ghế bảo trì; có danh mục loại phòng, ghế, thể loại, thời điểm. Cài mới chưa có phim/lịch chiếu, bảng giá bán hoặc sản phẩm F&B. Các phần này được tạo qua chức năng Admin, không phải lỗi cài đặt.

## 3. Tạo tài khoản kết nối ứng dụng

Mở MySQL client bằng file quản trị đã cấu hình:

~~~powershell
& $mysqlClient "--defaults-extra-file=$adminFile" --default-character-set=utf8mb4
~~~

Trong MySQL client, chạy các câu sau, thay mật khẩu mẫu bằng mật khẩu bạn chọn:

~~~sql
CREATE USER 'cinema_app'@'127.0.0.1' IDENTIFIED BY 'THAY_MAT_KHAU_CUA_BAN';
GRANT SELECT, INSERT, UPDATE, DELETE ON manager_cinema.* TO 'cinema_app'@'127.0.0.1';
EXIT;
~~~

Nếu tài khoản này đã tồn tại, dùng tài khoản ứng dụng khác hoặc quản lý mật khẩu/quyền của tài khoản hiện có; không xóa tài khoản đang dùng. Tài khoản chạy ứng dụng chỉ cần bốn quyền dữ liệu trên database của đồ án, không cần CREATE/DROP/TRIGGER. Nếu chọn tên database khác ở bước 2 thì phải đổi tên tương ứng trong GRANT và URL JDBC.

## 4. Tạo cấu hình trên máy mới

~~~powershell
Copy-Item config/database.properties.example config/database.properties
Copy-Item config/calendar.properties.example config/calendar.properties
Copy-Item config/admission.properties.example config/admission.properties
Copy-Item config/sales.properties.example config/sales.properties
~~~

Sửa database.properties để khớp MySQL/tài khoản bước 2–3:

~~~properties
db.url=jdbc:mysql://127.0.0.1:3306/manager_cinema?connectTimeout=5000&socketTimeout=10000&characterEncoding=UTF-8
db.user=cinema_app
db.password=THAY_MAT_KHAU_CUA_BAN
~~~

Đây là định dạng Java Properties: dấu gạch chéo ngược trong giá trị cần escape, ký tự Unicode trong mật khẩu có thể dùng escape Unicode của Properties. Không sao chép đường dẫn/mật khẩu máy cũ một cách mặc định. Khi dùng MySQL hiện tại cổng 3307, phải sửa cổng trong URL và file client cho đồng nhất.

Giữ cấu hình đã thống nhất: Thời hạn giữ ghế được đặt qua thuộc tính holdMinutes=5 trong sales.properties; admission.properties có beforeMinutes=15 và afterMinutes=15. calendar.properties chứa holidays theo yyyy-MM-dd, phân cách dấu phẩy. Để trống nghĩa là chưa khai báo ngày lễ; cần điền lịch ngày lễ dùng trong đồ án để tính đúng loại thời điểm.

## 5. Chạy và xác nhận

Tại thư mục gốc repo:

~~~powershell
mvn compile exec:java
# Hoặc dùng script PowerShell, script tự chuyển working directory về gốc repo:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Run-Desktop.ps1
~~~

Run-Desktop.ps1 dùng Maven trong PATH khi chưa có config/local-tools.json. Nếu có file này, script dùng đường dẫn maven được khai báo. Trong IDE, mở pom.xml, dùng JDK 17, chạy cinema.Main với working directory là thư mục gốc repo để tìm đúng config và data/posters.

Đăng nhập admin_demo hoặc staff_demo, mật khẩu demo Cinema@123. locked_demo và inactive_demo phải bị từ chối. Tài khoản mẫu chỉ dành cho đồ án/demo. Admin mở được các chức năng quản lý và báo cáo; Nhân viên mở bán vé/kiểm vé đúng phân quyền.

Để bán vé demo, Admin tạo phim, lịch chiếu tương lai ở DEMO01 và thiết lập giá vé tương ứng qua UC06. Không cần nhập trực tiếp giao dịch/thanh toán vào database.

Kiểm tra toàn bộ trên **database cài thử riêng** có đầy đủ schema và dữ liệu demo:

~~~powershell
mvn clean test '-Dcinema.mysql.tests=true'
~~~

Các test MySQL/Swing cần kết nối được server và tài khoản demo; không chạy khi server đang tắt. mvn test không bật thuộc tính trên chỉ chạy nhóm không phụ thuộc MySQL, nhóm MySQL sẽ bị bỏ qua. Maven/Java tải dependency thành công chưa đồng nghĩa đã kiểm chứng kết nối JDBC, vì vậy cần chạy nhóm tích hợp ở trên.

## 6. MySQL ZIP riêng — tùy chọn

Nếu máy dùng MySQL được cài như dịch vụ, khởi động/dừng qua dịch vụ đó; không cần local-tools.json hoặc Local-MySql.ps1. Nếu muốn dùng MySQL ZIP riêng như máy phát triển, tự chuẩn bị runtime/my.ini và file client quản trị, sao chép config/local-tools.json.example thành config/local-tools.json, sửa toàn bộ đường dẫn theo máy mới. Các file cấu hình/runtime đó không được chứa trong Git.

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Local-MySql.ps1 -Action Start
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Run-Desktop.ps1
# Sau khi đóng ứng dụng:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Local-MySql.ps1 -Action Stop
~~~

## 7. Chuyển máy và dữ liệu

Với cài mới để chạy lại đồ án: lấy mã nguồn, làm bước 1–5, tạo cấu hình riêng. Không cần chép target, out, thư mục Maven/MySQL từ máy cũ hoặc local-tools.json của máy cũ. Các file *.example mới là mẫu được lưu trong Git.

Nếu cần giữ dữ liệu đang làm: sao lưu CSDL hiện tại trước, mang theo data/posters và các ngày lễ/cấu hình nghiệp vụ đã chọn. Khôi phục bản sao lưu bằng tài khoản quản trị vào một database trống riêng rồi trỏ JDBC vào đó; **không chạy script nhập 001–010 lên bản sao lưu đã khôi phục**. Bản dump phải có đầy đủ bảng, dữ liệu và trigger, và các trigger phải có DEFINER hợp lệ trên máy đích. Quy trình phục hồi dữ liệu cũ không phải quy trình cài schema mới trong mục 2; cần kiểm chứng riêng khi thực hiện di chuyển dữ liệu thật. Kiểm tra lại tài khoản/mật khẩu, cổng và đường dẫn poster trên máy mới.

## 8. Xử lý lỗi thường gặp

- Connection refused/Communications link failure: kiểm tra server đang chạy và host/cổng trong JDBC.
- Access denied: kiểm tra user/password, host của tài khoản MySQL và GRANT đúng database.
- Unknown database/table hoặc thiếu cột: kiểm tra đủ SQL 001–010; không nhập chồng lên database đang dùng để thử sửa.
- Chưa đọc được cấu hình: đặt file đúng config/database.properties và chạy từ gốc repo.
- Không tìm thấy Maven/JDK: kiểm tra PATH, JAVA_HOME, mvn --version; local-tools.json nếu có phải trỏ đúng máy hiện tại.
- Bảng giá/suất/phim trống sau cài mới: tạo dữ liệu nghiệp vụ qua Admin như mục 5; schema/danh mục demo không cung cấp sẵn giao dịch bán vé.

## 9. Phạm vi kiểm chứng

Đã kiểm tra cài mới bằng bản sao chỉ chứa file Git cộng các script/cấu hình mẫu của mục này, trong thư mục có dấu cách, không mang config/local-tools.json hoặc database.properties cũ. Dùng database và user tạm riêng, MySQL 8.4.13, JDK 17 và Maven 3.9.9 đang có trên máy. Đã nhập đủ bảng/trigger/khóa ngoại và xác nhận từ chối nhập lại. Đây là kiểm tra cài mới mã nguồn/CSDL trên cùng máy, chưa phải kiểm tra trên một máy Windows vật lý khác hoặc Maven không có cache.

Kết quả ngày 05/11/2026: nhập mới thành công, chạy đủ 144/144 kiểm thử với tài khoản ứng dụng chỉ có SELECT/INSERT/UPDATE/DELETE; 0 lỗi, 0 bỏ qua. Checksum cả 18 bảng và cấu hình JDBC của database đang dùng không đổi. Launcher đã tìm được Maven qua PATH và compile thành công khi không có local-tools.json (bỏ qua riêng bước mở ứng dụng tương tác trong lần kiểm tra launcher; giao diện đã được kiểm tra bởi bộ test desktop). Đã xóa database, tài khoản và cấu hình JDBC tạm sau kiểm tra. Chủ đồ án đã yêu cầu commit/push phần cài đặt sau khi xác nhận kết quả kiểm tra.
