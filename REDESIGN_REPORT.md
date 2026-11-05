# Báo cáo redesign Java Swing

Ngày kiểm chứng: 05/11/2026. Nhánh: ui-redesign. Không push lên GitHub.

## 1. Hướng thiết kế

Cinema Workspace dùng nền xanh than, bề mặt phân lớp, điểm nhấn tím iris; sáng/tối giữ cùng cấu trúc. Chọn hướng này để tập trung vào thao tác tại quầy, phân biệt trạng thái và đọc bảng nhanh. Học phân vùng, thẻ, sidebar và mật độ từ đủ 8 ảnh tham khảo; không sao chép nội dung, logo hoặc tài sản ảnh. Đối chiếu SHA-256 cuối kỳ: cả 8 ảnh không đổi; thư mục Reference_UIUX không được add vào Git.

Theme tập trung palette, font Segoe UI/Consolas, 6 cỡ chữ, thang spacing và kích thước. FlatLaf được tinh chỉnh bằng token; icon, ghế và biểu đồ vẽ vector/Graphics2D. Không có hiệu ứng kính hoặc chuyển động nặng. Các cửa sổ chức năng vẫn giữ vòng đời cũ để bảo toàn giao dịch đang xử lý.

## 2. Các màn đã thay giao diện

- Chung: đăng nhập; khung điều hướng theo vai trò, thông tin người dùng, đăng xuất và nút sáng/tối.
- Admin: danh sách/chi tiết/form nhân viên; thư viện phim có poster và fallback; lịch chiếu và cảnh báo giờ trả phòng; phòng/sơ đồ/danh sách/form ghế; giá vé, sản phẩm, giá bán và tồn kho F&B; báo cáo thời gian, phim, nhân viên, vé và tồn kho F&B.
- Nhân viên: hồ sơ cá nhân, tra cứu phim/phòng, sơ đồ phòng chỉ đọc; soát vé theo mã/QR; bán vé tại quầy với bảng suất, ghế, tóm tắt, thanh toán và vé QR; lịch sử/chi tiết/tiếp tục/in lại giao dịch.

Không đưa F&B vào bán vé. Không thêm CSV vì chức năng cũ không có. Không suy diễn doanh thu F&B từ tồn kho. Không vẽ xu hướng từ dữ liệu tổng.

## 3. Nghiệm thu từng sprint

Các chữ “đạt” dưới đây áp dụng cho bằng chứng được liệt kê, không thay cho nghiệm thu bằng thiết bị thực tế.

1. **Nền tảng — đạt trong môi trường kiểm thử.** Theme sáng/tối lúc chạy, bảo toàn model/chọn dòng/ô nhập; AA hai palette; menu đúng vai trò, Ctrl+K/Enter. DesignSystemTest và các test desktop; ảnh foundation và sprint7.
2. **Bán vé — đạt trên CSDL kiểm thử; in vật lý chưa kiểm chứng.** SalesMySQLTest 18 ca, SalesDesktopMySQLTest 2 ca, InVeTest 2 ca và SalesPresentationTest 3 ca. Đã thực hiện giữ/tính tiền/thanh toán/phát hành, phục hồi mã, chống bán trùng, lịch sử và QR preview. Các nút cũ và timer 5000ms được giữ; không thêm DB trên EDT. Ảnh uc08-sales/preview/ticket.
3. **Soát vé/tra cứu — đạt trong môi trường kiểm thử.** Admission và GatePresentationTest kiểm tra hợp lệ/đã dùng/sai suất/chưa thanh toán; xác nhận lần hai bị từ chối. Các test hồ sơ/phim/phòng và kiểm tra menu Nhân viên. Ảnh uc07, uc02-staff, sprint7/staff_demo-*.
4. **Phim/phòng/lịch — đạt trong môi trường kiểm thử.** Test MySQL và desktop CRUD, phân quyền, ràng buộc hủy/sửa suất và ghế. SchedulePreviewTest kiểm tra dọn phòng và biên thời gian liền kề; preview chỉ gợi ý từ các hàng đang tải, BUS vẫn kiểm tra cuối cùng. Ảnh uc03/04/05, các form sprint7.
5. **Giá/F&B/nhân viên/đăng nhập — đạt trong môi trường kiểm thử.** Pricing, staff và đăng nhập giữ dữ liệu lịch sử, xác nhận thao tác nguy hiểm, trạng thái khóa/sai thông tin; form có hướng dẫn/lỗi tại trường và cảnh báo hết tồn. Ảnh uc06 và sprint5/login-dark/light.
6. **Báo cáo — đạt trong môi trường kiểm thử.** 23 kiểm thử liên quan ở sprint, kiểm thử desktop chạy đủ loại; snapshot dữ liệu trước/sau không đổi, Nhân viên bị từ chối. KPI/chart lấy từ model bảng hiện có. Ảnh uc09-time/film/staff/tickets/stock.
7. **Đồng nhất — đạt một phần.** Đã mở thật 2 trang chủ và 11 cửa sổ chức năng ở 1366×768, đổi sáng/tối, kiểm tra model và chụp ảnh; mở form nhân viên/phim/tạo lịch trong hai theme. Có kiểm thử palette, tablet, date/time picker và scale 150%. Chưa duyệt tay hết mọi nhánh hộp thoại bằng chuột/Tab, chưa kiểm tra nhiều màn hình có DPI khác nhau. Không tuyên bố đã nghiệm thu đầy đủ các trường hợp đó.

### Kết quả kiểm thử cuối

Đã chạy bằng POM gốc: mvn clean test -Dcinema.mysql.tests=true — 161/161 đạt, 0 lỗi, 0 bỏ qua (02:26 phút). Sau đó chạy lại 5/5 kiểm thử cửa sổ/date picker/design system ở FlatLaf scale 150% đạt. Không còn cảnh báo RowSorter trong lượt cuối. Log cục bộ: work/redesign-final-all.log và work/redesign-s7-dpi-final.log trong workspace làm việc.

Kiểm thử mở JFrame/JDialog thật trên Windows; ảnh bằng Window.paint/BufferedImage từ các cửa sổ này. Viền/titlebar do Windows vẽ không nằm trong ảnh client Swing. Không dùng ảnh mockup để coi là bằng chứng chạy ứng dụng.

## 4. Bảo toàn nghiệp vụ và thay đổi điều phối UI

Git đối chiếu với cfd13ba: không sửa controller/BUS, DAO, model, db, schema/SQL, cấu hình kết nối, scripts hoặc các test gốc. Chỉ thêm dependency FlatLaf, lớp trình bày, test trình bày và tài liệu. Không có thay đổi logic nghiệp vụ.

Những thay đổi điều phối ở view cần biết:

- Shell gọi lại callback mở màn cũ; đăng xuất hỏi xác nhận và tôn trọng requestClose của bán vé.
- SalesViews đồng bộ bảng trình bày với JComboBox/JList/JTextArea cũ, giữ nguồn binding nội bộ; không thêm truy vấn.
- BanVePanel giữ cart/id/selected/showId/busy/paymentOpen/needsCheck, run/SwingWorker, canLeave/requestClose/recover, timer và thứ tự nghiệp vụ. Chỉ bố cục, renderer, Enter tìm kiếm và ngày giờ thay đổi.
- DateInputs ghi định dạng văn bản cũ về ô nhập; parser cuối vẫn LocalDate/LocalTime/YearMonth như trước, giữ trường hợp để trống.
- Forms chỉ hướng dẫn/đánh dấu cú pháp tại trường; không thay save handler hoặc quyết định của BUS. SchedulePreview không chặn/cho phép lưu.
- ReadOnlyViews.profile đọc model gốc trong sự kiện cập nhật để không phụ thuộc thời điểm RowSorter cập nhật; không đổi dữ liệu hay thứ tự bảng.
- Poster được tải bằng SwingWorker; Theme không gọi CSDL. Không thêm Thread.sleep vào mã giao diện.

## 5. Thư viện

Chỉ thêm com.formdev:flatlaf:3.7.2 qua Maven, để có LAF sáng/tối, trạng thái tương tác và HiDPI. Giữ Java 17, cách build Maven và các dependency cũ. Không thêm thư viện icon, ngày giờ hay chart.

## 6. Giả định và khác biệt có chủ đích

- Giữ cửa sổ chức năng riêng với cùng shell thay vì tái tạo màn bên trong một cửa sổ; tránh mất giỏ, timer hoặc hook đóng cửa sổ cũ.
- “Đang chọn” là lựa chọn tại UI; “Đang giữ” là trạng thái server; ghế giao dịch hiện tại có dấu riêng, không gộp thành trống.
- Sơ đồ cấu trúc phòng chỉ biết Trống/Không khả dụng; trạng thái giữ/bán thuộc một suất chỉ thể hiện ở quầy bán vé.
- Tồn kho chỉ đếm sản phẩm còn/hết hàng, không cộng lẫn ly/hộp/chai. Biểu đồ doanh thu giữ thứ tự hàng và ghi rõ khi chỉ vẽ 6 dòng đầu; bảng bên dưới đầy đủ.
- Preview lịch chỉ bao phủ dữ liệu đang hiển thị, có thể đang lọc. Ràng buộc cuối cùng vẫn ở BUS/CSDL.
- Màn hẹp/DPI cao có thanh cuộn khi thiếu diện tích; không giảm cỡ chữ để cố nhét nội dung. Tablet là Windows 1024×768, không phải app Android/iOS.
- Không có chuyển động bố cục; không cần hiệu ứng phụ để thao tác tại quầy.

## 7. Tồn đọng và đề xuất tiếp theo

- Chạy thử với máy in vé và đầu đọc QR của quầy; hiện chỉ xác minh QR từ ảnh, preview và kiểm thử đầu ra in.
- Duyệt tay toàn bộ đường Tab/Shift+Tab và mọi form sửa/phân loại/giá trong sáng/tối; thử màn hình thực có DPI 125/150/200% và chuyển giữa hai màn hình. Scale 150% trong test là mô phỏng cấu hình FlatLaf, không thay cho thử thiết bị.
- Nghiệm thu với người thao tác quầy bằng một ca bán thật, dùng CSDL kiểm thử trước; không cần thay đổi BUS để thực hiện bước này.
- Thư mục target từng bị lỗi artifact/khóa trong quá trình phát triển; từng dùng output tạm để chẩn đoán. Kết quả cuối phải đọc dòng kiểm thử bên trên để biết đã chạy bằng POM nào; không giấu các lượt lỗi trong PROGRESS.
- Sau nghiệm thu giao diện, tiếp tục tổng rà soát đồ án và class diagram theo code theo kế hoạch môn học. Chưa sửa báo cáo Word trong đợt redesign.

## 8. Build, chạy và đổi theme

Tại thư mục repo với JDK 17 và Maven:

~~~powershell
mvn clean test
mvn test -Dcinema.mysql.tests=true
mvn compile exec:java
~~~

Kiểm thử MySQL cần database kiểm thử có danh mục demo và cấu hình JDBC hợp lệ sẵn có theo docs/CAI_DAT.md; các test tích hợp tạo fixture riêng rồi dọn. Lệnh đầu không bật cờ MySQL sẽ bỏ qua test tích hợp; không dùng nó để suy ra mọi ca nghiệp vụ đã đạt. Không nhập lại SQL vào dữ liệu đang dùng.

Cũng có thể chạy scripts/Run-Desktop.ps1 theo hướng dẫn cũ. Nút “Giao diện sáng/tối” nằm ở đăng nhập và cuối sidebar, đổi theme cho các cửa sổ đang mở. Enter tìm/đăng nhập theo màn; Ctrl+K đưa focus vào tìm chức năng. Giữ nguyên tên gọi và quyền truy cập nghiệp vụ.

Ảnh bằng chứng được sinh lại bằng các test desktop, LoginLayoutTest và RedesignReviewMySQLTest trong target; không commit artifact build. Hai tài liệu nền là UI_SPEC.md và PROGRESS.md. Mỗi sprint có commit riêng trên ui-redesign; nhánh chính không bị commit và chưa push.
