# UI_SPEC — Cinema Workspace

## Ràng buộc gốc (mục 3, 5, 5B)

## 3. NGUYÊN TẮC BẤT BIẾN

3.1. Bảo toàn chức năng
- Không đổi logic nghiệp vụ, API, schema, route, phân quyền, tên hàm, tên state.
- Nếu bắt buộc phải chạm vào logic để giao diện hoạt động, chọn cách ít xâm lấn
  nhất, ghi rõ vào PROGRESS.md (file, lý do, nội dung thay đổi). Không tự ý viết
  lại logic.

3.2. Không mở rộng phạm vi
- Không thêm chat, task, banner quảng cáo, hero, bảng xếp hạng, hay bất kỳ tính
  năng nào không có trong hệ thống.
- Luồng bán vé của Nhân viên KHÔNG có F&B. F&B chỉ nằm ở màn quản lý của Admin.

3.3. An toàn thao tác
- Làm việc trên branch `ui-redesign`. Không commit trực tiếp lên nhánh chính.
- Không dùng force push, không xóa lịch sử git, không xóa file không thuộc tầng
  giao diện.
- Không chạm vào file cấu hình môi trường (.env, secrets), CSDL, migration.
- Không cài thêm thư viện lớn khi chưa cần. Nếu thêm dependency thì phải ghi
  vào PROGRESS.md kèm lý do. Ưu tiên tận dụng thư viện đã có.
- Giữ nguyên framework và tech stack hiện tại.

3.4. Bản quyền ảnh tham khảo
- Ảnh tham khảo là dashboard mẫu thuộc các lĩnh vực khác (cá cược, thời tiết,
  ngân hàng, quản lý tác vụ...). Học bố cục, pattern và cảm giác thẩm mỹ.
  KHÔNG sao chép nội dung, logo, hình ảnh, chữ hay thương hiệu trong ảnh.

3.5. Quyền tự quyết
- Gặp điểm mơ hồ: tự chọn phương án hợp lý nhất, ghi vào mục "Giả định" trong
  PROGRESS.md, làm tiếp. Không dừng lại hỏi trừ khi rơi vào mục 10.


## 5. CHẤT LƯỢNG TỐI THIỂU (áp dụng cho mọi phong cách)

Đây là các tiêu chuẩn cứng. Phong cách có thể tự do, các điểm này thì không.

- Light / Dark: hỗ trợ cả hai, có toggle. Tự chọn theme mặc định.
- Khả năng đọc: tương phản chữ/nền đạt WCAG AA. Tránh phong cách tương phản
  thấp (ví dụ neumorphism) vì khó đọc khi thao tác nhanh.
- Khung chung: sidebar có chữ (không chỉ icon), khối thông tin người dùng (tên,
  vai trò), nút đăng xuất; topbar có tìm kiếm. Menu hiển thị theo vai trò.
- Design tokens: màu, font, cỡ chữ, spacing, bo góc, đổ bóng khai báo tại MỘT nơi.
  Không hardcode giá trị màu trong component. Một màu nhấn chính, kèm bộ màu
  ngữ nghĩa success / warning / danger / info.
- Bộ component dùng chung: Button, Input, Select, Table, Modal/Drawer, Badge
  trạng thái, Card, Toast, EmptyState, Skeleton/Loading.
- Mọi màn hình có đủ ba trạng thái: loading, rỗng, lỗi.
- Thao tác rủi ro (xóa, ngừng chiếu, hủy suất chiếu, hủy giao dịch) phải có hộp
  thoại xác nhận. Form validate ngay tại từng trường, thông báo lỗi nêu rõ cách sửa.
- Không truyền đạt thông tin chỉ bằng màu (hỗ trợ người mù màu): kèm icon, nhãn
  hoặc ký hiệu.
- Điều hướng bằng bàn phím được, focus hiển thị rõ.
- Responsive tối thiểu: không vỡ bố cục ở laptop 1366px và tablet.

## 5B. TIÊU CHUẨN THẨM MỸ (kỷ luật thiết kế, không ép phong cách)

Phong cách do bạn chọn, nhưng giao diện phải đạt độ hoàn thiện của một sản phẩm
thương mại, không phải giao diện mặc định hay giao diện "làm cho xong".

- Phân cấp thị giác: mỗi màn phải có một điểm nhấn chính rõ ràng; người dùng
  nhìn vào biết ngay việc quan trọng nhất là gì. Không để mọi thứ có cùng độ nổi.
- Khoảng cách: dùng một thang spacing cố định (ví dụ bội số của 4 hoặc 8px),
  căn lề thẳng hàng theo lưới. Không dùng giá trị margin/padding tùy tiện.
- Typography: giới hạn thang cỡ chữ ở khoảng 5-6 mức, dùng tối đa 2 họ font.
  Phân biệt rõ tiêu đề / nội dung / chú thích. Số liệu tiền, số lượng, giờ dùng
  chữ số đều chiều rộng (tabular numbers) để cột số thẳng hàng.
- Màu: dùng màu nhấn tiết chế, chỉ cho hành động chính và trạng thái quan trọng.
  Màu ngữ nghĩa dùng nhất quán trên toàn hệ thống (cùng một trạng thái luôn
  cùng một màu và một kiểu badge).
- Nhất quán: cùng loại phần tử thì cùng bo góc, cùng đổ bóng, cùng chiều cao
  nút/ô nhập, cùng cách đặt nút hành động (vị trí nút chính, nút hủy).
- Icon: dùng MỘT bộ icon đồng bộ nét và kích thước. Không dùng emoji làm icon.
- Mật độ thông tin: màn nhập liệu và bán vé cần gọn, hiệu quả; màn báo cáo cần
  thoáng, dễ đọc. Không nhồi quá nhiều thứ vào một thẻ, không để khoảng trống
  vô nghĩa.
- Trạng thái tương tác: mọi phần tử bấm được có đủ hover, focus, active,
  disabled. Chuyển động ngắn và tinh tế (khoảng 150-250ms), tôn trọng
  `prefers-reduced-motion`. Không dùng hiệu ứng rườm rà làm chậm thao tác.
- Dữ liệu: bảng không được trông như bảng HTML thô; có header rõ, hàng dễ quét,
  căn phải cho số, căn trái cho chữ, thao tác theo hàng gọn gàng. Ảnh poster có
  tỷ lệ cố định và ảnh thay thế khi thiếu hoặc lỗi.
- Biểu đồ: cùng một phong cách trên toàn hệ thống (màu, đường lưới, tooltip,
  chú giải); đủ nhãn trục, đơn vị; không dùng biểu đồ chỉ để trang trí.
- Sơ đồ ghế và các màn thao tác chính phải được chăm chút hơn mức trung bình,
  vì đây là nơi nhân viên làm việc nhiều nhất.
- Tránh vẻ ngoài "mẫu có sẵn": không chỉ để mặc định của thư viện UI; tinh chỉnh
  token để hệ thống có bản sắc riêng.
- Văn bản giao diện bằng tiếng Việt, ngắn gọn, đúng thuật ngữ, thống nhất cách
  gọi (ví dụ luôn dùng "suất chiếu", không lúc "ca chiếu" lúc "buổi chiếu").



## Hướng thiết kế đã chốt

Cinema Workspace: tối mặc định, bề mặt xanh than phân lớp, tím iris làm điểm nhấn duy nhất. Light mode giữ cấu trúc với nền xám sáng và surface trắng. Không sao chép thương hiệu/hình/nội dung mẫu, không thêm dashboard số liệu giả. Màn quầy tập trung ghế và đơn hàng, quản trị tập trung tìm/lọc/bảng, phim dùng thư viện poster. Viền tinh tế thay hiệu ứng lõm; không thêm dependency.

## Tokens và component

Mọi màu/font/spacing/radius/shadow ở cinema.view.ui.Tokens. Dark: background #10111B, surface #191B2A, raised #222538, border #3A4058, text #F2F4FC, muted #B0B9D0, accent #B7A1FF, accentInk #181327; success #7EE2B5, warning #F5CF83, danger #FFADB4, info #9EC4FF. Light: #F3F4F8/#FFFFFF/#F7F7FC/#C3C7D3, text #1B2035, muted #555F77, accent #5637AF, accentInk #FFFFFF, semantic #146548/#785100/#A52C3D/#254E98. Kiểm tra tỷ lệ AA cho chữ chính/phụ/nút/badge.

Spacing 4/8/12/16/24/32; radius 10 control, 16 card, 20 modal. Control cao 40–44, bảng hàng 44, header 44. Font Segoe UI (fallback sans), số/mã Consolas (fallback monospace), thang 11/12/14/16/20/28. Nét icon đồng bộ 1.6px, không emoji. Motion 160ms cho hover nếu có; giảm chuyển động qua cinema.ui.reducedMotion và desktop property win.clientAreaAnimation. Không cần trình duyệt/CSS prefers-reduced-motion vì đây là Swing; mặc định không có chuyển động bố cục.

Theme: theme chung toàn tiến trình, toggle rõ chữ; UIManager + custom Swing UI delegates. Components: button/input/select/table/modal/badge/card/status toast/empty/loading, validated field, wrap toolbar, responsive split, poster card, seat button, report chart. Theme đổi được cả cửa sổ/modal đang mở, không đổi model hoặc callback nghiệp vụ.

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
