# Bàn giao cuối phiên 05/11/2026

## Trạng thái đồ án

- Đã triển khai UC01–UC08 theo tài liệu đối chiếu docs/UC01.md đến docs/UC08.md.
- Đã có 17 kiểu nghiệp vụ: 16 record và enum VaiTro. Class diagram cuối cùng chưa dựng.
- UC09 vẫn là khung, chưa có xử lý hoặc màn hình thống kê hoàn chỉnh.
- Chủ đồ án đã yêu cầu chia commit tiếng Việt, dọn repo và push trong phiên này. Các phiên sau vẫn chỉ commit/push khi có quyết định mới của chủ đồ án.

## Việc bắt đầu phiên sau

1. Đọc lại mục 3.3.9 và sơ đồ liên quan trong S2_Nhom08_Lan3.docx, đối chiếu docs/doi-chieu.md và schema hiện tại.
2. UC09.1: doanh thu theo ngày, tháng, năm/khoảng ngày, số giao dịch và số vé; dùng thời gian thanh toán.
3. UC09.2: thống kê theo một/tất cả phim, dùng giá đã chốt.
4. UC09.3: thống kê theo một/tất cả nhân viên, bảo toàn người thực hiện ghi trên giao dịch.
5. UC09.4: vé theo thời gian phát hành, phim/suất và trạng thái hiện tại; F&B theo sản phẩm, trạng thái kinh doanh, còn/hết hàng.
6. Kiểm thử phân quyền Admin, thời gian biên, dữ liệu rỗng trả 0 và truy vấn chỉ đọc. Kiểm tra tránh nhân đôi tiền khi join nhiều vé; xử lý rõ giao dịch đã thanh toán nhưng chưa phát hành đủ vé.
7. Sau UC09: tổng rà soát UC01–UC09, dựng class diagram theo code, chỉnh các điểm báo cáo/CSDL chưa khớp, chuẩn bị dữ liệu và kịch bản bảo vệ, kiểm tra cài mới.

## Quyết định cần giữ

- Chỉ triển khai trong phạm vi đồ án và luôn đối chiếu báo cáo.
- Giữ ghế 5 phút, tối đa đến giờ bắt đầu suất; Đang thanh toán không tự giải phóng khi hết hạn.
- Cho vào phòng từ 15 phút trước đến 15 phút sau giờ bắt đầu.
- Thanh toán tại quầy do nhân viên xác nhận; không tích hợp cổng thanh toán.
- Báo cáo hiện chỉ yêu cầu tồn kho F&B, không có luồng bán hay doanh thu F&B.
- Không đổi giá vé lịch sử theo bảng giá mới. In lại dùng vé/QR đã lưu và giữ trạng thái đã sử dụng.
- Máy in và đầu đọc QR vật lý chưa thử trên thiết bị thực tế.

## Cấu hình và dọn dẹp

- config/*.example được lưu trong Git. database.properties, local-tools.json, calendar.properties, admission.properties và sales.properties riêng được giữ trên máy và bỏ qua bởi Git.
- Dữ liệu MySQL nằm ngoài repo; poster tại data/posters được giữ nguyên và không đưa lên Git.
- Đã bỏ 13 file package-info chỉ chứa mô tả khung ở các package đã có mã thực. Giữ package-info của revenue cho UC09.
- Đã xóa out cũ; build Maven sạch tạo lại target. target chứa báo cáo/ảnh kiểm thử và được Git bỏ qua.
- Công cụ cục bộ và file do tiện ích IDE tạo được giữ nguyên; không xem là mã nguồn để push.

## Kiểm tra trước push

Ngày 05/11/2026: Maven clean test với cinema.mysql.tests=true đạt BUILD SUCCESS; 126 kiểm thử, 0 thất bại, 0 lỗi, 0 bỏ qua. Đã bật MySQL cục bộ trước lượt chạy xác nhận. Cấu hình riêng được Git bỏ qua; không đưa target, out, dữ liệu MySQL hoặc poster cục bộ lên Git. Đã fetch origin để kiểm tra nhánh từ xa trước khi push. Nhánh bàn giao: main, remote origin của ThanhDat-png/OOAD-ManagerCinema.
