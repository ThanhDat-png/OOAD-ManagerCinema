INSERT INTO NHANVIEN (MaNV,HoTen,ChucVu) VALUES
('NV001','Nguyễn Minh Anh','Quản lý rạp'),('NV002','Trần Hoàng An','Nhân viên bán vé'),
('NV003','Lê Bảo Ngọc','Nhân viên bán vé'),('NV004','Đỗ Minh Khôi','Nhân viên soát vé');

INSERT INTO PHIM VALUES
('PH001','Mắt biếc',117,'Victor Vũ','2019-12-20',16,'','Đang chiếu','2D'),
('PH002','Song lang',101,'Leon Lê','2018-08-17',16,'','Đang chiếu','2D'),
('PH003','Tôi thấy hoa vàng trên cỏ xanh',103,'Victor Vũ','2015-10-02',0,'','Đang chiếu','2D');

INSERT INTO PHIM_THELOAI VALUES ('PH001',3),('PH001',5),('PH002',3),('PH003',3);

INSERT INTO PHONGCHIEU VALUES
('PC01',1,'Phòng 01',15,'Đang hoạt động'),('PC02',1,'Phòng 02',15,'Đang hoạt động');

WITH RECURSIVE seat_no(n) AS (VALUES(1) UNION ALL SELECT n+1 FROM seat_no WHERE n<8),
seat_row(label) AS (VALUES('A'),('B'),('C'),('D'))
INSERT INTO GHE (MaPhong, MaLoaiGhe, HangGhe, SoGhe, TrangThai)
SELECT MaPhong, CASE WHEN label='D' THEN 2 ELSE 1 END, label, n,
       CASE WHEN label='A' AND n=8 THEN 'Bảo trì' ELSE 'Sẵn sàng sử dụng' END
FROM PHONGCHIEU CROSS JOIN seat_row CROSS JOIN seat_no;

INSERT INTO BANGGIAVE (MaLoaiGhe, MaLoaiThoiDiem, MaLoaiPhong, GiaVe)
SELECT MaLoaiGhe, MaLoaiThoiDiem, MaLoaiPhong,
       65000+(MaLoaiGhe-1)*15000+(MaLoaiThoiDiem-1)*10000+(MaLoaiPhong-1)*20000
FROM LOAIGHE CROSS JOIN LOAITHOIDIEM CROSS JOIN LOAIPHONG;

WITH RECURSIVE days(n) AS (VALUES(0) UNION ALL SELECT n+1 FROM days WHERE n<13),
times(film,room,hour) AS (VALUES('PH001','PC01','17:00:00'),('PH002','PC01','20:00:00'),('PH003','PC02','19:00:00')),
starts(film,room,starts_at) AS (SELECT film,room,date('now','+7 hours','+'||n||' days')||' '||hour FROM times CROSS JOIN days)
INSERT INTO SUATCHIEU (MaPhim,MaPhong,MaLoaiThoiDiem,ThoiGianBatDau,ThoiGianTraPhong,TrangThai)
SELECT film,room,CASE WHEN strftime('%w',starts_at) IN ('0','6') THEN 2 ELSE 1 END,
       starts_at,datetime(starts_at,'+'||(p.ThoiLuong+r.ThoiGianDonPhong)||' minutes'),'Sắp chiếu'
FROM starts JOIN PHIM p ON p.MaPhim=film JOIN PHONGCHIEU r ON r.MaPhong=room;

INSERT INTO GHE_SUATCHIEU (MaSuat,MaGhe,TrangThai)
SELECT s.MaSuat,g.MaGhe,CASE WHEN g.TrangThai='Bảo trì' THEN 'Không khả dụng' ELSE 'Trống' END
FROM SUATCHIEU s JOIN GHE g ON s.MaPhong=g.MaPhong;

INSERT INTO SANPHAM VALUES
('SP001','Bắp caramel','Bắp rang phủ caramel','Hộp',45000,80,'Đang kinh doanh'),
('SP002','Nước suối','Nước suối 500 ml','Chai',20000,120,'Đang kinh doanh'),
('SP003','Trà đào','Trà đào lạnh','Ly',35000,0,'Đang kinh doanh');
