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
