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
