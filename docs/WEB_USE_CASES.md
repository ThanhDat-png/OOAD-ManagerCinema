# Cinema Workspace · 45 ca nghiệm thu

Đối chiếu giao diện web với mục 3.3 của [S2_Nhom08_Lan4](https://docs.google.com/document/d/1wKfSNIxwAfCds7Jt31CHKjY20Ck4u3SqqCOmES1YBqc/edit#heading=h.kmm11x4nd7f0). Mỗi mục ghi màn hình bắt đầu, tác nhân, thao tác cần thử, các tình huống thay thế và kết quả cần đạt. Đây là tiêu chí nghiệm thu. Kết quả chạy kiểm thử được ghi riêng trong WEB.md.

### UC01 · Đăng nhập

**Admin, Nhân viên** · `/login`

1. Người dùng mở giao diện đăng nhập.
2. Người dùng nhập tên đăng nhập và mật khẩu.
3. Người dùng chọn “Đăng nhập”.
4. Hệ thống kiểm tra thông tin đã nhập, đối chiếu tài khoản và mật khẩu với dữ liệu lưu trữ, đồng thời kiểm tra trạng thái tài khoản.
5. Hệ thống xác định vai trò của người dùng và thiết lập phiên đăng nhập.
6. Hệ thống chuyển sang giao diện làm việc tương ứng với vai trò Admin hoặc Nhân viên.

4.1. Người dùng chưa nhập tên đăng nhập hoặc mật khẩu:
• Hệ thống thông báo yêu cầu nhập đầy đủ thông tin.
• Quay lại bước 2.
4.2. Tên đăng nhập hoặc mật khẩu không chính xác:
• Hệ thống thông báo “Tên đăng nhập hoặc mật khẩu không chính xác”.
• Không thiết lập phiên đăng nhập.
• Quay lại bước 2.
4.3. Tài khoản đã bị khóa hoặc ngừng hoạt động:
• Hệ thống thông báo “Tài khoản không được phép truy cập. Vui lòng liên hệ Admin”.
• Không cho phép đăng nhập.

• Khi thành công: phiên đăng nhập được thiết lập; người dùng được chuyển đến giao diện làm việc và chỉ được truy cập các chức năng phù hợp với vai trò.
• Khi thất bại: không thiết lập phiên đăng nhập; người dùng vẫn ở giao diện đăng nhập.

### UC02.1.1 · Thêm nhân viên

**Admin** · `/admin/employees`

1. Admin chọn chức năng “Quản lý nhân viên”.
2. Hệ thống hiển thị danh sách nhân viên.
3. Admin chọn “Thêm nhân viên”.
4. Hệ thống hiển thị biểu mẫu thêm nhân viên.
5. Admin nhập thông tin nhân viên gồm họ tên, ngày sinh, số điện thoại, email, địa chỉ và chức vụ.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra thông tin đã nhập và lưu hồ sơ nhân viên vào cơ sở dữ liệu.
8. Hệ thống thông báo “Thêm nhân viên thành công” và cập nhật danh sách nhân viên.

6.1. Admin chọn “Hủy”: Hệ thống không lưu thông tin đã nhập và trở về danh sách nhân viên.
7.1. Thông tin bắt buộc bị bỏ trống hoặc dữ liệu không hợp lệ: Hệ thống thông báo các trường cần điều chỉnh, giữ thông tin đã nhập và quay lại bước 5.

Khi thành công, hồ sơ nhân viên mới được lưu và xuất hiện trong danh sách nhân viên. Nếu Admin hủy hoặc lưu thất bại, không tạo hồ sơ mới.

### UC02.1.2 · Sửa nhân viên

**Admin** · `/admin/employees`

1. Admin chọn chức năng “Quản lý nhân viên”.
2. Hệ thống hiển thị danh sách nhân viên.
3. Admin chọn nhân viên cần sửa và chọn “Sửa nhân viên”.
4. Hệ thống hiển thị biểu mẫu cùng thông tin hiện tại của nhân viên.
5. Admin chỉnh sửa các thông tin cần thay đổi.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra dữ liệu và lưu các thay đổi vào cơ sở dữ liệu.
8. Hệ thống thông báo “Cập nhật nhân viên thành công” và hiển thị thông tin đã cập nhật.

6.1. Admin chọn “Hủy”: Hệ thống không lưu các thay đổi và trở về danh sách nhân viên.
7.1. Thông tin bắt buộc bị bỏ trống hoặc dữ liệu không hợp lệ: Hệ thống thông báo các trường cần điều chỉnh, giữ nội dung chỉnh sửa và quay lại bước 5.

Khi thành công, thông tin nhân viên được cập nhật. Nếu Admin hủy hoặc cập nhật thất bại, thông tin đã lưu trước đó được giữ nguyên.

### UC02.1.3 · Xóa nhân viên

**Admin** · `/admin/employees`

1. Admin chọn chức năng “Quản lý nhân viên”.
2. Hệ thống hiển thị danh sách nhân viên.
3. Admin chọn nhân viên cần xóa và chọn “Xóa nhân viên”.
4. Hệ thống hiển thị thông báo “Bạn có chắc chắn muốn xóa nhân viên này?”.
5. Admin chọn “Có”.
6. Hệ thống kiểm tra dữ liệu liên quan đến nhân viên và xác định hồ sơ được phép xóa.
7. Hệ thống thực hiện xóa hồ sơ, thông báo “Xóa nhân viên thành công” và cập nhật danh sách nhân viên.

5.1. Admin chọn “Không”: Hệ thống đóng thông báo xác nhận và giữ nguyên hồ sơ nhân viên.
6.1. Nhân viên đã có dữ liệu giao dịch cần lưu giữ: Hệ thống thông báo “Không thể xóa nhân viên vì có dữ liệu giao dịch liên quan” và không thực hiện xóa.

Khi thành công, hồ sơ nhân viên được xóa khỏi danh sách quản lý. Nếu thao tác bị hủy, bị từ chối hoặc thất bại, hồ sơ được giữ nguyên. Dữ liệu lịch sử giao dịch không bị xóa bởi thao tác này.

### UC02.2 · Xem thông tin chi tiết nhân viên

**Admin, Nhân viên** · `/admin/employees · /profile`

1. Admin truy cập danh sách nhân viên; Nhân viên truy cập chức năng xem thông tin cá nhân.
2. Admin chọn nhân viên cần xem; đối với Nhân viên, hệ thống xác định hồ sơ tương ứng với tài khoản đang đăng nhập.
3. Hệ thống kiểm tra quyền truy cập và lấy thông tin hồ sơ.
4. Hệ thống hiển thị thông tin chi tiết gồm mã nhân viên, họ tên, ngày sinh, số điện thoại, email, địa chỉ và chức vụ.
5. Người dùng chọn “Trở về” để quay lại giao diện trước đó.

3.1. Không tìm thấy hồ sơ nhân viên: Hệ thống thông báo “Không tìm thấy thông tin nhân viên” và trở về giao diện trước đó.
3.2. Nhân viên yêu cầu xem hồ sơ của người khác: Hệ thống từ chối và thông báo “Bạn không có quyền xem thông tin này”.

Thông tin được hiển thị đúng quyền truy cập. Việc xem hồ sơ không làm thay đổi dữ liệu nhân viên.

### UC02.3 · Tìm kiếm nhân viên

**Admin** · `/admin/employees`

1. Admin chọn chức năng “Quản lý nhân viên”.
2. Hệ thống hiển thị danh sách nhân viên và ô tìm kiếm.
3. Admin nhập mã nhân viên, họ tên hoặc số điện thoại cần tìm.
4. Admin chọn “Tìm kiếm”.
5. Hệ thống tìm các hồ sơ phù hợp với thông tin đã nhập.
6. Hệ thống hiển thị danh sách kết quả tìm kiếm.

4.1. Admin không nhập từ khóa: Hệ thống hiển thị toàn bộ danh sách nhân viên.
5.1. Không có hồ sơ phù hợp: Hệ thống hiển thị danh sách kết quả trống và thông báo “Không tìm thấy nhân viên phù hợp”.

Hệ thống hiển thị kết quả theo thông tin tìm kiếm. Dữ liệu nhân viên không bị thay đổi.

### UC03.1 · Thêm phim

**Admin** · `/catalog/films`

1. Admin chọn chức năng “Quản lý phim”.
2. Hệ thống hiển thị danh sách phim.
3. Admin chọn “Thêm phim”.
4. Hệ thống hiển thị biểu mẫu thêm phim.
5. Admin nhập tên phim, thời lượng, thể loại, đạo diễn, ngày khởi chiếu, giới hạn độ tuổi, poster và trạng thái chiếu.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra dữ liệu và lưu thông tin phim vào cơ sở dữ liệu.
8. Hệ thống thông báo “Thêm phim thành công” và cập nhật danh sách phim.

6.1. Admin chọn “Hủy”: Hệ thống không lưu thông tin đã nhập và trở về danh sách phim.
7.1. Thông tin bắt buộc bị bỏ trống, thời lượng không lớn hơn 0 hoặc dữ liệu không hợp lệ: Hệ thống thông báo các trường cần điều chỉnh, giữ thông tin đã nhập và quay lại bước 5.

Khi thành công, phim mới được lưu và xuất hiện trong danh sách phim. Nếu Admin hủy hoặc lưu thất bại, không tạo phim mới.

### UC03.2.1 · Sửa tên phim

**Admin** · `/catalog/films`

1. Admin chọn chức năng “Quản lý phim”.
2. Hệ thống hiển thị danh sách phim.
3. Admin chọn phim cần sửa tên.
4. Hệ thống hiển thị thông tin hiện tại của phim.
5. Admin nhập tên phim mới và chọn “Lưu”.
6. Hệ thống kiểm tra tên phim và lưu thay đổi.
7. Hệ thống thông báo “Cập nhật tên phim thành công” và hiển thị tên phim đã cập nhật.

5.1. Admin chọn “Hủy”: Hệ thống không lưu thay đổi và giữ nguyên tên phim.
6.1. Tên phim bị bỏ trống: Hệ thống thông báo “Vui lòng nhập tên phim” và quay lại bước 5.

Khi thành công, tên phim được cập nhật; các thông tin khác của phim được giữ nguyên. Nếu thao tác bị hủy hoặc thất bại, tên phim không thay đổi.

### UC03.2.2 · Cập nhật poster phim

**Admin** · `/catalog/films`

1. Admin chọn chức năng “Quản lý phim”.
2. Hệ thống hiển thị danh sách phim.
3. Admin chọn phim cần cập nhật poster.
4. Hệ thống hiển thị thông tin và poster hiện tại của phim.
5. Admin chọn ảnh poster mới.
6. Hệ thống hiển thị ảnh đã chọn để Admin kiểm tra.
7. Admin chọn “Lưu”.
8. Hệ thống lưu poster mới và thông báo “Cập nhật poster thành công”.

5.1. Tệp được chọn không phải định dạng ảnh được hỗ trợ hoặc không thể đọc: Hệ thống thông báo “Ảnh không hợp lệ. Vui lòng chọn ảnh khác” và cho phép Admin chọn lại.
7.1. Admin chọn “Hủy”: Hệ thống không lưu ảnh mới và giữ nguyên poster hiện tại.

Khi thành công, poster mới được lưu và hiển thị cho phim tương ứng. Nếu thao tác bị hủy hoặc thất bại, poster hiện tại được giữ nguyên.

### UC03.2.3 · Cập nhật thể loại phim

**Admin** · `/catalog/films`

1. Admin chọn chức năng “Quản lý phim”.
2. Hệ thống hiển thị danh sách phim.
3. Admin chọn phim cần cập nhật thể loại.
4. Hệ thống hiển thị thông tin và thể loại hiện tại của phim.
5. Admin chọn thể loại phù hợp và chọn “Lưu”.
6. Hệ thống kiểm tra dữ liệu và lưu thay đổi.
7. Hệ thống thông báo “Cập nhật thể loại thành công” và hiển thị thông tin đã cập nhật.

5.1. Admin chọn “Hủy”: Hệ thống không lưu thay đổi và giữ nguyên thể loại hiện tại.
6.1. Admin chưa chọn thể loại: Hệ thống thông báo “Vui lòng chọn thể loại phim” và quay lại bước 5.

Khi thành công, thể loại phim được cập nhật. Nếu thao tác bị hủy hoặc thất bại, thể loại hiện tại được giữ nguyên.

### UC03.3 · Ngừng chiếu phim

**Admin** · `/catalog/films`

1. Admin chọn chức năng “Quản lý phim”.
2. Hệ thống hiển thị danh sách phim.
3. Admin chọn phim cần ngừng chiếu và chọn “Ngừng chiếu”.
4. Hệ thống hiển thị thông báo yêu cầu xác nhận.
5. Admin chọn “Có”.
6. Hệ thống kiểm tra phim không còn suất chiếu đang diễn ra hoặc sắp diễn ra.
7. Hệ thống chuyển trạng thái phim sang “Ngừng chiếu”.
8. Hệ thống thông báo “Ngừng chiếu phim thành công” và cập nhật danh sách phim.

5.1. Admin chọn “Không”: Hệ thống đóng thông báo và giữ nguyên trạng thái phim.
6.1. Phim còn suất chiếu đang diễn ra hoặc sắp diễn ra: Hệ thống từ chối thay đổi trạng thái và thông báo “Phim vẫn còn suất chiếu chưa kết thúc”. Admin cần xử lý các suất chiếu liên quan trước khi thực hiện lại.

Khi thành công, phim chuyển sang trạng thái “Ngừng chiếu” và không được sử dụng để tạo suất chiếu mới. Thông tin phim, suất chiếu và giao dịch đã phát sinh vẫn được lưu giữ. Nếu thao tác bị hủy hoặc thất bại, trạng thái phim không thay đổi.

### UC03.4 · Tra cứu phim

**Admin, Nhân viên** · `/catalog/films`

1. Người dùng truy cập chức năng tra cứu phim.
2. Hệ thống hiển thị danh sách phim và các trường tìm kiếm, lọc dữ liệu.
3. Người dùng nhập tên phim hoặc chọn thể loại, trạng thái chiếu cần tra cứu.
4. Người dùng chọn “Tìm kiếm”.
5. Hệ thống tìm các phim đáp ứng tiêu chí đã chọn.
6. Hệ thống hiển thị danh sách kết quả.
7. Người dùng chọn một phim để xem thông tin chi tiết.

4.1. Người dùng không nhập hoặc chọn tiêu chí: Hệ thống hiển thị toàn bộ danh sách phim được phép tra cứu.
5.1. Không có phim phù hợp: Hệ thống hiển thị danh sách kết quả trống và thông báo “Không tìm thấy phim phù hợp”.

Hệ thống hiển thị kết quả tra cứu và thông tin phim được chọn. Thao tác tra cứu không làm thay đổi dữ liệu phim.

### UC04.1 · Tạo suất chiếu

**Admin** · `/admin/showtimes`

1. Admin chọn chức năng “Quản lý lịch chiếu”.
2. Hệ thống hiển thị danh sách suất chiếu theo ngày và phòng chiếu.
3. Admin chọn “Tạo suất chiếu”.
4. Hệ thống hiển thị biểu mẫu tạo suất chiếu.
5. Admin chọn phim, phòng chiếu, ngày chiếu và giờ bắt đầu.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra dữ liệu, trạng thái phim và trạng thái phòng chiếu.
8. Hệ thống xác định khoảng thời gian sử dụng phòng dựa trên giờ bắt đầu, thời lượng phim và thời gian dọn phòng; thực hiện kiểm tra lịch trùng.
9. Hệ thống lưu suất chiếu mới và thiết lập danh sách ghế theo sơ đồ phòng, trong đó các ghế đang sử dụng được ở trạng thái chưa bán.
10. Hệ thống thông báo “Tạo suất chiếu thành công” và cập nhật danh sách suất chiếu.

6.1. Admin chọn “Hủy”: Hệ thống không lưu thông tin và trở về danh sách suất chiếu.
7.1. Thông tin chưa đầy đủ hoặc thời điểm bắt đầu đã qua: Hệ thống thông báo lỗi, giữ thông tin đã nhập và quay lại bước 5.
7.2. Phim không được phép xếp lịch hoặc phòng chiếu không hoạt động: Hệ thống yêu cầu Admin chọn phim hoặc phòng khác.
8.1. Khoảng thời gian sử dụng phòng bị trùng: Hệ thống thông báo “Lịch chiếu bị trùng”, không lưu suất chiếu và yêu cầu điều chỉnh phòng, ngày hoặc giờ chiếu.

Khi thành công, suất chiếu mới được lưu, không xung đột lịch phòng và có danh sách ghế phục vụ bán vé. Nếu thao tác bị hủy hoặc thất bại, không tạo suất chiếu mới.

### UC04.2.1 · Cập nhật phim chiếu

**Admin** · `/admin/showtimes`

1. Admin truy cập chức năng “Quản lý lịch chiếu”.
2. Admin chọn suất chiếu cần thay đổi phim và chọn “Cập nhật phim chiếu”.
3. Hệ thống kiểm tra suất chiếu chưa bắt đầu, chưa bị hủy và chưa phát sinh vé bán ra.
4. Hệ thống hiển thị phim hiện tại và danh sách phim được phép xếp lịch.
5. Admin chọn phim thay thế và chọn “Lưu”.
6. Hệ thống xác định lại khoảng thời gian sử dụng phòng theo thời lượng phim mới và thực hiện kiểm tra lịch trùng.
7. Hệ thống lưu thay đổi và thông báo “Cập nhật phim chiếu thành công”.

3.1. Suất chiếu đã bắt đầu, đã bị hủy hoặc đã phát sinh vé bán ra: Hệ thống thông báo không được phép thay đổi phim và kết thúc thao tác.
5.1. Admin chọn “Hủy”: Hệ thống giữ nguyên phim hiện tại.
6.1. Phim được chọn không còn được phép xếp lịch: Hệ thống yêu cầu chọn phim khác.
6.2. Thời lượng phim mới gây trùng lịch phòng: Hệ thống thông báo xung đột, không lưu thay đổi và cho phép Admin chọn lại phim.

Khi thành công, suất chiếu được gán phim mới và khoảng thời gian sử dụng phòng được cập nhật hợp lệ. Nếu thao tác bị hủy, bị từ chối hoặc thất bại, thông tin suất chiếu được giữ nguyên.

### UC04.2.2 · Thay đổi phòng chiếu

**Admin** · `/admin/showtimes`

1. Admin truy cập chức năng “Quản lý lịch chiếu”.
2. Admin chọn suất chiếu cần thay đổi phòng và chọn “Thay đổi phòng chiếu”.
3. Hệ thống kiểm tra suất chiếu chưa bắt đầu, chưa bị hủy và chưa phát sinh vé bán ra.
4. Hệ thống hiển thị thông tin phòng hiện tại và danh sách phòng chiếu.
5. Admin chọn phòng thay thế và chọn “Lưu”.
6. Hệ thống kiểm tra phòng mới đang hoạt động, có sơ đồ ghế hợp lệ và phù hợp với định dạng chiếu của suất chiếu.
7. Hệ thống kiểm tra lịch trùng tại phòng mới theo ngày, giờ và thời lượng của suất chiếu.
8. Hệ thống lưu phòng mới và thiết lập lại danh sách ghế của suất chiếu theo sơ đồ phòng mới.
9. Hệ thống thông báo “Thay đổi phòng chiếu thành công”.

3.1. Suất chiếu đã bắt đầu, đã bị hủy hoặc đã phát sinh vé bán ra: Hệ thống từ chối thay đổi phòng.
5.1. Admin chọn “Hủy”: Hệ thống giữ nguyên phòng hiện tại.
6.1. Phòng mới không đáp ứng điều kiện: Hệ thống thông báo lý do và yêu cầu chọn phòng khác.
7.1. Phòng mới có lịch chiếu bị trùng: Hệ thống thông báo xung đột và không lưu thay đổi.

Khi thành công, suất chiếu được chuyển sang phòng mới, danh sách ghế được cập nhật tương ứng và không xung đột lịch phòng. Nếu thao tác không thành công, dữ liệu suất chiếu được giữ nguyên.

### UC04.2.3 · Đổi ngày chiếu

**Admin** · `/admin/showtimes`

1. Admin truy cập chức năng “Quản lý lịch chiếu”.
2. Admin chọn suất chiếu cần thay đổi ngày và chọn “Đổi ngày chiếu”.
3. Hệ thống kiểm tra suất chiếu chưa bắt đầu, chưa bị hủy và chưa phát sinh vé bán ra.
4. Hệ thống hiển thị ngày chiếu hiện tại.
5. Admin chọn ngày mới và chọn “Lưu”.
6. Hệ thống kiểm tra ngày mới kết hợp với giờ bắt đầu hiện tại tạo thành thời điểm chiếu hợp lệ, chưa qua.
7. Hệ thống kiểm tra lịch trùng của phòng tại ngày mới.
8. Hệ thống lưu thay đổi, thông báo “Đổi ngày chiếu thành công” và cập nhật danh sách suất chiếu.

3.1. Suất chiếu đã bắt đầu, đã bị hủy hoặc đã phát sinh vé bán ra: Hệ thống từ chối đổi ngày chiếu.
5.1. Admin chọn “Hủy”: Hệ thống giữ nguyên ngày chiếu hiện tại.
6.1. Ngày chiếu không hợp lệ hoặc thời điểm chiếu mới đã qua: Hệ thống yêu cầu chọn lại ngày.
7.1. Lịch chiếu tại ngày mới bị trùng: Hệ thống thông báo xung đột và không lưu thay đổi.

Khi thành công, ngày chiếu được cập nhật hợp lệ và không xung đột lịch phòng. Nếu thao tác không thành công, ngày chiếu được giữ nguyên.

### UC04.2.4 · Đổi giờ chiếu

**Admin** · `/admin/showtimes`

1. Admin truy cập chức năng “Quản lý lịch chiếu”.
2. Admin chọn suất chiếu cần thay đổi giờ và chọn “Đổi giờ chiếu”.
3. Hệ thống kiểm tra suất chiếu chưa bắt đầu, chưa bị hủy và chưa phát sinh vé bán ra.
4. Hệ thống hiển thị giờ bắt đầu hiện tại.
5. Admin nhập giờ mới và chọn “Lưu”.
6. Hệ thống kiểm tra giờ mới kết hợp với ngày chiếu hiện tại tạo thành thời điểm chiếu hợp lệ, chưa qua.
7. Hệ thống xác định lại khoảng thời gian sử dụng phòng và kiểm tra lịch trùng.
8. Hệ thống lưu thay đổi, thông báo “Đổi giờ chiếu thành công” và cập nhật danh sách suất chiếu.

3.1. Suất chiếu đã bắt đầu, đã bị hủy hoặc đã phát sinh vé bán ra: Hệ thống từ chối đổi giờ chiếu.
5.1. Admin chọn “Hủy”: Hệ thống giữ nguyên giờ chiếu hiện tại.
6.1. Giờ nhập không hợp lệ hoặc thời điểm chiếu mới đã qua: Hệ thống yêu cầu nhập lại giờ.
7.1. Giờ mới gây trùng lịch phòng: Hệ thống thông báo xung đột và không lưu thay đổi.

Khi thành công, giờ bắt đầu và khoảng thời gian sử dụng phòng được cập nhật hợp lệ. Nếu thao tác không thành công, thông tin giờ chiếu được giữ nguyên.

### UC04.3 · Kiểm tra lịch trùng

**Admin, thông qua thao tác tạo hoặc cập nhật suất chiếu.** · `/admin/showtimes`

1. Admin thực hiện tạo hoặc cập nhật suất chiếu và chọn lưu thông tin.
2. Hệ thống lấy phòng chiếu, ngày chiếu, giờ bắt đầu, thời lượng phim và thời gian dọn phòng.
3. Hệ thống xác định khoảng thời gian sử dụng phòng của suất chiếu cần kiểm tra.
4. Hệ thống lấy các suất chiếu chưa bị hủy trong cùng phòng có khả năng giao nhau về thời gian; nếu đang cập nhật, không so sánh suất chiếu với chính nó.
5. Hệ thống so sánh các khoảng thời gian sử dụng phòng.
6. Nếu không có khoảng thời gian giao nhau, hệ thống trả kết quả không trùng lịch để tiếp tục lưu suất chiếu.

5.1. Phát hiện khoảng thời gian giao nhau: Hệ thống thông báo lịch chiếu bị trùng, không cho phép lưu và yêu cầu Admin điều chỉnh thông tin.

Hệ thống xác định suất chiếu có hoặc không có xung đột lịch phòng. Thao tác kiểm tra không tự làm thay đổi dữ liệu lịch chiếu.

### UC04.4 · Hủy suất chiếu

**Admin** · `/admin/showtimes`

1. Admin truy cập chức năng “Quản lý lịch chiếu”.
2. Hệ thống hiển thị danh sách suất chiếu.
3. Admin chọn suất chiếu cần hủy và chọn “Hủy suất chiếu”.
4. Hệ thống hiển thị thông báo yêu cầu xác nhận.
5. Admin chọn “Có”.
6. Hệ thống kiểm tra suất chiếu chưa bắt đầu, chưa bị hủy và chưa phát sinh vé bán ra.
7. Hệ thống chuyển trạng thái suất chiếu sang “Đã hủy” và không cho phép tiếp tục bán vé cho suất chiếu đó.
8. Hệ thống thông báo “Hủy suất chiếu thành công” và cập nhật danh sách suất chiếu.

5.1. Admin chọn “Không”: Hệ thống đóng thông báo và giữ nguyên suất chiếu.
6.1. Suất chiếu đã bắt đầu hoặc đã phát sinh vé bán ra: Hệ thống thông báo không được phép hủy và giữ nguyên dữ liệu.
6.2. Suất chiếu đã bị hủy: Hệ thống thông báo trạng thái hiện tại và không thực hiện hủy lần nữa.

Khi thành công, suất chiếu ở trạng thái “Đã hủy”, không được bán vé và không còn chiếm lịch sử dụng phòng. Thông tin suất chiếu vẫn được lưu để tra cứu. Nếu thao tác không thành công, dữ liệu được giữ nguyên.

### UC05.1.1 · Thêm ghế

**Admin** · `/catalog/rooms`

1. Admin chọn chức năng “Quản lý phòng chiếu”.
2. Hệ thống hiển thị danh sách phòng chiếu.
3. Admin chọn phòng cần bổ sung ghế và mở sơ đồ ghế.
4. Admin chọn “Thêm ghế”.
5. Hệ thống hiển thị biểu mẫu nhập thông tin ghế.
6. Admin nhập hàng ghế, số ghế, loại ghế và trạng thái sử dụng.
7. Admin chọn “Lưu”.
8. Hệ thống kiểm tra vị trí ghế không bị trùng và việc bổ sung không ảnh hưởng đến các suất chiếu đã bán vé, chưa kết thúc.
9. Hệ thống lưu ghế mới, cập nhật sơ đồ và tổng số ghế của phòng.
10. Hệ thống thông báo “Thêm ghế thành công”.

7.1. Admin chọn “Hủy”: Hệ thống không lưu thông tin và trở về sơ đồ ghế.
8.1. Thông tin chưa đầy đủ hoặc vị trí ghế bị trùng: Hệ thống thông báo lỗi, giữ thông tin đã nhập và yêu cầu điều chỉnh.
8.2. Phòng có suất chiếu đã bán vé, chưa kết thúc: Hệ thống từ chối bổ sung ghế và thông báo không được thay đổi kết cấu sơ đồ ghế lúc này.

Khi thành công, ghế mới được lưu, xuất hiện trên sơ đồ và tổng số ghế của phòng được cập nhật. Nếu thao tác không thành công, sơ đồ ghế được giữ nguyên.

### UC05.1.2 · Cập nhật thông tin ghế

**Admin** · `/catalog/rooms`

1. Admin truy cập chức năng “Quản lý phòng chiếu”.
2. Admin chọn phòng chiếu và mở sơ đồ ghế.
3. Admin chọn ghế cần cập nhật.
4. Hệ thống hiển thị thông tin hiện tại của ghế.
5. Admin chỉnh sửa hàng, số ghế hoặc trạng thái sử dụng thành “Sẵn sàng sử dụng” hay “Bảo trì”.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra dữ liệu, vị trí ghế và các suất chiếu liên quan.
8. Hệ thống lưu thay đổi, cập nhật sơ đồ ghế và thông báo “Cập nhật thông tin ghế thành công”.

6.1. Admin chọn “Hủy”: Hệ thống không lưu thay đổi.
7.1. Thông tin chưa đầy đủ hoặc vị trí mới trùng với ghế khác: Hệ thống thông báo lỗi và yêu cầu điều chỉnh.
7.2. Admin thay đổi hàng hoặc số ghế khi phòng có suất chiếu đã bán vé, chưa kết thúc: Hệ thống từ chối thay đổi để bảo toàn thông tin vị trí ghế trên vé.
7.3. Admin chuyển ghế sang “Bảo trì” nhưng ghế đã được bán trong suất chiếu chưa kết thúc: Hệ thống từ chối và thông báo ghế đang có vé liên quan.

Khi thành công, thông tin ghế được cập nhật. Ghế bảo trì không được chọn để bán vé. Nếu thao tác không thành công, dữ liệu ghế được giữ nguyên. Việc cập nhật không làm thay đổi thông tin vé đã phát hành.

### UC05.1.3 · Phân loại ghế

**Admin** · `/catalog/rooms`

1. Admin truy cập chức năng “Quản lý phòng chiếu”.
2. Admin chọn phòng chiếu và mở sơ đồ ghế.
3. Admin chọn ghế cần phân loại.
4. Hệ thống hiển thị loại ghế hiện tại và các loại ghế được hỗ trợ.
5. Admin chọn loại ghế phù hợp như ghế thường, ghế VIP hoặc ghế đôi.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra loại ghế và các suất chiếu liên quan.
8. Hệ thống lưu loại ghế mới, cập nhật sơ đồ và thông báo “Phân loại ghế thành công”.

6.1. Admin chọn “Hủy”: Hệ thống giữ nguyên loại ghế hiện tại.
7.1. Admin chưa chọn loại ghế hoặc loại ghế không hợp lệ: Hệ thống yêu cầu chọn lại.
7.2. Phòng có suất chiếu đã bán vé, chưa kết thúc: Hệ thống từ chối thay đổi loại ghế để tránh ảnh hưởng đến thông tin ghế và giá vé đã áp dụng.

Khi thành công, loại ghế được cập nhật và sử dụng để xác định giá cho các giao dịch mới. Nếu thao tác không thành công, loại ghế được giữ nguyên. Giá trị giao dịch đã hoàn tất không bị thay đổi.

### UC05.2 · Thêm phòng chiếu

**Admin** · `/catalog/rooms`

1. Admin chọn chức năng “Quản lý phòng chiếu”.
2. Hệ thống hiển thị danh sách phòng chiếu.
3. Admin chọn “Thêm phòng chiếu”.
4. Hệ thống hiển thị biểu mẫu thêm phòng.
5. Admin nhập tên phòng, chọn loại phòng chiếu và trạng thái hoạt động.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra thông tin và tên phòng không trùng với phòng đã có.
8. Hệ thống lưu phòng mới, thông báo “Thêm phòng chiếu thành công” và cập nhật danh sách phòng.

6.1. Admin chọn “Hủy”: Hệ thống không lưu thông tin và trở về danh sách phòng.
7.1. Thông tin bắt buộc bị bỏ trống hoặc dữ liệu không hợp lệ: Hệ thống thông báo các trường cần điều chỉnh và giữ thông tin đã nhập.
7.2. Tên phòng bị trùng: Hệ thống thông báo “Tên phòng chiếu đã tồn tại” và yêu cầu nhập tên khác.

Khi thành công, phòng mới được lưu và xuất hiện trong danh sách phòng chiếu. Phòng chỉ được sử dụng để tạo suất chiếu sau khi có sơ đồ ghế hợp lệ và đang hoạt động. Nếu thao tác không thành công, không tạo phòng mới.

### UC05.3 · Tra cứu phòng chiếu

**Admin, Nhân viên** · `/catalog/rooms`

1. Người dùng truy cập chức năng tra cứu phòng chiếu.
2. Hệ thống hiển thị danh sách phòng và các tiêu chí tìm kiếm.
3. Người dùng nhập mã hoặc tên phòng, hoặc chọn loại phòng và trạng thái hoạt động.
4. Người dùng chọn “Tìm kiếm”.
5. Hệ thống hiển thị các phòng phù hợp.
6. Người dùng chọn phòng cần xem.
7. Hệ thống hiển thị thông tin phòng, tổng số ghế, sơ đồ ghế và trạng thái hoạt động.

4.1. Người dùng không nhập hoặc chọn tiêu chí: Hệ thống hiển thị toàn bộ danh sách phòng được phép tra cứu.
5.1. Không có phòng phù hợp: Hệ thống hiển thị danh sách kết quả trống và thông báo “Không tìm thấy phòng chiếu phù hợp”.

Thông tin phòng chiếu được hiển thị theo kết quả tra cứu. Thao tác không làm thay đổi dữ liệu phòng hoặc ghế.

### UC05.4 · Cập nhật thông tin phòng chiếu

**Admin** · `/catalog/rooms`

1. Admin truy cập chức năng “Quản lý phòng chiếu”.
2. Admin chọn phòng cần sửa và chọn “Cập nhật thông tin phòng chiếu”.
3. Hệ thống hiển thị thông tin hiện tại của phòng.
4. Admin chỉnh sửa tên hoặc loại phòng chiếu.
5. Admin chọn “Lưu”.
6. Hệ thống kiểm tra dữ liệu và các suất chiếu liên quan.
7. Hệ thống lưu thay đổi, thông báo “Cập nhật phòng chiếu thành công” và cập nhật danh sách phòng.

5.1. Admin chọn “Hủy”: Hệ thống giữ nguyên thông tin phòng.
6.1. Thông tin bắt buộc bị bỏ trống, loại phòng không hợp lệ hoặc tên phòng trùng với phòng khác: Hệ thống thông báo lỗi và yêu cầu điều chỉnh.
6.2. Admin thay đổi loại phòng khi còn suất chiếu chưa bị hủy, chưa kết thúc: Hệ thống từ chối thay đổi loại phòng để tránh làm sai lệch thông tin suất chiếu.

Khi thành công, thông tin phòng được cập nhật. Sơ đồ ghế và tổng số ghế không thay đổi bởi thao tác này. Nếu thao tác không thành công, thông tin phòng được giữ nguyên.

### UC05.5 · Cập nhật trạng thái phòng chiếu

**Admin** · `/catalog/rooms`

1. Admin truy cập chức năng “Quản lý phòng chiếu”.
2. Admin chọn phòng cần thay đổi trạng thái.
3. Hệ thống hiển thị trạng thái hiện tại của phòng.
4. Admin chọn trạng thái mới và chọn “Lưu”.
5. Hệ thống kiểm tra các điều kiện liên quan đến trạng thái được chọn.
6. Hệ thống lưu thay đổi, thông báo “Cập nhật trạng thái phòng thành công” và cập nhật danh sách phòng.

4.1. Admin chọn “Hủy”: Hệ thống giữ nguyên trạng thái phòng.
5.1. Admin chọn “Tạm ngừng hoạt động” hoặc “Bảo trì” nhưng phòng còn suất chiếu chưa bị hủy, chưa kết thúc: Hệ thống từ chối và yêu cầu xử lý các suất chiếu liên quan trước.
5.2. Admin chọn “Đang hoạt động” nhưng phòng chưa có ghế sử dụng được: Hệ thống thông báo phòng chưa đủ điều kiện hoạt động và không lưu thay đổi.

Khi thành công, trạng thái phòng được cập nhật. Phòng tạm ngừng hoặc bảo trì không được sử dụng để tạo suất chiếu mới. Nếu thao tác không thành công, trạng thái phòng được giữ nguyên.

### UC06.1.1 · Thiết lập giá vé

**Admin** · `/admin/prices`

1. Admin chọn chức năng “Quản lý giá vé và F&B”.
2. Hệ thống hiển thị bảng giá vé hiện tại.
3. Admin chọn “Thiết lập giá vé”.
4. Hệ thống hiển thị biểu mẫu thiết lập giá.
5. Admin chọn loại ghế, loại phòng chiếu, thời điểm áp dụng như ngày thường, cuối tuần hoặc ngày lễ và nhập mức giá vé.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra mức giá và các điều kiện áp dụng.
8. Hệ thống lưu mức giá mới, thông báo “Thiết lập giá vé thành công” và cập nhật bảng giá.

6.1. Admin chọn “Hủy”: Hệ thống không lưu thông tin và trở về bảng giá vé.
7.1. Thông tin chưa đầy đủ hoặc mức giá không phải số lớn hơn 0: Hệ thống thông báo lỗi, giữ thông tin đã nhập và yêu cầu điều chỉnh.
7.2. Đã tồn tại mức giá có cùng điều kiện áp dụng: Hệ thống thông báo “Giá vé cho điều kiện này đã tồn tại” và yêu cầu Admin sử dụng chức năng Sửa giá vé.

Khi thành công, mức giá mới được lưu và có thể sử dụng để tính tiền cho các giao dịch bán vé phù hợp. Nếu thao tác không thành công, bảng giá được giữ nguyên.

### UC06.1.2 · Sửa giá vé

**Admin** · `/admin/prices`

1. Admin truy cập chức năng “Quản lý giá vé và F&B”.
2. Hệ thống hiển thị bảng giá vé.
3. Admin chọn mức giá cần sửa và chọn “Sửa giá vé”.
4. Hệ thống hiển thị mức giá hiện tại cùng các điều kiện áp dụng.
5. Admin nhập mức giá mới và chọn “Lưu”.
6. Hệ thống kiểm tra mức giá mới hợp lệ.
7. Hệ thống lưu thay đổi, thông báo “Cập nhật giá vé thành công” và cập nhật bảng giá.

5.1. Admin chọn “Hủy”: Hệ thống không lưu thay đổi và giữ nguyên mức giá hiện tại.
6.1. Mức giá bị bỏ trống hoặc không phải số lớn hơn 0: Hệ thống thông báo “Vui lòng nhập giá vé hợp lệ” và yêu cầu nhập lại.

Khi thành công, mức giá mới được áp dụng cho các giao dịch bán vé phát sinh sau khi cập nhật. Giá vé và tổng tiền của các giao dịch đã hoàn tất được giữ nguyên. Nếu thao tác không thành công, bảng giá không thay đổi.

### UC06.2.1 · Thêm sản phẩm

**Admin** · `/admin/products`

1. Admin truy cập chức năng “Quản lý giá vé và F&B”.
2. Admin chọn mục “Quản lý F&B”.
3. Hệ thống hiển thị danh sách sản phẩm.
4. Admin chọn “Thêm sản phẩm”.
5. Hệ thống hiển thị biểu mẫu thêm sản phẩm.
6. Admin nhập mã sản phẩm, tên sản phẩm, mô tả, đơn vị tính, đơn giá, số lượng tồn kho ban đầu và tình trạng kinh doanh.
7. Admin chọn “Lưu”.
8. Hệ thống kiểm tra thông tin sản phẩm.
9. Hệ thống lưu sản phẩm mới, thông báo “Thêm sản phẩm thành công” và cập nhật danh sách sản phẩm.

7.1. Admin chọn “Hủy”: Hệ thống không lưu thông tin và trở về danh sách sản phẩm.
8.1. Thông tin bắt buộc bị bỏ trống: Hệ thống thông báo các trường cần bổ sung và giữ thông tin đã nhập.
8.2. Mã sản phẩm đã tồn tại: Hệ thống thông báo mã bị trùng và yêu cầu nhập mã khác.
8.3. Đơn giá không phải số lớn hơn 0 hoặc số lượng tồn kho không phải số nguyên không âm: Hệ thống thông báo dữ liệu không hợp lệ và yêu cầu điều chỉnh.

Khi thành công, sản phẩm mới được lưu cùng giá bán và số lượng tồn kho ban đầu, đồng thời xuất hiện trong danh mục F&B. Nếu thao tác không thành công, không tạo sản phẩm mới.

### UC06.2.2 · Cập nhật sản phẩm

**Admin** · `/admin/products`

1. Admin truy cập mục “Quản lý F&B”.
2. Hệ thống hiển thị danh sách sản phẩm.
3. Admin chọn sản phẩm cần sửa và chọn “Cập nhật sản phẩm”.
4. Hệ thống hiển thị thông tin hiện tại của sản phẩm.
5. Admin chỉnh sửa tên, mô tả, đơn vị tính, tình trạng kinh doanh hoặc cập nhật số lượng tồn kho theo số lượng thực tế đã kiểm tra.
6. Admin chọn “Lưu”.
7. Hệ thống kiểm tra thông tin đã chỉnh sửa.
8. Hệ thống lưu thay đổi, thông báo “Cập nhật sản phẩm thành công” và cập nhật danh sách sản phẩm.

6.1. Admin chọn “Hủy”: Hệ thống không lưu thay đổi và giữ nguyên thông tin sản phẩm.
7.1. Thông tin bắt buộc bị bỏ trống hoặc dữ liệu không hợp lệ: Hệ thống thông báo lỗi, giữ nội dung chỉnh sửa và yêu cầu điều chỉnh.
7.2. Số lượng tồn kho không phải số nguyên không âm: Hệ thống thông báo “Số lượng tồn kho không hợp lệ”, giữ nội dung đã nhập và yêu cầu điều chỉnh.

Khi thành công, các thông tin được chỉnh sửa và số lượng tồn kho được lưu, sử dụng cho chức năng kiểm tra tồn kho và thống kê F&B. Mã sản phẩm và giá bán không thay đổi bởi thao tác này. Nếu thao tác không thành công, dữ liệu sản phẩm được giữ nguyên.

### UC06.2.3 · Quản lý giá bán

**Admin** · `/admin/products`

1. Admin truy cập mục “Quản lý F&B”.
2. Admin chọn chức năng “Quản lý giá bán”.
3. Hệ thống hiển thị danh sách sản phẩm cùng giá bán hiện tại.
4. Admin chọn sản phẩm cần điều chỉnh giá.
5. Hệ thống hiển thị thông tin sản phẩm và ô nhập giá bán.
6. Admin nhập giá mới và chọn “Lưu”.
7. Hệ thống kiểm tra giá bán hợp lệ.
8. Hệ thống lưu giá mới, thông báo “Cập nhật giá bán thành công” và cập nhật danh sách giá sản phẩm.

6.1. Admin chọn “Hủy”: Hệ thống giữ nguyên giá bán hiện tại.
7.1. Giá bán bị bỏ trống hoặc không phải số lớn hơn 0: Hệ thống thông báo “Vui lòng nhập giá bán hợp lệ” và yêu cầu nhập lại.

Khi thành công, giá bán của sản phẩm được cập nhật. Các thông tin khác và số lượng tồn kho được giữ nguyên. Nếu thao tác không thành công, giá bán không thay đổi.

### UC06.2.4 · Kiểm tra tồn kho

**Admin** · `/admin/products`

1. Admin truy cập mục “Quản lý F&B”.
2. Admin chọn “Kiểm tra tồn kho”.
3. Hệ thống hiển thị danh sách sản phẩm cùng mã sản phẩm, tên, đơn vị tính và số lượng tồn kho.
4. Admin nhập mã hoặc tên sản phẩm cần xem, hoặc chọn trạng thái còn hàng, hết hàng để lọc danh sách.
5. Hệ thống hiển thị các sản phẩm phù hợp cùng số lượng tồn kho hiện tại.

4.1. Admin không nhập hoặc chọn tiêu chí: Hệ thống hiển thị tồn kho của toàn bộ sản phẩm.
5.1. Không có sản phẩm phù hợp: Hệ thống hiển thị danh sách kết quả trống và thông báo “Không tìm thấy sản phẩm phù hợp”.

Hệ thống hiển thị số lượng tồn kho theo dữ liệu đang lưu. Sản phẩm có số lượng bằng 0 được xác định là hết hàng. Thao tác kiểm tra không làm thay đổi số lượng tồn kho.

### UC07.1 · Tra cứu vé

**Nhân viên** · `/staff/admission`

1. Nhân viên chọn chức năng “Kiểm tra và xác nhận vé”.
2. Hệ thống hiển thị giao diện tra cứu vé.
3. Nhân viên quét mã QR hoặc nhập mã vé.
4. Hệ thống tìm vé tương ứng trong cơ sở dữ liệu.
5. Hệ thống hiển thị thông tin gồm mã vé, tên phim, phòng chiếu, ngày giờ chiếu, vị trí ghế và trạng thái vé.

3.1. Mã QR không đọc được: Hệ thống thông báo lỗi và cho phép Nhân viên quét lại hoặc nhập mã vé thủ công.
3.2. Nhân viên chưa nhập mã vé: Hệ thống yêu cầu nhập mã vé hoặc quét mã QR.
4.1. Không tìm thấy vé: Hệ thống thông báo “Vé không tồn tại” và cho phép tra cứu lại.

Khi thành công, thông tin vé được hiển thị. Thao tác tra cứu không làm thay đổi trạng thái vé.

### UC07.2 · Kiểm tra tính hợp lệ

**Nhân viên** · `/staff/admission`

1. Nhân viên tra cứu vé bằng mã vé hoặc mã QR.
2. Hệ thống tìm và hiển thị thông tin vé.
3. Hệ thống kiểm tra vé đã thanh toán và chưa sử dụng.
4. Hệ thống kiểm tra vé thuộc đúng phòng và suất chiếu đang được kiểm soát, đồng thời thời điểm kiểm tra nằm trong khoảng thời gian cho phép vào phòng.
5. Hệ thống hiển thị kết quả “Vé hợp lệ”.
6. Nhân viên đối chiếu thông tin và chọn “Xác nhận” để thực hiện cập nhật trạng thái vé.

2.1. Vé không tồn tại: Hệ thống thông báo “Vé không tồn tại” và không cho phép xác nhận.
3.1. Vé chưa thanh toán hoặc đã sử dụng: Hệ thống thông báo vé không hợp lệ kèm lý do và không cho phép xác nhận.
4.1. Vé không đúng phòng, không đúng suất chiếu hoặc ngoài thời gian được phép vào: Hệ thống thông báo lý do từ chối và giữ nguyên trạng thái vé.
6.1. Nhân viên không xác nhận cho khách vào: Hệ thống giữ nguyên trạng thái vé.

Hệ thống hiển thị kết quả kiểm tra. Nếu vé hợp lệ và Nhân viên xác nhận, hệ thống thực hiện cập nhật trạng thái. Nếu vé không hợp lệ, Nhân viên chưa xác nhận hoặc kiểm tra thất bại, trạng thái vé được giữ nguyên.

### UC07.3 · Cập nhật trạng thái

**Nhân viên** · `/staff/admission`

1. Hệ thống hiển thị kết quả kiểm tra vé hợp lệ.
2. Nhân viên chọn “Xác nhận” cho khách vào phòng chiếu.
3. Hệ thống kiểm tra lại vé vẫn đủ điều kiện sử dụng tại thời điểm xác nhận.
4. Hệ thống cập nhật trạng thái vé sang “Đã sử dụng”.
5. Hệ thống thông báo “Xác nhận vé thành công” và hiển thị trạng thái mới.
6. Nhân viên cho khách vào phòng chiếu.

2.1. Nhân viên hủy xác nhận: Hệ thống giữ nguyên trạng thái vé.
3.1. Vé đã được xác nhận sử dụng tại lần kiểm tra khác: Hệ thống thông báo “Vé đã được sử dụng” và không thực hiện cập nhật lần nữa.
3.2. Vé không còn đủ điều kiện sử dụng: Hệ thống thông báo lý do từ chối và không cập nhật trạng thái.
4.1. Nhân viên chưa cho khách vào cho đến khi hệ thống xác nhận thành công.

Khi thành công, vé chuyển sang “Đã sử dụng” và không được xác nhận sử dụng lần nữa. Nếu thao tác bị hủy hoặc cập nhật thất bại, hệ thống không ghi nhận thêm lần sử dụng vé.

### UC08.1 · Tìm suất chiếu

**Nhân viên** · `/staff/sales`

1. Nhân viên chọn chức năng “Quản lý bán vé”.
2. Hệ thống hiển thị danh sách suất chiếu đang được phép bán vé và các tiêu chí tìm kiếm.
3. Nhân viên nhập tên phim hoặc chọn ngày, giờ chiếu cần tìm.
4. Nhân viên chọn “Tìm kiếm”.
5. Hệ thống hiển thị các suất chiếu phù hợp, gồm tên phim, phòng chiếu, ngày và giờ bắt đầu.
6. Nhân viên chọn suất chiếu theo yêu cầu của khách để tiếp tục kiểm tra ghế trống.

4.1. Nhân viên không nhập hoặc chọn tiêu chí: Hệ thống hiển thị toàn bộ suất chiếu đang được phép bán vé.
5.1. Không có suất chiếu phù hợp: Hệ thống thông báo “Không tìm thấy suất chiếu phù hợp” và cho phép thay đổi tiêu chí.
6.1. Suất chiếu được chọn không còn được phép bán vé: Hệ thống thông báo và yêu cầu chọn suất chiếu khác.

Hệ thống hiển thị kết quả tìm kiếm. Nếu Nhân viên chọn một suất chiếu hợp lệ, hệ thống sử dụng suất chiếu đó cho các thao tác bán vé tiếp theo.

### UC08.2 · Kiểm tra ghế trống

**Nhân viên** · `/staff/sales`

1. Nhân viên chọn suất chiếu cần bán vé.
2. Hệ thống lấy sơ đồ ghế của phòng và tình trạng ghế theo suất chiếu đã chọn.
3. Hệ thống hiển thị các ghế còn trống, đã bán, đang được giữ hoặc không khả dụng.
4. Nhân viên chọn ghế còn trống theo yêu cầu của khách.
5. Hệ thống kiểm tra các ghế được chọn vẫn còn trống và sử dụng được.
6. Hệ thống giữ tạm các ghế cho giao dịch hiện tại và hiển thị danh sách ghế đã chọn.
7. Nhân viên xác nhận lựa chọn để tiếp tục tính tiền.

3.1. Không còn ghế trống: Hệ thống thông báo “Suất chiếu đã hết chỗ” và cho phép Nhân viên chọn suất chiếu khác.
5.1. Ghế đã được bán, được giữ bởi giao dịch khác hoặc không khả dụng: Hệ thống thông báo ghế không thể chọn, cập nhật sơ đồ và yêu cầu chọn lại.
7.1. Nhân viên hủy giao dịch hoặc đổi suất chiếu: Hệ thống giải phóng các ghế đang giữ cho giao dịch hiện tại.
7.2. Hết thời gian giữ ghế khi giao dịch chưa bắt đầu xử lý thanh toán: Hệ thống giải phóng ghế, thông báo và yêu cầu kiểm tra lại tình trạng ghế. Khi giao dịch đang xử lý thanh toán, hệ thống tiếp tục giữ ghế cho đến khi xác định kết quả; nếu kết quả chưa rõ, Nhân viên kiểm tra giao dịch trước khi tiếp tục xử lý.

Khi thành công, các ghế được chọn được giữ tạm cho giao dịch hiện tại, chưa chuyển sang “Đã bán”. Nếu giao dịch bị hủy trước khi thanh toán hoặc hết thời gian giữ khi chưa bắt đầu xử lý thanh toán, các ghế được giải phóng

### UC08.3 · Tính tiền

**Nhân viên** · `/staff/sales`

1. Nhân viên xác nhận danh sách ghế đã chọn.
2. Hệ thống kiểm tra các ghế vẫn được giữ hợp lệ cho giao dịch hiện tại.
3. Hệ thống xác định loại ghế, loại phòng và thời điểm chiếu để lấy mức giá tương ứng.
4. Hệ thống tính tiền từng vé và tổng tiền của các vé được chọn.
5. Hệ thống hiển thị danh sách ghế, giá từng vé và tổng tiền cần thanh toán.
6. Nhân viên thông báo tổng tiền cho khách.
7. Nếu khách đồng ý mua, Nhân viên chọn “Thanh toán” để tiếp tục giao dịch.

2.1. Chưa chọn ghế hoặc thời gian giữ ghế đã hết: Hệ thống yêu cầu Nhân viên kiểm tra và chọn lại ghế.
3.1. Chưa có giá vé phù hợp: Hệ thống thông báo “Chưa thiết lập giá vé cho lựa chọn này” và không cho phép thanh toán.
6.1. Khách muốn thay đổi ghế hoặc số lượng vé: Nhân viên quay lại lựa chọn ghế; hệ thống tính lại tiền theo lựa chọn mới.
6.2. Khách không đồng ý mua: Nhân viên hủy giao dịch; hệ thống giải phóng các ghế đang giữ.

Khi thành công, hệ thống hiển thị tổng tiền vé. Nếu khách đồng ý, giao dịch được chuyển sang bước thanh toán. Việc tính tiền chưa ghi nhận giao dịch thanh toán thành công.

### UC08.4 · Thanh toán

**Nhân viên** · `/staff/sales`

1. Sau khi tính tiền và khách đồng ý mua, Nhân viên chọn “Thanh toán”.
2. Hệ thống hiển thị thông tin suất chiếu, danh sách ghế và tổng tiền.
3. Hệ thống kiểm tra các ghế vẫn được giữ hợp lệ cho giao dịch. Nếu hợp lệ, hệ thống tiếp tục giữ ghế trong quá trình xử lý thanh toán.
4. Nhân viên chọn phương thức thanh toán: tiền mặt, chuyển khoản hoặc thẻ.
5. Nhân viên nhận tiền mặt hoặc kiểm tra kết quả thanh toán qua chuyển khoản, thẻ tại quầy.
6. Khi đã nhận đủ tiền hoặc xác nhận thanh toán thành công, Nhân viên chọn “Hoàn tất giao dịch”; hệ thống kiểm tra thông tin thanh toán đầy đủ.
7. Hệ thống lưu giao dịch thành công, gồm mã giao dịch, thời gian, Nhân viên thực hiện, phương thức thanh toán, các ghế và số tiền.
8. Hệ thống chuyển các ghế của giao dịch sang “Đã bán” và thông báo “Thanh toán thành công”.
9. Hệ thống tiếp tục thực hiện phát hành vé.

3.1. Ghế không còn được giữ hợp lệ: Hệ thống không cho phép bắt đầu thanh toán và yêu cầu Nhân viên kiểm tra, chọn lại ghế trước khi nhận tiền.
4.1. Khách hủy trước khi thanh toán hoặc thanh toán chưa thành công: Nhân viên chưa xác nhận hoàn tất; hệ thống chưa ghi nhận giao dịch thành công.
5.1. Khách hủy trước khi thanh toán: Nhân viên hủy giao dịch; hệ thống giải phóng các ghế đang giữ.
6.1. Thiếu thông tin thanh toán: Hệ thống yêu cầu bổ sung trước khi hoàn tất.
7.1. Nhân viên kiểm tra giao dịch trước khi thực hiện lại để tránh ghi nhận trùng.

Khi thành công, giao dịch được ghi nhận đã thanh toán, các ghế chuyển sang “Đã bán” và hệ thống thực hiện phát hành vé. Nếu chưa thanh toán thành công, hệ thống không phát hành vé.

### UC08.5 · Phát hành vé

**Nhân viên** · `/staff/sales`

1. Hệ thống xác định giao dịch đã thanh toán thành công.
2. Hệ thống lấy thông tin suất chiếu và các ghế đã bán trong giao dịch.
3. Hệ thống tạo vé tương ứng với từng ghế, gồm mã vé, mã QR, tên phim, phòng, ngày giờ chiếu, vị trí ghế và giá vé.
4. Hệ thống lưu vé với trạng thái chưa sử dụng và liên kết vé với giao dịch.
5. Hệ thống hiển thị vé để Nhân viên kiểm tra và thực hiện in.
6. Nhân viên kiểm tra vé đã in và giao vé cho khách.

1.1. Giao dịch chưa thanh toán thành công: Hệ thống không cho phép phát hành vé.
3.1. Giao dịch đã có vé được phát hành: Hệ thống sử dụng lại các vé đã lưu, không tạo thêm vé mới cho cùng ghế trong giao dịch.
4.1. Giao dịch đã thanh toán vẫn được giữ để kiểm tra và tiếp tục phát hành vé.
5.1. Nhân viên in lại vé đã lưu. Không tạo giao dịch mới hoặc thu tiền lần nữa.

Khi thành công, vé được lưu với trạng thái chưa sử dụng và được in để giao cho khách. Nếu chỉ việc in thất bại, dữ liệu vé và giao dịch vẫn được giữ nguyên.

### UC08.6 · Kiểm tra giao dịch

**Nhân viên** · `/staff/transactions`

1. Nhân viên chọn chức năng “Kiểm tra giao dịch”.
2. Hệ thống hiển thị giao diện tra cứu giao dịch.
3. Nhân viên nhập mã giao dịch, mã vé hoặc chọn khoảng thời gian cần tra cứu.
4. Nhân viên chọn “Tìm kiếm”.
5. Hệ thống tìm các giao dịch phù hợp và hiển thị danh sách kết quả.
6. Nhân viên chọn giao dịch cần xem.
7. Hệ thống hiển thị mã giao dịch, thời gian, Nhân viên thực hiện, phương thức thanh toán, tổng tiền, trạng thái giao dịch và các vé liên quan.

4.1. Nhân viên chưa nhập hoặc chọn tiêu chí: Hệ thống yêu cầu nhập thông tin tra cứu.
4.2. Khoảng thời gian không hợp lệ: Hệ thống yêu cầu chọn lại.
5.1. Không có giao dịch phù hợp: Hệ thống hiển thị danh sách kết quả trống và thông báo “Không tìm thấy giao dịch phù hợp”.

Hệ thống hiển thị thông tin giao dịch và các vé liên quan. Thao tác kiểm tra không làm thay đổi dữ liệu giao dịch, trạng thái ghế hoặc trạng thái vé.

### UC09.1 · Doanh thu theo thời gian

**Admin** · `/admin/reports`

1. Admin chọn chức năng “Báo cáo và thống kê”.
2. Admin chọn “Doanh thu theo thời gian”.
3. Hệ thống hiển thị các tiêu chí lựa chọn thời gian.
4. Admin chọn ngày, tháng, năm hoặc nhập khoảng thời gian cần xem.
5. Admin chọn “Thống kê”.
6. Hệ thống lấy các giao dịch bán vé đã thanh toán thành công có thời gian thanh toán thuộc phạm vi được chọn.
7. Hệ thống tổng hợp số giao dịch, số vé bán và tổng doanh thu theo thời gian.
8. Hệ thống hiển thị kết quả thống kê.

5.1. Admin chưa chọn thời gian: Hệ thống yêu cầu chọn thời gian cần thống kê.
5.2. Ngày bắt đầu lớn hơn ngày kết thúc: Hệ thống thông báo “Khoảng thời gian không hợp lệ” và yêu cầu chọn lại.
6.1. Không có giao dịch thành công trong thời gian được chọn: Hệ thống hiển thị số giao dịch, số vé bán và doanh thu bằng 0.

Hệ thống hiển thị doanh thu bán vé theo thời gian đã chọn. Thao tác thống kê không làm thay đổi dữ liệu giao dịch.

### UC09.2 · Doanh thu theo phim

**Admin** · `/admin/reports`

1. Admin truy cập chức năng “Báo cáo và thống kê”.
2. Admin chọn “Doanh thu theo phim”.
3. Hệ thống hiển thị danh sách phim và các trường chọn thời gian.
4. Admin chọn một phim hoặc tất cả phim, đồng thời chọn khoảng thời gian cần xem.
5. Admin chọn “Thống kê”.
6. Hệ thống lấy các vé thuộc giao dịch đã thanh toán thành công trong thời gian được chọn và xác định phim tương ứng với từng vé.
7. Hệ thống tổng hợp số lượng vé bán và doanh thu theo từng phim.
8. Hệ thống hiển thị tên phim, số lượng vé bán và tổng doanh thu tương ứng.

5.1. Admin chưa chọn thời gian hoặc khoảng thời gian không hợp lệ: Hệ thống thông báo và yêu cầu điều chỉnh.
6.1. Phim không có vé bán trong thời gian được chọn: Hệ thống hiển thị số lượng vé và doanh thu của phim bằng 0.

Hệ thống hiển thị doanh thu theo phim và thời gian được chọn. Doanh thu được tính từ giá vé đã lưu trong giao dịch, không tính lại theo bảng giá hiện tại. Dữ liệu gốc được giữ nguyên.

### UC09.3 · Doanh thu theo nhân viên

**Admin** · `/admin/reports`

1. Admin truy cập chức năng “Báo cáo và thống kê”.
2. Admin chọn “Doanh thu theo nhân viên”.
3. Hệ thống hiển thị danh sách nhân viên và các trường chọn thời gian.
4. Admin chọn một nhân viên hoặc tất cả nhân viên, đồng thời chọn khoảng thời gian cần xem.
5. Admin chọn “Thống kê”.
6. Hệ thống lấy các giao dịch đã thanh toán thành công trong thời gian được chọn, theo Nhân viên được ghi nhận thực hiện giao dịch.
7. Hệ thống tổng hợp số giao dịch, số vé bán và doanh thu theo từng Nhân viên.
8. Hệ thống hiển thị mã nhân viên, họ tên và các số liệu tương ứng.

5.1. Admin chưa chọn thời gian hoặc khoảng thời gian không hợp lệ: Hệ thống thông báo và yêu cầu điều chỉnh.
6.1. Nhân viên không có giao dịch thành công trong thời gian được chọn: Hệ thống hiển thị số giao dịch, số vé bán và doanh thu bằng 0.

Hệ thống hiển thị doanh thu theo Nhân viên và thời gian được chọn. Việc thống kê không làm thay đổi hồ sơ nhân viên hoặc dữ liệu giao dịch.

### UC09.4 · Thống kê theo vé và F&B

**Admin** · `/admin/reports`

1. Admin truy cập chức năng “Báo cáo và thống kê”.
2. Admin chọn “Thống kê theo vé và F&B”.
3. Hệ thống hiển thị hai lựa chọn: thống kê vé và thống kê F&B.
4. Admin chọn loại thống kê cần xem.
5. Nếu chọn thống kê vé, Admin chọn khoảng thời gian phát hành vé và có thể lọc theo phim hoặc suất chiếu.
6. Nếu chọn thống kê F&B, Admin chọn tất cả sản phẩm hoặc lọc theo sản phẩm, tình trạng kinh doanh, còn hàng hay hết hàng.
7. Admin chọn “Thống kê”.
8. Hệ thống tổng hợp dữ liệu theo lựa chọn.
9. Với thống kê vé, hệ thống hiển thị tổng số vé đã phát hành trong thời gian được chọn và số lượng theo trạng thái hiện tại: chưa sử dụng hoặc đã sử dụng.
10. Với thống kê F&B, hệ thống hiển thị mã sản phẩm, tên sản phẩm, đơn vị tính, tình trạng kinh doanh và số lượng tồn kho hiện tại.

7.1. Chưa chọn loại thống kê hoặc thiếu tiêu chí bắt buộc: Hệ thống yêu cầu bổ sung lựa chọn.
7.2. Khoảng thời gian thống kê vé không hợp lệ: Hệ thống thông báo và yêu cầu chọn lại.
8.1. Không có vé phù hợp: Hệ thống hiển thị các số lượng vé bằng 0.
8.2. Không có sản phẩm phù hợp: Hệ thống hiển thị danh sách kết quả trống và thông báo “Không có sản phẩm phù hợp”.

Hệ thống hiển thị kết quả thống kê theo lựa chọn. F&B chỉ thống kê thông tin sản phẩm và tồn kho hiện tại, không thống kê doanh thu hoặc số lượng bán. Dữ liệu vé và sản phẩm không bị thay đổi.
