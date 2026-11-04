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
