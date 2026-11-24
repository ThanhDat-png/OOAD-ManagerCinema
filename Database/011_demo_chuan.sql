-- Demo data for UC02-UC09 (safe to import once after 001-010 on an empty demo database).
START TRANSACTION;

-- 1. Bo sung ho so nhan vien mau va 1 tai khoan nhan vien thu hai
UPDATE NHANVIEN
SET NgaySinh = '1995-05-15', SoDienThoai = '0901234567',
    Email = 'admin@cinema.local', DiaChi = '123 Lê Lợi, Q.1, TP.HCM'
WHERE MaNV = 'NV001' AND NgaySinh IS NULL;

UPDATE NHANVIEN
SET NgaySinh = '1998-09-20', SoDienThoai = '0902345678',
    Email = 'nhanvien@cinema.local', DiaChi = '88 Nguyễn Huệ, Q.1, TP.HCM'
WHERE MaNV = 'NV002' AND NgaySinh IS NULL;

UPDATE NHANVIEN
SET NgaySinh = '1999-11-05', SoDienThoai = '0908881111',
    Email = 'locked@cinema.local', DiaChi = '12 Pasteur, Q.1, TP.HCM'
WHERE MaNV = 'NV003' AND NgaySinh IS NULL;

UPDATE NHANVIEN
SET NgaySinh = '1997-04-22', SoDienThoai = '0908882222',
    Email = 'inactive@cinema.local', DiaChi = '79 Hai Bà Trưng, Q.3, TP.HCM'
WHERE MaNV = 'NV004' AND NgaySinh IS NULL;

INSERT INTO NHANVIEN (MaNV, HoTen, NgaySinh, SoDienThoai, Email, DiaChi, ChucVu) VALUES
('NV005', 'Trần Minh Anh', '2000-03-10', '0903456789', 'minhanh@cinema.local', '45 Nguyễn Trãi, Q.5, TP.HCM', 'Nhân viên bán vé'),
('NV006', 'Phạm Quốc Bảo', '1999-07-18', '0904567890', 'quocbao@cinema.local', '210 Cách Mạng Tháng 8, Q.3, TP.HCM', 'Nhân viên soát vé');

INSERT INTO TAIKHOAN (MaTK, MaVaiTro, MaNV, TenDangNhap, MatKhauHash, TrangThai) VALUES
(5, 2, 'NV006', 'bao_demo', 'pbkdf2-sha256$600000$IIOfw4Q++vry1W+QDvBh5Q==$Gzz2gpv5vbReIAJeJEqy+xvGSrdy6KPq1H81vbxv+fA=', 'Đang hoạt động');

-- 2. Bo sung danh muc the loai
INSERT INTO THELOAI (MaTheLoai, TenTheLoai) VALUES
(5, 'Khoa học viễn tưởng'),
(6, 'Kinh dị');

-- 3. Phong chieu va so do ghe mau (2D + 3D + Bao tri)
SET @lg_thuong = (SELECT MaLoaiGhe FROM LOAIGHE WHERE TenLoaiGhe = 'Ghế thường' LIMIT 1);
SET @lg_vip    = (SELECT MaLoaiGhe FROM LOAIGHE WHERE TenLoaiGhe = 'Ghế VIP' LIMIT 1);
SET @lg_doi    = (SELECT MaLoaiGhe FROM LOAIGHE WHERE TenLoaiGhe = 'Ghế đôi' LIMIT 1);

INSERT INTO PHONGCHIEU (MaPhong, MaLoaiPhong, TenPhong, ThoiGianDonPhong, TrangThai) VALUES
('PHONG01', 1, 'Phòng 01', 15, 'Đang hoạt động'),
('PHONG02', 2, 'Phòng 02', 20, 'Đang hoạt động'),
('PHONG03', 1, 'Phòng 03', 15, 'Bảo trì');

INSERT INTO GHE (MaPhong, MaLoaiGhe, HangGhe, SoGhe, TrangThai) VALUES
('PHONG01', @lg_thuong, 'A', 1, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_thuong, 'A', 2, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_thuong, 'A', 3, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_thuong, 'A', 4, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_thuong, 'A', 5, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_thuong, 'A', 6, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_vip,    'B', 1, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_vip,    'B', 2, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_vip,    'B', 3, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_vip,    'B', 4, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_vip,    'B', 5, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_vip,    'B', 6, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_doi,    'C', 1, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_doi,    'C', 2, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_doi,    'C', 3, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_doi,    'C', 4, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 1, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 2, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 3, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 4, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 5, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 6, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 1, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 2, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 3, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 4, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 5, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 6, 'Sẵn sàng sử dụng'),
('PHONG03', @lg_thuong, 'A', 1, 'Bảo trì'),
('PHONG03', @lg_thuong, 'A', 2, 'Bảo trì'),
('PHONG03', @lg_thuong, 'A', 3, 'Sẵn sàng sử dụng'),
('PHONG03', @lg_thuong, 'A', 4, 'Sẵn sàng sử dụng');

-- 4. Danh muc phim mau (5 phim kem poster UUID that trong data/posters/)
INSERT INTO PHIM (MaPhim, TenPhim, ThoiLuong, DaoDien, NgayKhoiChieu, GioiHanTuoi, Poster, TrangThai, DinhDangChieu) VALUES
('PHIMDEMO01', 'Hành Trình Vô Cực', 120, 'Nguyễn Quang Dũng', CURDATE(), 13, 'data/posters/11111111-1111-4111-8111-111111111101.jpg', 'Đang chiếu', '2D'),
('PHIMDEMO02', 'Bí Ẩn Đêm Mưa', 110, 'Trần Hữu Tấn', CURDATE(), 16, 'data/posters/11111111-1111-4111-8111-111111111102.jpg', 'Đang chiếu', '3D'),
('PHIMDEMO03', 'Mùa Hè Rực Rỡ', 95, 'Victor Vũ', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, 'data/posters/11111111-1111-4111-8111-111111111103.jpg', 'Sắp chiếu', '2D'),
('PHIMDEMO04', 'Ký Ức Cũ', 100, 'Phan Gia Nhật Linh', DATE_SUB(CURDATE(), INTERVAL 45 DAY), 13, 'data/posters/11111111-1111-4111-8111-111111111104.jpg', 'Ngừng chiếu', '2D'),
('PHIMDEMO05', 'Biệt Đội Ánh Sáng', 105, 'Lê Thanh Sơn', CURDATE(), 0, 'data/posters/11111111-1111-4111-8111-111111111105.jpg', 'Đang chiếu', '3D');

INSERT INTO PHIM_THELOAI (MaPhim, MaTheLoai) VALUES
('PHIMDEMO01', 1), ('PHIMDEMO01', 5),
('PHIMDEMO02', 3), ('PHIMDEMO02', 6),
('PHIMDEMO03', 2), ('PHIMDEMO03', 4),
('PHIMDEMO04', 3),
('PHIMDEMO05', 1), ('PHIMDEMO05', 4);

-- 5. Bang gia ve day du (3 loai ghe x 2 loai phong x 3 loai thoi diem)
INSERT INTO BANGGIAVE (MaLoaiGhe, MaLoaiThoiDiem, MaLoaiPhong, GiaVe) VALUES
(@lg_thuong, 1, 1, 65000),  (@lg_vip, 1, 1, 85000),  (@lg_doi, 1, 1, 140000),
(@lg_thuong, 2, 1, 75000),  (@lg_vip, 2, 1, 95000),  (@lg_doi, 2, 1, 160000),
(@lg_thuong, 3, 1, 90000),  (@lg_vip, 3, 1, 115000), (@lg_doi, 3, 1, 190000),
(@lg_thuong, 1, 2, 90000),  (@lg_vip, 1, 2, 115000), (@lg_doi, 1, 2, 180000),
(@lg_thuong, 2, 2, 105000), (@lg_vip, 2, 2, 130000), (@lg_doi, 2, 2, 210000),
(@lg_thuong, 3, 2, 120000), (@lg_vip, 3, 2, 150000), (@lg_doi, 3, 2, 240000);

-- 6. Suat chieu demo:
-- 6a. Suat dang trong khung gio soat ve (+5 phut) tai DEMO01 de demo UC07 ngay lap tuc
SET @td_today = CASE WHEN DAYOFWEEK(NOW()) IN (1, 7) THEN 2 ELSE 1 END;
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO01', 'DEMO01', @td_today, TIMESTAMPADD(MINUTE, 5, NOW()), TIMESTAMPADD(MINUTE, 140, NOW()), 'Sắp chiếu');
SET @suat_live = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_live, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'DEMO01';

-- 6b. Suat sap chieu hom nay (+2 gio) tai PHONG01 (2D) de demo ban ve UC08 trong ngay
SET @start_today_p1 = TIMESTAMPADD(HOUR, 2, NOW());
SET @td_today_p1 = CASE WHEN DAYOFWEEK(@start_today_p1) IN (1, 7) THEN 2 ELSE 1 END;
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO01', 'PHONG01', @td_today_p1, @start_today_p1, TIMESTAMPADD(MINUTE, 135, @start_today_p1), 'Sắp chiếu');
SET @suat_today_2d = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_today_2d, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'PHONG01';

-- 6c. Suat tuong lai (ngay mai 18:30) tai PHONG01 (2D)
SET @tomorrow = DATE_ADD(CURDATE(), INTERVAL 1 DAY);
SET @td_tomorrow = CASE WHEN DAYOFWEEK(@tomorrow) IN (1, 7) THEN 2 ELSE 1 END;
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO01', 'PHONG01', @td_tomorrow,
        TIMESTAMP(@tomorrow, '18:30:00'),
        TIMESTAMPADD(MINUTE, 135, TIMESTAMP(@tomorrow, '18:30:00')), 'Sắp chiếu');
SET @suat_future_2d = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_future_2d, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'PHONG01';

-- 6d. Suat tuong lai (ngay mai 20:00) tai PHONG02 (3D) - Biet Doi Anh Sang
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO05', 'PHONG02', @td_tomorrow,
        TIMESTAMP(@tomorrow, '20:00:00'),
        TIMESTAMPADD(MINUTE, 125, TIMESTAMP(@tomorrow, '20:00:00')), 'Sắp chiếu');
SET @suat_future_3d_a = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_future_3d_a, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'PHONG02';

-- 6e. Suat tuong lai (ngay kia 19:30) tai PHONG02 (3D) - Bi An Dem Mua
SET @day_after = DATE_ADD(CURDATE(), INTERVAL 2 DAY);
SET @td_day_after = CASE WHEN DAYOFWEEK(@day_after) IN (1, 7) THEN 2 ELSE 1 END;
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO02', 'PHONG02', @td_day_after,
        TIMESTAMP(@day_after, '19:30:00'),
        TIMESTAMPADD(MINUTE, 130, TIMESTAMP(@day_after, '19:30:00')), 'Sắp chiếu');
SET @suat_future_3d_b = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_future_3d_b, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'PHONG02';

-- 6f. Suat lich su hom qua (19:00) tai PHONG02 (3D) cho bao cao UC09
SET @yesterday = DATE_SUB(CURDATE(), INTERVAL 1 DAY);
SET @td_yesterday = CASE WHEN DAYOFWEEK(@yesterday) IN (1, 7) THEN 2 ELSE 1 END;
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO02', 'PHONG02', @td_yesterday,
        TIMESTAMP(@yesterday, '19:00:00'),
        TIMESTAMPADD(MINUTE, 130, TIMESTAMP(@yesterday, '19:00:00')), 'Sắp chiếu');
SET @suat_past_3d = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_past_3d, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'PHONG02';

-- 6g. Suat lich su 2 ngay truoc (15:00) tai PHONG01 (2D) cho bao cao UC09
SET @two_days_ago = DATE_SUB(CURDATE(), INTERVAL 2 DAY);
SET @td_2d_ago = CASE WHEN DAYOFWEEK(@two_days_ago) IN (1, 7) THEN 2 ELSE 1 END;
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO01', 'PHONG01', @td_2d_ago,
        TIMESTAMP(@two_days_ago, '15:00:00'),
        TIMESTAMPADD(MINUTE, 135, TIMESTAMP(@two_days_ago, '15:00:00')), 'Sắp chiếu');
SET @suat_past_2d = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_past_2d, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'PHONG01';

-- 7. Giao dich, thanh toan va ve mau cho UC07, UC08 & UC09
-- 7a. Giao dich hom nay tren DEMO01 (Tien mat): A1 Chua su dung (VE_DEMO_001), A2 Da su dung (VE_DEMO_002)
SELECT MaGia, GiaVe INTO @gia_live, @tien_live
FROM BANGGIAVE
WHERE MaLoaiGhe = @lg_thuong AND MaLoaiThoiDiem = @td_today AND MaLoaiPhong = 1
LIMIT 1;

INSERT INTO GIAODICH (MaGiaoDich, MaNV, ThoiGianTao, TongTien, TrangThai)
VALUES ('G_DEMO_0000000000001', 'NV002', NOW(), @tien_live * 2, 'Đã thanh toán');

INSERT INTO THANHTOAN (MaGiaoDich, PhuongThuc, SoTien, ThoiGianThanhToan)
VALUES ('G_DEMO_0000000000001', 'Tiền mặt', @tien_live * 2, NOW());

SET @ghe_a1 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'DEMO01' AND HangGhe = 'A' AND SoGhe = 1 LIMIT 1);
SET @ghe_a2 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'DEMO01' AND HangGhe = 'A' AND SoGhe = 2 LIMIT 1);

UPDATE GHE_SUATCHIEU
SET MaGiaoDich = 'G_DEMO_0000000000001', TrangThai = 'Đã bán',
    ThoiGianHetHanGiu = NULL, MaGia = @gia_live, GiaVeChot = @tien_live
WHERE MaSuat = @suat_live AND MaGhe IN (@ghe_a1, @ghe_a2);

INSERT INTO VE (MaVe, MaGiaoDich, MaSuat, MaGhe, MaGia, MaQR, GiaVe, TrangThai, ThoiGianPhatHanh) VALUES
('VE_DEMO_001', 'G_DEMO_0000000000001', @suat_live, @ghe_a1, @gia_live, 'QR-DEMO-001', @tien_live, 'Chưa sử dụng', NOW()),
('VE_DEMO_002', 'G_DEMO_0000000000001', @suat_live, @ghe_a2, @gia_live, 'QR-DEMO-002', @tien_live, 'Đã sử dụng', NOW());

-- 7b. Giao dich hom qua tren PHONG02 (Chuyen khoan, NV002, 2 ghe VIP B1, B2)
SELECT MaGia, GiaVe INTO @gia_past_vip, @tien_past_vip
FROM BANGGIAVE
WHERE MaLoaiGhe = @lg_vip AND MaLoaiThoiDiem = @td_yesterday AND MaLoaiPhong = 2
LIMIT 1;

SET @tg_past_tx = TIMESTAMP(@yesterday, '18:15:00');
INSERT INTO GIAODICH (MaGiaoDich, MaNV, ThoiGianTao, TongTien, TrangThai)
VALUES ('G_DEMO_0000000000002', 'NV002', @tg_past_tx, @tien_past_vip * 2, 'Đã thanh toán');

INSERT INTO THANHTOAN (MaGiaoDich, PhuongThuc, SoTien, ThoiGianThanhToan)
VALUES ('G_DEMO_0000000000002', 'Chuyển khoản', @tien_past_vip * 2, @tg_past_tx);

SET @ghe_p2_b1 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'PHONG02' AND HangGhe = 'B' AND SoGhe = 1 LIMIT 1);
SET @ghe_p2_b2 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'PHONG02' AND HangGhe = 'B' AND SoGhe = 2 LIMIT 1);

UPDATE GHE_SUATCHIEU
SET MaGiaoDich = 'G_DEMO_0000000000002', TrangThai = 'Đã bán',
    ThoiGianHetHanGiu = NULL, MaGia = @gia_past_vip, GiaVeChot = @tien_past_vip
WHERE MaSuat = @suat_past_3d AND MaGhe IN (@ghe_p2_b1, @ghe_p2_b2);

INSERT INTO VE (MaVe, MaGiaoDich, MaSuat, MaGhe, MaGia, MaQR, GiaVe, TrangThai, ThoiGianPhatHanh) VALUES
('VE_DEMO_003', 'G_DEMO_0000000000002', @suat_past_3d, @ghe_p2_b1, @gia_past_vip, 'QR-DEMO-003', @tien_past_vip, 'Đã sử dụng', @tg_past_tx),
('VE_DEMO_004', 'G_DEMO_0000000000002', @suat_past_3d, @ghe_p2_b2, @gia_past_vip, 'QR-DEMO-004', @tien_past_vip, 'Đã sử dụng', @tg_past_tx);

-- 7c. Giao dich 2 ngay truoc tren PHONG01 (The, NV006, 2 ghe Thuong + 1 ghe Doi)
SELECT MaGia, GiaVe INTO @gia_2d_thuong, @tien_2d_thuong
FROM BANGGIAVE
WHERE MaLoaiGhe = @lg_thuong AND MaLoaiThoiDiem = @td_2d_ago AND MaLoaiPhong = 1
LIMIT 1;
SELECT MaGia, GiaVe INTO @gia_2d_doi, @tien_2d_doi
FROM BANGGIAVE
WHERE MaLoaiGhe = @lg_doi AND MaLoaiThoiDiem = @td_2d_ago AND MaLoaiPhong = 1
LIMIT 1;

SET @tg_2d_tx = TIMESTAMP(@two_days_ago, '14:20:00');
SET @tong_2d_tx = @tien_2d_thuong * 2 + @tien_2d_doi;
INSERT INTO GIAODICH (MaGiaoDich, MaNV, ThoiGianTao, TongTien, TrangThai)
VALUES ('G_DEMO_0000000000003', 'NV006', @tg_2d_tx, @tong_2d_tx, 'Đã thanh toán');

INSERT INTO THANHTOAN (MaGiaoDich, PhuongThuc, SoTien, ThoiGianThanhToan)
VALUES ('G_DEMO_0000000000003', 'Thẻ', @tong_2d_tx, @tg_2d_tx);

SET @ghe_p1_a1 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'PHONG01' AND HangGhe = 'A' AND SoGhe = 1 LIMIT 1);
SET @ghe_p1_a2 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'PHONG01' AND HangGhe = 'A' AND SoGhe = 2 LIMIT 1);
SET @ghe_p1_c1 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'PHONG01' AND HangGhe = 'C' AND SoGhe = 1 LIMIT 1);

UPDATE GHE_SUATCHIEU
SET MaGiaoDich = 'G_DEMO_0000000000003', TrangThai = 'Đã bán',
    ThoiGianHetHanGiu = NULL, MaGia = @gia_2d_thuong, GiaVeChot = @tien_2d_thuong
WHERE MaSuat = @suat_past_2d AND MaGhe IN (@ghe_p1_a1, @ghe_p1_a2);

UPDATE GHE_SUATCHIEU
SET MaGiaoDich = 'G_DEMO_0000000000003', TrangThai = 'Đã bán',
    ThoiGianHetHanGiu = NULL, MaGia = @gia_2d_doi, GiaVeChot = @tien_2d_doi
WHERE MaSuat = @suat_past_2d AND MaGhe = @ghe_p1_c1;

INSERT INTO VE (MaVe, MaGiaoDich, MaSuat, MaGhe, MaGia, MaQR, GiaVe, TrangThai, ThoiGianPhatHanh) VALUES
('VE_DEMO_005', 'G_DEMO_0000000000003', @suat_past_2d, @ghe_p1_a1, @gia_2d_thuong, 'QR-DEMO-005', @tien_2d_thuong, 'Đã sử dụng', @tg_2d_tx),
('VE_DEMO_006', 'G_DEMO_0000000000003', @suat_past_2d, @ghe_p1_a2, @gia_2d_thuong, 'QR-DEMO-006', @tien_2d_thuong, 'Đã sử dụng', @tg_2d_tx),
('VE_DEMO_007', 'G_DEMO_0000000000003', @suat_past_2d, @ghe_p1_c1, @gia_2d_doi,    'QR-DEMO-007', @tien_2d_doi,    'Đã sử dụng', @tg_2d_tx);

-- 8. Danh muc san pham F&B mau cho UC06 & UC09
INSERT INTO SANPHAM (MaSP, TenSP, MoTa, DonViTinh, GiaBan, SoLuongTon, TinhTrangKinhDoanh) VALUES
('SP001', 'Bắp rang bơ phô mai', 'Hộp bắp rang cỡ lớn vị phô mai', 'Hộp', 55000, 45, 'Đang kinh doanh'),
('SP002', 'Nước ngọt có ga', 'Ly nước ngọt có ga 700ml', 'Ly', 35000, 80, 'Đang kinh doanh'),
('SP003', 'Combo Đôi Bắp Nước', '1 hộp bắp lớn và 2 ly nước ngọt', 'Combo', 115000, 20, 'Đang kinh doanh'),
('SP004', 'Bắp caramel đặc biệt', 'Hộp bắp phủ đường caramel cỡ lớn', 'Hộp', 60000, 0, 'Đang kinh doanh'),
('SP005', 'Trà đào cam sả', 'Ly trà trái cây 500ml', 'Ly', 45000, 12, 'Ngừng kinh doanh');

COMMIT;
