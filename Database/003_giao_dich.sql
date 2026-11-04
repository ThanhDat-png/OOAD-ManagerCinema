-- Prerequisite for UC02 history preservation; extends the project schema.
CREATE TABLE GIAODICH (
    MaGiaoDich VARCHAR(20) PRIMARY KEY,
    MaNV VARCHAR(10) NOT NULL,
    ThoiGianTao DATETIME NOT NULL,
    TongTien DECIMAL(12,0) NOT NULL,
    TrangThai NVARCHAR(40) NOT NULL,
    FOREIGN KEY (MaNV) REFERENCES NHANVIEN(MaNV) ON DELETE RESTRICT
);
