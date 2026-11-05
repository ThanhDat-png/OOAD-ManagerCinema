# Bàn giao sau triển khai UC09 — 05/11/2026

## Trạng thái hiện tại

- UC01–UC09 đã có xử lý và giao diện; tài liệu đối chiếu tại docs/UC01.md đến docs/UC09.md.
- UC09 có doanh thu theo thời gian/phim/nhân viên; vé theo ngày phát hành/trạng thái; lọc tồn kho F&B. Chỉ Admin được truy cập. Xem UC09.md về khoản đã thu đang chờ phát hành và cách tính các tổng.
- Giữ 17 kiểu nghiệp vụ, các record kết quả báo cáo được thống kê riêng; class diagram cuối cùng chưa dựng.
- Chủ đồ án đã yêu cầu commit/push UC09 trong phiên này. Các phiên sau vẫn chỉ commit/push khi có quyết định mới.

## Việc tiếp theo

1. Tổng rà soát từng UC01–UC09 với báo cáo gốc, gồm luồng chính, luồng phụ, phân quyền, dữ liệu trước/sau và thông báo.
2. Chạy kịch bản xuyên suốt: phim → phòng/ghế → lịch chiếu → giá → bán vé → kiểm vé → báo cáo. Kiểm tra các trường hợp giữ ghế hết hạn, thanh toán chưa rõ, phục hồi phát hành và in lại.
3. Kiểm tra cài mới bằng SQL 001–010 trên database trống, cấu hình MySQL/JDK/Maven và hướng dẫn chuyển máy; không ghi đè database đang dùng.
4. Dựng class diagram từ code đã ổn định. Tách danh sách 17 kiểu nghiệp vụ và các lớp BUS/DAO/Swing/kiểu kết quả hỗ trợ; đối chiếu sơ đồ tuần tự và mô tả bảng còn thiếu/sai của báo cáo.
5. Chuẩn bị dữ liệu demo, hướng dẫn chạy và kịch bản bảo vệ; ghi nhận giới hạn phần cứng còn cần thử.
6. Cuối phiên tổng kết thay đổi, kiểm thử và kế hoạch. Không tự commit/push nếu chưa được yêu cầu.

## Quyết định cần giữ

- Luôn trong phạm vi đồ án và đối chiếu S2_Nhom08_Lan3.docx.
- Giữ ghế 5 phút, không vượt giờ bắt đầu; Đang thanh toán không tự hết hạn.
- Vào phòng từ 15 phút trước đến 15 phút sau giờ bắt đầu.
- Thanh toán tại quầy do nhân viên xác nhận; không tích hợp cổng ngân hàng.
- F&B chỉ quản lý/thống kê tồn kho hiện tại, chưa có bán hay doanh thu F&B.
- Giá lịch sử không đổi theo bảng giá mới. In lại dùng vé/QR đã lưu và giữ trạng thái đã sử dụng.
- Báo cáo không làm thay đổi dữ liệu, không tự dọn ghế hoặc phát hành vé; thời gian/nhân viên ghi nhận tiền đã thanh toán, theo phim cộng giá VE đã phát hành.
- Máy in và đầu đọc QR vật lý chưa thử trên thiết bị thực tế.

## Cấu hình và kiểm thử

Cấu hình riêng và dữ liệu cục bộ vẫn bỏ qua bởi Git. Schema không đổi sau UC09. Tệp mẫu cấu hình và hướng dẫn thuộc repo; target chứa build, báo cáo/ảnh kiểm thử và không push. Dữ liệu kiểm thử dùng mã riêng, được dọn sau từng ca. Lệnh kiểm tra: mvn clean test '-Dcinema.mysql.tests=true'.

Kết quả xác nhận ngày 05/11/2026: Maven clean test với cinema.mysql.tests=true đạt BUILD SUCCESS; 144 kiểm thử, 0 thất bại, 0 lỗi, 0 bỏ qua. UC09 bổ sung 18 kiểm thử (4 quy tắc, 12 MySQL, 2 desktop). Đã rà soát ảnh cả bốn tab và hai lựa chọn Vé/F&B. Chủ đồ án đã yêu cầu bàn giao UC09 qua GitHub sau khi xác nhận kiểm thử.
