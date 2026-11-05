# UI_SPEC — Cinema Workspace (chuẩn Java Swing cập nhật)

## Ràng buộc gốc mới nhất

## 3. NGUYÊN TẮC BẤT BIẾN

3.1. Bảo toàn chức năng

- Không đổi logic nghiệp vụ, lớp BUS/DAO/model, phân quyền, tên phương thức

  public mà nơi khác đang gọi.

- Nếu bắt buộc phải chạm vào logic để giao diện hoạt động, chọn cách ít xâm lấn

  nhất, ghi rõ vào PROGRESS.md (file, lý do, nội dung thay đổi).

3.2. Không mở rộng phạm vi

- Không thêm chat, task, banner quảng cáo, hero, bảng xếp hạng, hay bất kỳ tính

  năng nào không có trong hệ thống.

- Luồng bán vé của Nhân viên KHÔNG có F&B. F&B chỉ nằm ở màn quản lý của Admin.

3.3. An toàn thao tác

- Làm việc trên git branch `ui-redesign`. Không commit trực tiếp lên nhánh chính.

- Không force push, không xóa lịch sử git, không xóa file không thuộc tầng

  giao diện.

- Không chạm vào file cấu hình kết nối CSDL, script SQL, dữ liệu thật.

- Thư viện: được thêm FlatLaf (và gói icon đi kèm nếu cần) vì đây là nền tảng

  để làm giao diện hiện đại cho Swing. Với ngày giờ, ưu tiên JSpinner /

  JFormattedTextField; chỉ thêm thư viện chọn ngày nhỏ gọn nếu thật cần. Biểu

  đồ: ưu tiên tự vẽ bằng Graphics2D; chỉ thêm thư viện biểu đồ nếu tự vẽ không

  khả thi. Mọi thư viện thêm vào phải ghi PROGRESS.md kèm lý do và phiên bản,

  và thêm theo đúng cách project đang quản lý dependency (Maven, Gradle hay

  thư viện thủ công; kiểm tra file build trước). Không thêm thư viện nào khác.

- Giữ nguyên phiên bản Java và cách build hiện tại của project.

3.4. Bản quyền ảnh tham khảo

- Ảnh tham khảo là dashboard mẫu thuộc các lĩnh vực khác (cá cược, thời tiết,

  ngân hàng, quản lý tác vụ...), thiết kế cho web/app. Học bố cục, pattern và

  cảm giác thẩm mỹ, rồi chuyển thể sang những gì Swing làm tốt. KHÔNG sao chép

  nội dung, logo, hình ảnh, chữ hay thương hiệu trong ảnh.

3.5. Quyền tự quyết

- Gặp điểm mơ hồ: tự chọn phương án hợp lý nhất, ghi vào mục "Giả định" trong

  PROGRESS.md, làm tiếp. Không dừng lại hỏi trừ khi rơi vào mục 10.

3.6. QUY TẮC AN TOÀN RIÊNG CHO CODE SWING HIỆN TẠI

Các lớp view đang trộn giao diện với trạng thái nghiệp vụ. Ví dụ BanVePanel có

các trường cart, id, selected, showId, busy, paymentOpen, needsCheck, hàm

run(...) dùng SwingWorker, timer refresh 5 giây, canLeave(), requestClose(),

recover(). Các thành phần này là cơ chế chống mất tiền và chống bán trùng ghế.

- TUYỆT ĐỐI KHÔNG đổi ý nghĩa, thứ tự gọi, cách khởi tạo của các trường và hàm

  điều khiển trạng thái này. Chỉ thay phần dựng và trình bày giao diện.

- Mọi truy cập CSDL tiếp tục chạy ngoài Event Dispatch Thread (SwingWorker như

  hiện tại); mọi cập nhật giao diện ở trên EDT. Không đưa thao tác DB vào luồng

  giao diện. Không thêm Thread.sleep hay thao tác chặn trên EDT.

- Giữ nguyên các nút/chức năng đang có (kể cả "Kiểm tra mã hiện tại", "Giữ ghế /

  đổi ghế", "Hủy giao dịch", "Giao dịch mới", "In lại vé", "Tiếp tục giao dịch

  đã chọn"). Được đổi hình thức, vị trí, nhãn cho dễ hiểu; không được bỏ.

- Được tách phần dựng giao diện ra lớp/phương thức riêng cho dễ đọc, nhưng giữ

  nguyên chữ ký các phương thức public mà nơi khác gọi (ví dụ requestClose()).

- Không format lại cả file chỉ để đổi kiểu viết; diff phải tập trung vào thay đổi

  có chủ đích để dễ review.

- Ô nhập ngày/giờ có thể đổi sang thành phần chọn ngày giờ, nhưng dữ liệu chuyển

  xuống tầng nghiệp vụ vẫn là LocalDate / LocalTime như cũ, và vẫn xử lý được

  trường hợp để trống như hiện tại.

- Nội dung giỏ hàng và chi tiết giao dịch hiện là văn bản thuần trong JTextArea.

  Được trình bày lại thành bảng/nhãn có cấu trúc, nhưng thông tin phải đầy đủ như

  cũ (mã giao dịch, thời gian tạo, nhân viên, trạng thái, phim, suất, phòng, từng

  ghế + giá, hạn giữ, tổng tiền, thông tin thanh toán).

- Trạng thái ghế dùng nhất quán trong toàn hệ thống: Trống / Đang giữ / Đã bán /

  Không khả dụng (kèm ghế thuộc giao dịch hiện tại). Đưa màu vào Theme, thêm

  ký hiệu hoặc icon ngoài màu.


## 5. CHẤT LƯỢNG TỐI THIỂU (áp dụng cho mọi phong cách)

Đây là các tiêu chuẩn cứng. Phong cách có thể tự do, các điểm này thì không.

- Light / Dark: hỗ trợ cả hai, có nút chuyển, đổi được lúc ứng dụng đang chạy

  (cập nhật cả cây component đang mở, ví dụ bằng

  SwingUtilities.updateComponentTreeUI). Tự chọn theme mặc định.

- Khả năng đọc: tương phản chữ/nền đạt WCAG AA. Tránh phong cách tương phản thấp

  vì khó đọc khi thao tác nhanh.

- Khung chính: thanh điều hướng bên có chữ (không chỉ icon), khối thông tin

  người dùng (tên, vai trò), nút đăng xuất. Menu hiển thị theo vai trò; Nhân

  viên không thấy mục quản lý và báo cáo.

- Design tokens: màu, font, cỡ chữ, khoảng cách, bo góc khai báo tại MỘT nơi

  (lớp Theme trong cinema.view.ui). Không hardcode new Color(...) hay new

  Font(...) trong các panel. Một màu nhấn chính, kèm bộ màu ngữ nghĩa success /

  warning / danger / info.

- Bộ component dùng chung trong cinema.view.ui: nút (chính/phụ/nguy hiểm), ô

  nhập, ô chọn, bảng dữ liệu, hộp thoại / form, badge trạng thái, thẻ (card),

  thông báo (toast hoặc thanh trạng thái), màn hình trống, chỉ báo đang tải.

- Mọi màn hình có đủ ba trạng thái: đang tải, rỗng, lỗi.

- Thao tác rủi ro (xóa, ngừng chiếu, hủy suất chiếu, hủy giao dịch) phải có hộp

  thoại xác nhận. Form kiểm tra dữ liệu ngay tại từng trường, thông báo lỗi nêu

  rõ cách sửa.

- Không truyền đạt thông tin chỉ bằng màu: kèm icon, nhãn hoặc ký hiệu.

- Dùng được bằng bàn phím: thứ tự Tab hợp lý, focus hiển thị rõ, phím tắt cho

  thao tác chính ở quầy bán vé (ví dụ Enter để tìm) mà không đổi hành vi nghiệp vụ.

- Co giãn: cửa sổ resize không vỡ bố cục, dùng được ở độ phân giải tối thiểu

  1366x768; hạn chế setPreferredSize bằng pixel cố định, dùng LayoutManager phù

  hợp (BorderLayout, GridBagLayout, MigLayout nếu project đã có...). Hỗ trợ màn

  hình có scale DPI cao (font, icon, khoảng cách không bị nhỏ xíu).

## 5B. TIÊU CHUẨN THẨM MỸ (kỷ luật thiết kế, không ép phong cách)

Phong cách do bạn chọn, nhưng giao diện phải đạt độ hoàn thiện của một sản phẩm

thương mại, không phải giao diện Swing mặc định hay giao diện "làm cho xong".

- Phân cấp thị giác: mỗi màn có một điểm nhấn chính rõ ràng; người dùng nhìn vào

  biết ngay việc quan trọng nhất. Không để mọi thứ cùng độ nổi.

- Khoảng cách: dùng một thang spacing cố định (ví dụ bội số của 4 hoặc 8 px) đặt

  thành hằng số trong Theme, căn lề thẳng hàng. Không dùng số tùy tiện trong

  EmptyBorder hay layout.

- Typography: tối đa 5-6 mức cỡ chữ, tối đa 2 họ font, đảm bảo font có hiển thị

  đúng tiếng Việt có dấu. Phân biệt rõ tiêu đề / nội dung / chú thích. Số tiền,

  số lượng, giờ căn phải và dùng chữ số đều chiều rộng để cột số thẳng hàng.

- Màu: dùng màu nhấn tiết chế, chỉ cho hành động chính và trạng thái quan trọng.

  Cùng một trạng thái luôn cùng màu và cùng kiểu badge trên toàn hệ thống.

- Nhất quán: cùng loại phần tử thì cùng bo góc, cùng chiều cao nút/ô nhập, cùng

  cách đặt nút hành động (vị trí nút chính, nút hủy).

- Icon: dùng MỘT bộ icon đồng bộ nét và kích thước (vector, SVG hoặc icon font,

  sắc nét ở mọi DPI). Không dùng emoji làm icon.

- Mật độ thông tin: màn nhập liệu và bán vé cần gọn, hiệu quả; màn báo cáo cần

  thoáng, dễ đọc. Không nhồi quá nhiều thứ vào một khối, không để khoảng trống

  vô nghĩa.

- Trạng thái tương tác: mọi phần tử bấm được có đủ hover, focus, pressed,

  disabled. Hiệu ứng ngắn, tinh tế, không làm chậm thao tác.

- Dữ liệu: bảng (JTable) phải được tùy biến: header rõ, hàng dễ quét, căn phải

  cho số, căn trái cho chữ, dòng xen kẽ hoặc đường kẻ nhẹ, chọn dòng rõ ràng,

  cột co giãn hợp lý. Không để JTable/JList trông như mặc định. Poster phim có

  tỷ lệ cố định và ảnh thay thế khi thiếu hoặc lỗi.

- Biểu đồ: cùng một phong cách trên toàn hệ thống (màu, đường lưới, tooltip,

  chú giải); đủ nhãn trục, đơn vị; không dùng biểu đồ chỉ để trang trí.

- Sơ đồ ghế và các màn thao tác chính (bán vé, soát vé) phải được chăm chút hơn

  mức trung bình, vì đây là nơi nhân viên làm việc nhiều nhất.

- Tránh vẻ ngoài "mẫu có sẵn": không để nguyên mặc định của FlatLaf hay Swing;

  tinh chỉnh token để hệ thống có bản sắc riêng.

- Văn bản giao diện bằng tiếng Việt, ngắn gọn, đúng thuật ngữ, thống nhất cách

  gọi (ví dụ luôn dùng "suất chiếu", không lúc "ca chiếu" lúc "buổi chiếu").


## Hướng thiết kế đã chốt

Cinema Workspace: tối mặc định, bề mặt xanh than phân lớp, tím iris làm điểm nhấn duy nhất. Light mode giữ cấu trúc với nền xám sáng và surface trắng. Không sao chép thương hiệu/hình/nội dung mẫu, không thêm dashboard số liệu giả. Màn quầy tập trung ghế và đơn hàng, quản trị tập trung tìm/lọc/bảng, phim dùng thư viện poster. Viền tinh tế thay hiệu ứng lõm; chỉ thêm FlatLaf 3.7.2 theo bản giao việc Java Swing.

## Tokens và component

Mọi màu/font/spacing/radius/shadow khai báo trong cinema.view.ui.Theme; Tokens là facade kế thừa để giữ tương thích các component đã dựng, không chứa định nghĩa trùng. Dark: background #10111B, surface #191B2A, raised #222538, border #3A4058, text #F2F4FC, muted #B0B9D0, accent #B7A1FF, accentInk #181327; success #7EE2B5, warning #F5CF83, danger #FFADB4, info #9EC4FF. Light: #F3F4F8/#FFFFFF/#F7F7FC/#C3C7D3, text #1B2035, muted #555F77, accent #5637AF, accentInk #FFFFFF, semantic #146548/#785100/#A52C3D/#254E98. Kiểm tra tỷ lệ AA cho chữ chính/phụ/nút/badge.

Spacing 4/8/12/16/24/32; radius 12 control, 16 card; modal dùng cùng thang spacing. Control cao 40–44, bảng hàng 44, header 44. Font Segoe UI (fallback sans), số/mã Consolas (fallback monospace), thang 11/12/14/16/20/28. Nét icon đồng bộ 1.6px, không emoji. Motion 160ms cho hover nếu có; giảm chuyển động qua cinema.ui.reducedMotion và desktop property win.clientAreaAnimation. Không cần trình duyệt/CSS prefers-reduced-motion vì đây là Swing; mặc định không có chuyển động bố cục.

Theme: theme chung toàn tiến trình, toggle rõ chữ; FlatLaf 3.7.2 + UIManager; Graphics2D cho ghế, thẻ và chart. Components: button/input/select/table/modal/badge/card/status toast/empty/loading, validated field, wrap toolbar, responsive split, poster card, seat button, report chart. Theme đổi được cả cửa sổ/modal đang mở, không đổi model hoặc callback nghiệp vụ.

## Cấu trúc và quy ước

- cinema.view.ui: Tokens, Theme, component/UI delegates, Icons, Shell, layout/validation helpers, chart.
- Các lớp Panel/Window cũ giữ tên field/hàm/state, gọi BUS/DAO như cũ; chỉ thay cây component/trình bày.
- Component mới có accessibleName, labelFor, focus rõ. Tên client property dùng cinema.*. Không đặt màu tại component.
- src/test/java/cinema/view: kiểm tra theme/contrast/layout; integration giữ nguyên các kiểm thử nghiệp vụ hiện có. Ảnh QA nằm target/ui-redesign, không commit bản build.
- UI_SPEC.md là chuẩn; PROGRESS.md ghi từng sprint và ngoại lệ; REDESIGN_REPORT.md chỉ viết kết quả đã xác minh.

## Danh mục màn hình

Chung: DangNhapWindow; CinemaWindow (menu theo vai trò). Admin: NhanVienPanel (danh sách/chi tiết/thêm/sửa/xóa); PhimPanel (lọc/chi tiết/thêm/sửa tên/poster/thể loại/ngừng); LichChieuPanel (danh sách/tạo/đổi phim/phòng/ngày/giờ/hủy); PhongChieuPanel (phòng/thêm/sửa/trạng thái, SeatMap và form ghế/phân loại); GiaVeFnBPanel (bảng giá và form giá, sản phẩm/tồn kho/form sản phẩm/giá bán); BaoCaoPanel (thời gian/phim/nhân viên/vé/F&B). Nhân viên: hồ sơ cá nhân, tra cứu phim, tra cứu phòng và sơ đồ ghế, KiemTraVePanel (mã/ảnh QR/kiểm tra/xác nhận), BanVePanel (suất/ghế/giỏ/thanh toán/preview-in và lịch sử giao dịch).

## Responsive và dữ liệu

Kiểm tra 1366×768 và tablet 1024×768 (tablet Windows, không chuyển thành web). Sidebar thu gọn có chữ ở ngưỡng hẹp; toolbar wrap, vùng lọc cuộn khi cần; split chuyển dọc khi hẹp. Bảng/sơ đồ cho phép cuộn, không che nút quan trọng. Form cuộn khi quá cao.

Giữ cả trạng thái Đang giữ của giao dịch khác ngoài bốn trạng thái tối thiểu của đề bài; không gộp thành Trống. Chọn tại UI chưa phải giữ tại server. Bán vé không có F&B. Không sửa BUS/DAO/model/Database/migration/config, không thêm CSV, không suy diễn dữ liệu chuỗi thời gian từ số tổng. Chart chỉ biểu diễn kết quả báo cáo đã có, có nhãn/đơn vị và bảng đối chiếu.

## An toàn Swing bổ sung

Giữ nguyên khởi tạo/thứ tự và ý nghĩa cart/id/selected/showId/busy/paymentOpen/needsCheck, các run/SwingWorker, timer 5 giây, canLeave/requestClose/recover. Không sửa BUS/DAO/model. Trình bày danh sách suất và giao dịch bằng model trình bày đồng bộ với model cũ, không thêm truy vấn; tóm tắt giao dịch có cấu trúc và giữ đủ thông tin từ description. Dùng JSpinner/JFormattedTextField cho nhập ngày/giờ; chuyển xuống BUS vẫn LocalDate/LocalTime, giữ ô trống nếu được phép. Không thêm thread sleep trên EDT.

## Bổ sung kiểm chứng Sprint 7

Sidebar có kích thước theo UIScale, nhãn hai dòng ở chế độ gọn. AdaptivePage giữ control có thể truy cập bằng cuộn khi cửa sổ/DPI thiếu diện tích. Các chế độ ngày/tháng/năm/giờ giữ định dạng parser cũ và ô tùy chọn để trống. Xem REDESIGN_REPORT.md để phân biệt tiêu chí đã chạy và tiêu chí cần thử thiết bị/duyệt tay.

## Điều hướng một cửa sổ — cập nhật theo phản hồi người dùng

Một JFrame và một Shell cho mỗi phiên đăng nhập; sidebar/topbar giữ nguyên đối tượng khi chuyển chức năng bằng CardLayout. Không tạo cửa sổ chính mới từ menu. Biểu mẫu/chi tiết/thanh toán vẫn là JDialog. Giữ panel bán vé trong cây component kể cả khi ẩn để bảo toàn state và timer; đóng ứng dụng/đăng xuất gọi requestClose. Các màn khác tải lại khi mở lại để cập nhật danh mục, không tạo lại nếu bấm đúng mục đang mở. Đây là quyết định mới thay giả định giữ cửa sổ chức năng riêng trong lịch sử PROGRESS.md.
