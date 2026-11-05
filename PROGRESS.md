# PROGRESS — UI redesign

## Baseline

- Trước sửa UI: main cfd13ba; Java 17, Maven 3.9.9, Swing/AWT; MySQL JDBC + ZXing, JUnit 5. Không có web framework, CSS, lint/typecheck riêng. javac là kiểm tra kiểu; Maven clean test là build/test.
- Đã chạy 05/11/2026: mvn clean test '-Dcinema.mysql.tests=true' → BUILD SUCCESS, 144/144, 0 lỗi/0 bỏ qua. Chạy MySQL cục bộ bằng script sẵn có, không sửa cấu hình.
- UI hiện tạo JPanel/JButton/JTable trực tiếp, layout Border/Grid/Flow, màu cục bộ ở ghế. Chưa có design system. Các SwingWorker và listener nằm chung lớp UI; rủi ro đổi cây component làm mất listener, selection/sorter, busy/enable, timer giữ ghế hoặc callback modal.
- Hook kiểm thử hiện dùng tên cửa sổ/nút, kiểu JTextField và số cột, JTable/JList đầu tiên, model và selection. Giữ nguyên model/field/hàm/tên nút thao tác cũ; không sửa test để bỏ qua lỗi.
- Toàn bộ màn hình/biểu mẫu theo vai trò liệt kê trong UI_SPEC.md. Không có baseline lỗi build/test đã biết.

## Hướng thiết kế

Cinema Workspace, nền tối xanh than, tím iris, light mode tương ứng; Segoe UI + Consolas, spacing bội 4, card viền rõ. Tham chiếu đầy đủ tám ảnh dưới đây. Không sao chép ảnh/logo/nội dung mẫu. Không thêm dependency.

## Khảo sát từng ảnh — đã mở và xem cả 8 WebP

### original-084d6eeafc402c32ab470a94220baa1e.webp

Dashboard thiên nhiên: nền navy, rail trái, 2 vùng thẻ đứng/biểu đồ; sans hình học, thẻ dọc màu tươi, chart đường lưới mảnh. Học tỷ lệ poster và phân lớp nền; bỏ hero, sidebar chỉ icon và nhiều màu gây phân tán.

SHA-256: 071ac275c45154f1eb3723734d1455d84e52727ce9d2370989506f43c46187d4

### original-294d4646016457397b2b51e9fc5eb11b.webp

Dashboard doanh số: sidebar có chữ, KPI nhỏ, panel phải; xám than/teal, sans tròn, thẻ lõm và gauge. Học phân nhóm KPI và khối người dùng; không lấy neumorphism vì tương phản thấp, số liệu/gauge không có nguồn ở đồ án.

SHA-256: ba76bf7c58541f857ac17e7639b7e7c9ae4e6d4f1b2d76f7734406ee0d72ccaa

### original-70c2da112175bc17e2eeb8bf0813983d.webp

Dashboard thể thao: sidebar chữ + hồ sơ, topbar tìm, hàng dữ liệu dạng thẻ, tab pill, panel tóm tắt phải; than/emerald/xanh, chữ sans rõ. Học vùng lọc và nhịp hàng, sidebar active; bỏ nội dung cá cược, banner, thông báo và các tính năng ngoài phạm vi.

SHA-256: bfb7a06d4ca44fcf3448c1328961f912a43b07bcfc05b7c8b88238185edfdcce

### original-77b078d77f6bc5e2d1b1e9e044784034.webp

Dashboard cuộc gọi: canvas đen, dải ngày ngang, biểu đồ lớn, thẻ chỉ số có lưới chấm; lime/mint, tiêu đề đậm. Học trạng thái chọn ngày, ký hiệu ghế và khoảng cách; giảm glow, bỏ ảnh avatar/nội dung gọi điện, không vẽ đường xu hướng khi API chỉ trả tổng.

SHA-256: 82c51b6f7023d4cf1213a6cba6ed9b58161d6a6de49fccee33960b05d6f820b7

### original-9be6263de6c0cec5f29a540068b25ae8.webp

Dashboard analytics sáng: nền trắng, đường viền mảnh, KPI + sparkline, chart lớn và panel phụ; tím/đen, sans thân thiện. Học light theme, số lớn, nhãn đơn vị; bỏ radar/trend/CSV vì chưa có logic nguồn.

SHA-256: 0d83d812f8299751382d6504539260286f72e66ee35ed58aa97ae5742e36e7b5

### original-be9eb88c3b8ee1e6907bbec645cd0dd8.webp

Dashboard ngân hàng: sidebar có chữ, hồ sơ, thẻ số liệu trên, danh sách giao dịch phải; than đen, nhấn tím/đỏ/vàng, sans tròn. Học tóm tắt giao dịch, căn phải tiền và viền thẻ; không dùng ví/chuyển tiền, gradient chart hoặc phần trăm giả.

SHA-256: f3cb9083bd6aaa6c48848c0d99fa164a07b7d30f8f57035cdd2c1ffda16eafcb

### original-d56ade39b839acf74e5c1fd45311edf6.webp

Dashboard tác vụ: sidebar chữ, topbar, hai thẻ hình lớn, lịch/panel phải, card thành viên; nền đen/xanh, sans rõ. Học thư viện poster, khối hồ sơ, cỡ thẻ nhất quán; không lấy task/chat/lịch việc/tiến độ hoặc ảnh người trong mẫu.

SHA-256: c2785819b3ca828f688e3de8a101fa8c22f71673d44f1a00ba111e83ff2bee29

### original-e3d05cf9b27f75410fd029126a5aae8f.webp

Dashboard thời tiết: bố cục sáng 2 cột, thẻ đứng pastel, chart riêng có lọc; trắng/coral/tím, chữ sans rộng rãi. Học light mode và phân vùng báo cáo; giảm khoảng trống, không lấy minh họa địa danh hoặc video và không dùng pastel thiếu tương phản làm chữ.

SHA-256: b14dc19699f0c1b01a66f682c01795d08e1d093028a448e0cafff1e78c96330f


## Tiến độ từng sprint

- Giai đoạn 0: xong khảo sát ảnh/code/baseline, chốt hướng, tạo hai tài liệu. Chưa sửa code giao diện tại mốc này.
- Sprint 1: đã hoàn tất nền tảng FlatLaf và kiểm chứng; ghi nhận chi tiết bên dưới.
- Sprint 2: đã kiểm chứng bán vé và tra cứu giao dịch; xem ghi nhận bên dưới.
- Sprint 3: chưa bắt đầu.
- Sprint 4: chưa bắt đầu.
- Sprint 5: chưa bắt đầu.
- Sprint 6: chưa bắt đầu.
- Sprint 7: chưa bắt đầu.

## Giả định

- Giữ Java Swing. Tablet nghĩa là cửa sổ 1024×768 trên Windows, không phải ứng dụng Android/iOS/web.
- Topbar tìm trong các chức năng được phép truy cập (điều hướng UI), không thêm API tìm kiếm toàn CSDL.
- Đăng xuất là đóng phiên giao diện và mở lại đăng nhập; không thay xác thực/quyền, phải tôn trọng requestClose của giao dịch đang xử lý.
- Giữ vòng đời cửa sổ chức năng hiện có để không làm mất giỏ/hold và hook kiểm thử; dùng cùng shell/sidebar/topbar trong các cửa sổ. Không tạo route mới.
- Kết quả soát vé ngoài bốn trường hợp mẫu (hết giờ/không tồn tại/hủy/phòng đóng) vẫn giữ nguyên thông báo nghiệp vụ, không ép thành hợp lệ.
- Lịch chiếu có kiểm tra xung đột trước lưu tại BUS; chỉ có thể cảnh báo trực quan tức thời từ dữ liệu lịch đang có, kiểm tra cuối cùng vẫn ở BUS. Không thêm API validation/mutation.
- Chart chỉ dùng số liệu đã trả về; không vẽ trend/sparkline giả hay thêm CSV (hiện chưa có).
- Giữ năm trạng thái ghế gồm Trống/Đang giữ/Đang chọn/Đã bán/Không khả dụng.

## Thay đổi chạm logic

Chưa có. Không đổi BUS/DAO/model/migration/config. Mọi thay đổi điều phối UI cần ghi tại đây, giữ nguyên hàm/state và callback nghiệp vụ.

## Vấn đề tồn đọng

- Cần xác minh từng sprint bằng kiểm thử thật và ảnh dark/light, 1366/tablet, bàn phím.
- Phần cứng máy in/đầu đọc QR chưa có; tiếp tục dùng preview/QR ảnh và kiểm thử dữ liệu đã có.
- Reference_UIUX là file người dùng chưa tracked: không add/commit, không đổi/xóa/di chuyển. Hash trước/sau để kiểm chứng.

## Tiếp tục sau lần ngắt — bản giao việc Java Swing

Đã đọc lại toàn bộ UI_SPEC/PROGRESS và code Theme/Design/Shell. Giai đoạn 0 trước đó đã xem đủ 8 ảnh và baseline 144/144; không làm lại từ đầu. Mã nền tảng chưa commit khi phiên bị ngắt. Trước tích hợp FlatLaf đã chạy lại đủ 147/147 (144 cũ + 3 design system), không sửa các test cũ. Lượt thử đầu có 3 timeout do topbar search đứng trước trường nghiệp vụ trong cây component; đã đổi thứ tự thêm component, giữ UI và test tương thích. Đã cập nhật nguyên văn mục 3/5/5B theo bản mới, đặc biệt 3.6.

Khả thi Swing của 8 ảnh: 084d/70c2/9be6/be9e/d56a/e3d0 có mức cao cho bố cục thẻ/sidebar/bảng, vừa cho chart Graphics2D; 294d chỉ học phân vùng (không dùng lõm); 77b0 học dải ngày/ký hiệu ghế, không hiệu ứng glow/curve giả. Tất cả WebP đọc được.

## Thư viện đã thêm

FlatLaf 3.7.2 (com.formdev:flatlaf qua Maven): nền LAF sáng/tối, combo/spinner/dialog và HiDPI nhất quán theo yêu cầu mới. Nguồn: https://github.com/JFormDesigner/FlatLaf/releases/tag/3.7.2 và https://www.formdev.com/flatlaf/customizing/. Không thêm thư viện icon/ngày/biểu đồ khác; icon và chart tự vẽ vector/Graphics2D.

## Sprint 1 — Nền tảng đã kiểm chứng

Thêm Theme (toàn bộ token), Tokens facade, Skin/Icons/Design/Shell; page chrome, sidebar theo vai trò, người dùng, đăng xuất, tìm điều hướng, đổi sáng/tối trên mọi cửa sổ. Theme cập nhật cây component lúc chạy; bảng có renderer/số/nhãn trạng thái, form dài dùng GridBag và cuộn, toolbar wrap và focus. Java/Maven giữ nguyên. Đã kiểm tra AA hai palette qua 3 test design system. Trước FlatLaf: đủ 147/147 đạt; sau FlatLaf build sạch và 141 test đầu đều đạt, VM thoát ở test đăng nhập vì thanh titlebar FlatLaf thêm nút close vào cây Swing. Đã giữ titlebar Windows ngoài cây Swing (không sửa test), chạy lại 6/6 test đăng nhập + design system đều đạt. Sẽ chạy bộ đầy đủ trên revision cuối ở Sprint 7. Có ảnh màn Admin thật target/uc02-admin.png và theme dark/light/tablet target/ui-redesign/sprint1; đã xem để chỉnh title, nền phân lớp, header bảng. Lỗi incremental artifact sau các lần ngắt được giải quyết bằng clean; không đổi lớp nghiệp vụ.

Điều phối UI: CinemaWindow giữ open cùng các nhánh/nút cũ; Shell gọi lại các callback đó. Đăng xuất hỏi xác nhận và tôn trọng requestClose trước đóng cửa sổ. Không thêm API, không đổi BUS/DAO/model/config/SQL, không đổi test cũ. Chưa nghiệm thu phần chuyên biệt từng màn; tiếp tục Sprint 2.

## Sprint 2 — Bán vé và kiểm tra giao dịch

Thêm SalesViews (bảng suất/giao dịch đồng bộ model chọn cũ, chi tiết có cấu trúc), SeatVisual (4 trạng thái + đánh dấu giao dịch hiện tại, ký hiệu, loại ghế, chú giải đồ họa/màn hình), DateInputs (JSpinner trong popup ghi lại ô nhập cũ). BanVePanel chỉ đổi cây component, renderer và bố cục thanh toán; giữ nguyên cart/id/selected/showId/busy/paymentOpen/needsCheck, thứ tự run/hold/quote/payment/issue/recover/cancel, timer 5000ms, canLeave/requestClose. Các JTextArea/JComboBox/JList cũ giữ làm nguồn binding nội bộ, không còn là trình bày chính. Không thêm truy vấn DB; đọc ảnh/nghiệp vụ vẫn theo SwingWorker hiện có.

Đã chạy 28/28: SalesMySQLTest 18, SalesDesktopMySQLTest 2, InVeTest 2, DesignSystemTest 3, SalesPresentationTest 3. Kiểm chứng thực tế bán từ chọn/giữ/tính tiền đến thanh toán/preview QR, phục hồi giữ/đóng thanh toán, lịch sử và chống trùng; các test cũ không sửa. Ba test mới xác minh summary giữ đủ trường, selection adapter và ghế không đổi availability. Đã xem target/uc08-sales.png và uc08-preview.png; lần ảnh đầu ghế bị đẩy xuống, đã thu gọn danh sách suất và đưa Chọn suất sang bên, ảnh mới thấy ghế cùng giỏ và nút. Tổng tiền luôn nổi bật, chi tiết cuộn dọc. Máy in vật lý chưa kiểm tra, preview/QR có test. Sáng/tối + resize toàn bộ màn sẽ xác minh thêm tại Sprint 7.
