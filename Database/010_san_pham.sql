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
