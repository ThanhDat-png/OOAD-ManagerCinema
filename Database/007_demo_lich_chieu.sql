-- Reference data and a small demo room. Apply once; create a movie in UC03 first.
INSERT INTO LOAITHOIDIEM VALUES (1,'Ngày thường'), (2,'Cuối tuần'), (3,'Ngày lễ');
INSERT INTO LOAIPHONG VALUES (1,'Phòng 2D demo','2D');
INSERT INTO PHONGCHIEU VALUES ('DEMO01',1,'Phòng demo 01',15,'Đang hoạt động');
INSERT INTO LOAIGHE VALUES (1,'Ghế thường');
INSERT INTO GHE (MaPhong,MaLoaiGhe,HangGhe,SoGhe,TrangThai) VALUES
('DEMO01',1,'A',1,'Sẵn sàng sử dụng'),('DEMO01',1,'A',2,'Sẵn sàng sử dụng'),
('DEMO01',1,'A',3,'Sẵn sàng sử dụng'),('DEMO01',1,'A',4,'Sẵn sàng sử dụng'),
('DEMO01',1,'A',5,'Sẵn sàng sử dụng'),('DEMO01',1,'A',6,'Bảo trì');
