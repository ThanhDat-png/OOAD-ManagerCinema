-- ============================================================================
-- He thong quan ly rap chieu phim - Nhom 08 (S2_Nhom08_DB.sql)
-- MySQL 8.0+ | Gom day du schema (18 bang, 6 trigger, 22 FK) va du lieu demo
-- ============================================================================
CREATE DATABASE IF NOT EXISTS `manager_cinema` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `manager_cinema`;

-- --- 001_tai_khoan.sql ---
-- MySQL 8. Run once in an empty project database. Scope: UC01.
CREATE TABLE VAITRO (
    MaVaiTro INT PRIMARY KEY,
    TenVaiTro NVARCHAR(30) NOT NULL UNIQUE
);
CREATE TABLE NHANVIEN (
    MaNV VARCHAR(10) PRIMARY KEY,
    HoTen NVARCHAR(100) NOT NULL,
    NgaySinh DATE,
    SoDienThoai VARCHAR(15),
    Email VARCHAR(100),
    DiaChi NVARCHAR(200),
    ChucVu NVARCHAR(50)
);
CREATE TABLE TAIKHOAN (
    MaTK INT PRIMARY KEY,
    MaVaiTro INT NOT NULL,
    MaNV VARCHAR(10) UNIQUE,
    TenDangNhap VARCHAR(50) NOT NULL UNIQUE,
    MatKhauHash VARCHAR(255) NOT NULL,
    TrangThai NVARCHAR(20) NOT NULL DEFAULT 'Đang hoạt động',
    FOREIGN KEY (MaVaiTro) REFERENCES VAITRO(MaVaiTro),
    FOREIGN KEY (MaNV) REFERENCES NHANVIEN(MaNV)
);

-- --- 002_demo_uc01.sql ---
-- Demo UC01 only. Import once after 001 in an empty demo database.
START TRANSACTION;
INSERT INTO VAITRO (MaVaiTro, TenVaiTro) VALUES (1, 'Admin'), (2, 'Nhân viên');
INSERT INTO NHANVIEN (MaNV, HoTen, ChucVu) VALUES
('NV001', 'Quản trị demo', 'Admin'), ('NV002', 'Nhân viên demo', 'Nhân viên'),
('NV003', 'Tài khoản khóa demo', 'Nhân viên'), ('NV004', 'Tài khoản ngừng hoạt động demo', 'Nhân viên');
INSERT INTO TAIKHOAN (MaTK, MaVaiTro, MaNV, TenDangNhap, MatKhauHash, TrangThai) VALUES
(1, 1, 'NV001', 'admin_demo', 'pbkdf2-sha256$600000$IIOfw4Q++vry1W+QDvBh5Q==$Gzz2gpv5vbReIAJeJEqy+xvGSrdy6KPq1H81vbxv+fA=', 'Đang hoạt động'),
(2, 2, 'NV002', 'staff_demo', 'pbkdf2-sha256$600000$IIOfw4Q++vry1W+QDvBh5Q==$Gzz2gpv5vbReIAJeJEqy+xvGSrdy6KPq1H81vbxv+fA=', 'Đang hoạt động'),
(3, 2, 'NV003', 'locked_demo', 'pbkdf2-sha256$600000$IIOfw4Q++vry1W+QDvBh5Q==$Gzz2gpv5vbReIAJeJEqy+xvGSrdy6KPq1H81vbxv+fA=', 'Bị khóa'),
(4, 2, 'NV004', 'inactive_demo', 'pbkdf2-sha256$600000$IIOfw4Q++vry1W+QDvBh5Q==$Gzz2gpv5vbReIAJeJEqy+xvGSrdy6KPq1H81vbxv+fA=', 'Ngừng hoạt động');
COMMIT;

-- --- 003_giao_dich.sql ---
-- Prerequisite for UC02 history preservation; extends the project schema.
CREATE TABLE GIAODICH (
    MaGiaoDich VARCHAR(20) PRIMARY KEY,
    MaNV VARCHAR(10) NOT NULL,
    ThoiGianTao DATETIME NOT NULL,
    TongTien DECIMAL(12,0) NOT NULL,
    TrangThai NVARCHAR(40) NOT NULL,
    FOREIGN KEY (MaNV) REFERENCES NHANVIEN(MaNV) ON DELETE RESTRICT
);

-- --- 004_phim.sql ---
-- Apply once after 001–003. UC03 and schedule prerequisites from the report.
CREATE TABLE PHIM (
    MaPhim VARCHAR(10) PRIMARY KEY,
    TenPhim NVARCHAR(200) NOT NULL,
    ThoiLuong INT NOT NULL CHECK (ThoiLuong > 0),
    DaoDien NVARCHAR(100) NOT NULL,
    NgayKhoiChieu DATE NOT NULL,
    GioiHanTuoi INT NOT NULL CHECK (GioiHanTuoi >= 0),
    Poster VARCHAR(255) NOT NULL,
    TrangThai NVARCHAR(20) NOT NULL,
    DinhDangChieu NVARCHAR(20) NOT NULL
);
CREATE TABLE THELOAI (
    MaTheLoai INT PRIMARY KEY,
    TenTheLoai NVARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE PHIM_THELOAI (
    MaPhim VARCHAR(10) NOT NULL,
    MaTheLoai INT NOT NULL,
    PRIMARY KEY (MaPhim, MaTheLoai),
    FOREIGN KEY (MaPhim) REFERENCES PHIM(MaPhim) ON DELETE RESTRICT,
    FOREIGN KEY (MaTheLoai) REFERENCES THELOAI(MaTheLoai) ON DELETE RESTRICT
);
CREATE TABLE LOAIPHONG (
    MaLoaiPhong INT PRIMARY KEY,
    TenLoaiPhong NVARCHAR(50) NOT NULL UNIQUE,
    DinhDangChieu NVARCHAR(20) NOT NULL
);
CREATE TABLE PHONGCHIEU (
    MaPhong VARCHAR(10) PRIMARY KEY,
    MaLoaiPhong INT NOT NULL,
    TenPhong NVARCHAR(50) NOT NULL UNIQUE,
    ThoiGianDonPhong INT NOT NULL CHECK (ThoiGianDonPhong >= 0),
    TrangThai NVARCHAR(30) NOT NULL,
    FOREIGN KEY (MaLoaiPhong) REFERENCES LOAIPHONG(MaLoaiPhong)
);
CREATE TABLE LOAITHOIDIEM (
    MaLoaiThoiDiem INT PRIMARY KEY,
    TenLoaiThoiDiem NVARCHAR(30) NOT NULL UNIQUE
);
CREATE TABLE SUATCHIEU (
    MaSuat INT PRIMARY KEY AUTO_INCREMENT,
    MaPhim VARCHAR(10) NOT NULL,
    MaPhong VARCHAR(10) NOT NULL,
    MaLoaiThoiDiem INT NOT NULL,
    ThoiGianBatDau DATETIME NOT NULL,
    ThoiGianTraPhong DATETIME NOT NULL,
    TrangThai NVARCHAR(20) NOT NULL,
    CHECK (ThoiGianTraPhong > ThoiGianBatDau),
    FOREIGN KEY (MaPhim) REFERENCES PHIM(MaPhim) ON DELETE RESTRICT,
    FOREIGN KEY (MaPhong) REFERENCES PHONGCHIEU(MaPhong),
    FOREIGN KEY (MaLoaiThoiDiem) REFERENCES LOAITHOIDIEM(MaLoaiThoiDiem)
);
-- Coordinate with PhimDAO.stop: lock the parent before admitting a schedule write.
DELIMITER $$
CREATE TRIGGER suatchieu_phim_insert BEFORE INSERT ON SUATCHIEU FOR EACH ROW
BEGIN
    DECLARE filmStatus NVARCHAR(20);
    SELECT TrangThai INTO filmStatus FROM PHIM WHERE MaPhim=NEW.MaPhim FOR SHARE;
    IF filmStatus = 'Ngừng chiếu' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Phim đã ngừng chiếu';
    END IF;
END$$
CREATE TRIGGER suatchieu_phim_update BEFORE UPDATE ON SUATCHIEU FOR EACH ROW
BEGIN
    DECLARE filmStatus NVARCHAR(20);
    SELECT TrangThai INTO filmStatus FROM PHIM WHERE MaPhim=NEW.MaPhim FOR SHARE;
    IF filmStatus = 'Ngừng chiếu' AND NEW.TrangThai <> 'Đã hủy' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Phim đã ngừng chiếu';
    END IF;
END$$
DELIMITER ;

-- --- 005_demo_the_loai.sql ---
-- Reference data for the course demo, insert once.
INSERT INTO THELOAI (MaTheLoai, TenTheLoai) VALUES
(1, 'Hành động'), (2, 'Hài'), (3, 'Tâm lý'), (4, 'Hoạt hình');

-- --- 006_lich_chieu.sql ---
-- Apply once after 001–005. Dependencies for schedule seats and sold-ticket guards.
CREATE TABLE LOAIGHE (
    MaLoaiGhe INT PRIMARY KEY AUTO_INCREMENT,
    TenLoaiGhe NVARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE GHE (
    MaGhe INT PRIMARY KEY AUTO_INCREMENT,
    MaPhong VARCHAR(10) NOT NULL,
    MaLoaiGhe INT NOT NULL,
    HangGhe CHAR(2) NOT NULL,
    SoGhe INT NOT NULL CHECK (SoGhe > 0),
    TrangThai NVARCHAR(30) NOT NULL,
    UNIQUE (MaPhong, HangGhe, SoGhe),
    FOREIGN KEY (MaPhong) REFERENCES PHONGCHIEU(MaPhong),
    FOREIGN KEY (MaLoaiGhe) REFERENCES LOAIGHE(MaLoaiGhe)
);
CREATE TABLE BANGGIAVE (
    MaGia INT PRIMARY KEY AUTO_INCREMENT,
    MaLoaiGhe INT NOT NULL,
    MaLoaiThoiDiem INT NOT NULL,
    MaLoaiPhong INT NOT NULL,
    GiaVe DECIMAL(12,0) NOT NULL CHECK (GiaVe > 0),
    UNIQUE (MaLoaiGhe, MaLoaiThoiDiem, MaLoaiPhong),
    FOREIGN KEY (MaLoaiGhe) REFERENCES LOAIGHE(MaLoaiGhe),
    FOREIGN KEY (MaLoaiThoiDiem) REFERENCES LOAITHOIDIEM(MaLoaiThoiDiem),
    FOREIGN KEY (MaLoaiPhong) REFERENCES LOAIPHONG(MaLoaiPhong)
);
CREATE TABLE THANHTOAN (
    MaThanhToan INT PRIMARY KEY AUTO_INCREMENT,
    MaGiaoDich VARCHAR(20) NOT NULL UNIQUE,
    PhuongThuc NVARCHAR(20) NOT NULL,
    SoTien DECIMAL(12,0) NOT NULL CHECK (SoTien > 0),
    ThoiGianThanhToan DATETIME NOT NULL,
    FOREIGN KEY (MaGiaoDich) REFERENCES GIAODICH(MaGiaoDich)
);
CREATE TABLE GHE_SUATCHIEU (
    MaSuat INT NOT NULL,
    MaGhe INT NOT NULL,
    MaGiaoDich VARCHAR(20),
    TrangThai NVARCHAR(20) NOT NULL,
    ThoiGianHetHanGiu DATETIME,
    MaGia INT,
    GiaVeChot DECIMAL(12,0) CHECK (GiaVeChot > 0),
    PRIMARY KEY (MaSuat, MaGhe),
    UNIQUE (MaSuat, MaGhe, MaGiaoDich),
    CHECK (TrangThai NOT IN ('Đang giữ','Đã bán') OR MaGiaoDich IS NOT NULL),
    FOREIGN KEY (MaSuat) REFERENCES SUATCHIEU(MaSuat),
    FOREIGN KEY (MaGhe) REFERENCES GHE(MaGhe),
    FOREIGN KEY (MaGiaoDich) REFERENCES GIAODICH(MaGiaoDich),
    FOREIGN KEY (MaGia) REFERENCES BANGGIAVE(MaGia)
);
CREATE TABLE VE (
    MaVe VARCHAR(20) PRIMARY KEY,
    MaGiaoDich VARCHAR(20) NOT NULL,
    MaSuat INT NOT NULL,
    MaGhe INT NOT NULL,
    MaGia INT NOT NULL,
    MaQR VARCHAR(100) NOT NULL UNIQUE,
    GiaVe DECIMAL(12,0) NOT NULL CHECK (GiaVe > 0),
    TrangThai NVARCHAR(20) NOT NULL,
    ThoiGianPhatHanh DATETIME NOT NULL,
    UNIQUE (MaSuat, MaGhe),
    FOREIGN KEY (MaSuat, MaGhe, MaGiaoDich) REFERENCES GHE_SUATCHIEU(MaSuat, MaGhe, MaGiaoDich),
    FOREIGN KEY (MaGiaoDich) REFERENCES GIAODICH(MaGiaoDich),
    FOREIGN KEY (MaGia) REFERENCES BANGGIAVE(MaGia)
);
DELIMITER $$
CREATE TRIGGER ghe_suat_insert BEFORE INSERT ON GHE_SUATCHIEU FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT 1 FROM GHE g JOIN SUATCHIEU s ON s.MaPhong=g.MaPhong
                   WHERE g.MaGhe=NEW.MaGhe AND s.MaSuat=NEW.MaSuat) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ghe khong thuoc phong cua suat chieu';
    END IF;
END$$
CREATE TRIGGER ghe_suat_update BEFORE UPDATE ON GHE_SUATCHIEU FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT 1 FROM GHE g JOIN SUATCHIEU s ON s.MaPhong=g.MaPhong
                   WHERE g.MaGhe=NEW.MaGhe AND s.MaSuat=NEW.MaSuat) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ghe khong thuoc phong cua suat chieu';
    END IF;
END$$
DELIMITER ;

-- --- 007_demo_lich_chieu.sql ---
-- Reference data and a small demo room. Apply once; create a movie in UC03 first.
INSERT INTO LOAITHOIDIEM VALUES (1,'Ngày thường'), (2,'Cuối tuần'), (3,'Ngày lễ');
INSERT INTO LOAIPHONG VALUES (1,'Phòng 2D demo','2D');
INSERT INTO PHONGCHIEU VALUES ('DEMO01',1,'Phòng demo 01',15,'Đang hoạt động');
INSERT INTO LOAIGHE VALUES (1,'Ghế thường');
INSERT INTO GHE (MaPhong,MaLoaiGhe,HangGhe,SoGhe,TrangThai) VALUES
('DEMO01',1,'A',1,'Sẵn sàng sử dụng'),('DEMO01',1,'A',2,'Sẵn sàng sử dụng'),
('DEMO01',1,'A',3,'Sẵn sàng sử dụng'),('DEMO01',1,'A',4,'Sẵn sàng sử dụng'),
('DEMO01',1,'A',5,'Sẵn sàng sử dụng'),('DEMO01',1,'A',6,'Bảo trì');

-- --- 008_bao_toan_thong_tin_ghe.sql ---
-- Preserve issued seat labels when UC05 changes a physical seat after a show ends.
ALTER TABLE VE ADD HangGheLucPhatHanh CHAR(2), ADD SoGheLucPhatHanh INT,
    ADD TenLoaiGheLucPhatHanh NVARCHAR(50);
UPDATE VE v JOIN GHE g ON g.MaGhe=v.MaGhe JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe
SET v.HangGheLucPhatHanh=g.HangGhe,v.SoGheLucPhatHanh=g.SoGhe,v.TenLoaiGheLucPhatHanh=l.TenLoaiGhe;
ALTER TABLE VE MODIFY HangGheLucPhatHanh CHAR(2) NOT NULL,
    MODIFY SoGheLucPhatHanh INT NOT NULL, MODIFY TenLoaiGheLucPhatHanh NVARCHAR(50) NOT NULL;
DELIMITER $$
CREATE TRIGGER ve_ghe_snapshot BEFORE INSERT ON VE FOR EACH ROW
BEGIN
    DECLARE seatRow CHAR(2);
    DECLARE seatNumber INT;
    DECLARE seatType NVARCHAR(50);
    SELECT g.HangGhe,g.SoGhe,l.TenLoaiGhe INTO seatRow,seatNumber,seatType
    FROM GHE g JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe WHERE g.MaGhe=NEW.MaGhe;
    SET NEW.HangGheLucPhatHanh=seatRow;
    SET NEW.SoGheLucPhatHanh=seatNumber;
    SET NEW.TenLoaiGheLucPhatHanh=seatType;
END$$
CREATE TRIGGER ve_ghe_snapshot_immutable BEFORE UPDATE ON VE FOR EACH ROW
BEGIN
    SET NEW.HangGheLucPhatHanh=OLD.HangGheLucPhatHanh;
    SET NEW.SoGheLucPhatHanh=OLD.SoGheLucPhatHanh;
    SET NEW.TenLoaiGheLucPhatHanh=OLD.TenLoaiGheLucPhatHanh;
END$$
DELIMITER ;

-- --- 009_demo_danh_muc_phong_ghe.sql ---
-- Additional reference choices for UC05, apply once after 008.
INSERT INTO LOAIPHONG VALUES (2,'Phòng 3D demo','3D');
INSERT INTO LOAIGHE (TenLoaiGhe) VALUES ('Ghế VIP'),('Ghế đôi');

-- --- 010_san_pham.sql ---
-- UC06 catalog and stock, apply once after 009.
CREATE TABLE SANPHAM (
    MaSP VARCHAR(10) PRIMARY KEY,
    TenSP NVARCHAR(100) NOT NULL,
    MoTa NVARCHAR(255),
    DonViTinh NVARCHAR(20) NOT NULL,
    GiaBan DECIMAL(12,0) NOT NULL CHECK (GiaBan > 0),
    SoLuongTon INT NOT NULL CHECK (SoLuongTon >= 0),
    TinhTrangKinhDoanh NVARCHAR(30) NOT NULL
);

-- --- 011_demo_chuan.sql ---
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

