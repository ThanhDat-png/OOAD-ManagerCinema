-- Demo data for UC02-UC09 (safe to import once after 001-010 on an empty demo database).
START TRANSACTION;

-- 1. Bo sung ho so nhan vien mau
UPDATE NHANVIEN
SET NgaySinh = '1995-05-15', SoDienThoai = '0901234567',
    Email = 'admin@cinema.local', DiaChi = '123 Lê Lợi, Q.1, TP.HCM'
WHERE MaNV = 'NV001' AND NgaySinh IS NULL;

UPDATE NHANVIEN
SET NgaySinh = '1998-09-20', SoDienThoai = '0902345678',
    Email = 'nhanvien@cinema.local', DiaChi = '88 Nguyễn Huệ, Q.1, TP.HCM'
WHERE MaNV = 'NV002' AND NgaySinh IS NULL;

INSERT INTO NHANVIEN (MaNV, HoTen, NgaySinh, SoDienThoai, Email, DiaChi, ChucVu) VALUES
('NV005', 'Trần Minh Anh', '2000-03-10', '0903456789', 'minhanh@cinema.local', '45 Nguyễn Trãi, Q.5, TP.HCM', 'Nhân viên');

-- 2. Bo sung danh muc the loai
INSERT INTO THELOAI (MaTheLoai, TenTheLoai) VALUES
(5, 'Khoa học viễn tưởng'),
(6, 'Kinh dị');

-- 3. Phong chieu va so do ghe mau (2D + 3D)
SET @lg_thuong = (SELECT MaLoaiGhe FROM LOAIGHE WHERE TenLoaiGhe = 'Ghế thường' LIMIT 1);
SET @lg_vip    = (SELECT MaLoaiGhe FROM LOAIGHE WHERE TenLoaiGhe = 'Ghế VIP' LIMIT 1);
SET @lg_doi    = (SELECT MaLoaiGhe FROM LOAIGHE WHERE TenLoaiGhe = 'Ghế đôi' LIMIT 1);

INSERT INTO PHONGCHIEU (MaPhong, MaLoaiPhong, TenPhong, ThoiGianDonPhong, TrangThai) VALUES
('PHONG01', 1, 'Phòng 01', 15, 'Đang hoạt động'),
('PHONG02', 2, 'Phòng 02', 20, 'Đang hoạt động');

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
('PHONG01', @lg_doi,    'C', 1, 'Sẵn sàng sử dụng'),
('PHONG01', @lg_doi,    'C', 2, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 1, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 2, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 3, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_thuong, 'A', 4, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 1, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 2, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 3, 'Sẵn sàng sử dụng'),
('PHONG02', @lg_vip,    'B', 4, 'Sẵn sàng sử dụng');

-- 4. Danh muc phim mau va lien ket the loai
INSERT INTO PHIM (MaPhim, TenPhim, ThoiLuong, DaoDien, NgayKhoiChieu, GioiHanTuoi, Poster, TrangThai, DinhDangChieu) VALUES
('PHIMDEMO01', 'Hành Trình Vô Cực', 120, 'Nguyễn Quang Dũng', CURDATE(), 13, 'demo/poster-01.jpg', 'Đang chiếu', '2D'),
('PHIMDEMO02', 'Bí Ẩn Đêm Mưa', 110, 'Trần Hữu Tấn', CURDATE(), 16, 'demo/poster-02.jpg', 'Đang chiếu', '3D'),
('PHIMDEMO03', 'Mùa Hè Rực Rỡ', 95, 'Victor Vũ', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, 'demo/poster-03.jpg', 'Sắp chiếu', '2D'),
('PHIMDEMO04', 'Ký Ức Cũ', 100, 'Phan Gia Nhật Linh', DATE_SUB(CURDATE(), INTERVAL 45 DAY), 13, 'demo/poster-04.jpg', 'Ngừng chiếu', '2D');

INSERT INTO PHIM_THELOAI (MaPhim, MaTheLoai) VALUES
('PHIMDEMO01', 1), ('PHIMDEMO01', 5),
('PHIMDEMO02', 3), ('PHIMDEMO02', 6),
('PHIMDEMO03', 2), ('PHIMDEMO03', 4),
('PHIMDEMO04', 3);

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

-- 6b. Suat tuong lai (ngay mai 18:30) tai PHONG01 (2D) de demo ban ve UC08
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

-- 6c. Suat tuong lai (ngay kia 20:00) tai PHONG02 (3D) de demo ban ve 3D & lich chieu UC04/UC08
SET @day_after = DATE_ADD(CURDATE(), INTERVAL 2 DAY);
SET @td_day_after = CASE WHEN DAYOFWEEK(@day_after) IN (1, 7) THEN 2 ELSE 1 END;
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO02', 'PHONG02', @td_day_after,
        TIMESTAMP(@day_after, '20:00:00'),
        TIMESTAMPADD(MINUTE, 130, TIMESTAMP(@day_after, '20:00:00')), 'Sắp chiếu');
SET @suat_future_3d = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_future_3d, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'PHONG02';

-- 6d. Suat hom qua (19:00) tai PHONG02 (3D) de co du lieu lich su cho bao cao UC09
SET @yesterday = DATE_SUB(CURDATE(), INTERVAL 1 DAY);
SET @td_yesterday = CASE WHEN DAYOFWEEK(@yesterday) IN (1, 7) THEN 2 ELSE 1 END;
INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai)
VALUES ('PHIMDEMO02', 'PHONG02', @td_yesterday,
        TIMESTAMP(@yesterday, '19:00:00'),
        TIMESTAMPADD(MINUTE, 130, TIMESTAMP(@yesterday, '19:00:00')), 'Sắp chiếu');
SET @suat_past = LAST_INSERT_ID();

INSERT INTO GHE_SUATCHIEU (MaSuat, MaGhe, TrangThai)
SELECT @suat_past, MaGhe, IF(TrangThai = 'Sẵn sàng sử dụng', 'Trống', 'Không khả dụng')
FROM GHE WHERE MaPhong = 'PHONG02';

-- 7. Giao dich, thanh toan va ve mau cho UC07 & UC09
-- 7a. Giao dich hom nay tren suat DEMO01: A1 (Chua su dung -> test soat ve hop le), A2 (Da su dung -> test tu choi)
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

-- 7b. Giao dich hom qua tren suat PHONG02 (3D, ghe VIP B1, B2)
SELECT MaGia, GiaVe INTO @gia_past, @tien_past
FROM BANGGIAVE
WHERE MaLoaiGhe = @lg_vip AND MaLoaiThoiDiem = @td_yesterday AND MaLoaiPhong = 2
LIMIT 1;

SET @tg_past_tx = TIMESTAMP(@yesterday, '18:15:00');
INSERT INTO GIAODICH (MaGiaoDich, MaNV, ThoiGianTao, TongTien, TrangThai)
VALUES ('G_DEMO_0000000000002', 'NV002', @tg_past_tx, @tien_past * 2, 'Đã thanh toán');

INSERT INTO THANHTOAN (MaGiaoDich, PhuongThuc, SoTien, ThoiGianThanhToan)
VALUES ('G_DEMO_0000000000002', 'Chuyển khoản', @tien_past * 2, @tg_past_tx);

SET @ghe_p2_b1 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'PHONG02' AND HangGhe = 'B' AND SoGhe = 1 LIMIT 1);
SET @ghe_p2_b2 = (SELECT MaGhe FROM GHE WHERE MaPhong = 'PHONG02' AND HangGhe = 'B' AND SoGhe = 2 LIMIT 1);

UPDATE GHE_SUATCHIEU
SET MaGiaoDich = 'G_DEMO_0000000000002', TrangThai = 'Đã bán',
    ThoiGianHetHanGiu = NULL, MaGia = @gia_past, GiaVeChot = @tien_past
WHERE MaSuat = @suat_past AND MaGhe IN (@ghe_p2_b1, @ghe_p2_b2);

INSERT INTO VE (MaVe, MaGiaoDich, MaSuat, MaGhe, MaGia, MaQR, GiaVe, TrangThai, ThoiGianPhatHanh) VALUES
('VE_DEMO_003', 'G_DEMO_0000000000002', @suat_past, @ghe_p2_b1, @gia_past, 'QR-DEMO-003', @tien_past, 'Đã sử dụng', @tg_past_tx),
('VE_DEMO_004', 'G_DEMO_0000000000002', @suat_past, @ghe_p2_b2, @gia_past, 'QR-DEMO-004', @tien_past, 'Đã sử dụng', @tg_past_tx);

-- 8. Danh muc san pham F&B mau cho UC06 & UC09
INSERT INTO SANPHAM (MaSP, TenSP, MoTa, DonViTinh, GiaBan, SoLuongTon, TinhTrangKinhDoanh) VALUES
('SP001', 'Bắp rang bơ phô mai', 'Hộp bắp rang cỡ lớn vị phô mai', 'Hộp', 55000, 45, 'Đang kinh doanh'),
('SP002', 'Nước ngọt có ga', 'Ly nước ngọt có ga 700ml', 'Ly', 35000, 80, 'Đang kinh doanh'),
('SP003', 'Combo Đôi Bắp Nước', '1 hộp bắp lớn và 2 ly nước ngọt', 'Combo', 115000, 20, 'Đang kinh doanh'),
('SP004', 'Bắp caramel đặc biệt', 'Hộp bắp phủ đường caramel cỡ lớn', 'Hộp', 60000, 0, 'Đang kinh doanh'),
('SP005', 'Trà đào cam sả', 'Ly trà trái cây 500ml', 'Ly', 45000, 12, 'Ngừng kinh doanh');

COMMIT;
