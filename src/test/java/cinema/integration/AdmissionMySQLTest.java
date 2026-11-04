package cinema.integration;

import cinema.controller.admission.*;
import cinema.controller.auth.PhienDangNhap;
import cinema.controller.schedule.LichChieuBUS;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class AdmissionMySQLTest {
    private final String key=UUID.randomUUID().toString().replace("-","").substring(0,8),film="F"+key,room="R"+key,otherRoom="O"+key;
    private final String ticket="V"+UUID.randomUUID().toString().replace("-","").substring(0,19),qr="Qr"+UUID.randomUUID().toString().replace("-","").substring(0,18),tx="G"+key;
    private final int type=100000+ThreadLocalRandom.current().nextInt(1_000_000_000);
    private final VeBUS service=new VeBUS(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),new VeDAO(),new ThoiGianVaoPhong(15,15));
    private int show,otherShow;

    @BeforeEach void setup() throws Exception {
        LocalDate date=LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(30);
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO LOAIGHE VALUES (?,?)",type,"Seat "+key); execute(c,"INSERT INTO LOAIPHONG VALUES (?,?,'2D')",type,"Type "+key);
            execute(c,"INSERT INTO PHIM VALUES (?,?,90,'Test',?,0,'test','Đang chiếu','2D')",film,film,java.sql.Date.valueOf(date));
            for(String id:List.of(room,otherRoom)) {
                execute(c,"INSERT INTO PHONGCHIEU VALUES (?,?,?,15,'Đang hoạt động')",id,type,id);
                execute(c,"INSERT INTO GHE (MaPhong,MaLoaiGhe,HangGhe,SoGhe,TrangThai) VALUES (?,?,'A',1,'Sẵn sàng sử dụng'),(?,?,'A',2,'Sẵn sàng sử dụng')",id,type,id,type);
            }
            execute(c,"INSERT INTO BANGGIAVE (MaLoaiGhe,MaLoaiPhong,MaLoaiThoiDiem,GiaVe) VALUES (?,?,1,100000)",type,type);
        }
        var scheduling=new LichChieuBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new SuatChieuDAO(),Set.of());
        show=scheduling.create(film,room,date.atTime(10,0)).maSuat(); otherShow=scheduling.create(film,otherRoom,date.atTime(10,0)).maSuat();
        time(0);
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO GIAODICH VALUES (?,'NV002',NOW(),100000,'Đã thanh toán')",tx);
            execute(c,"UPDATE GHE_SUATCHIEU gs JOIN GHE g ON g.MaGhe=gs.MaGhe SET gs.MaGiaoDich=?,gs.TrangThai='Đã bán',gs.GiaVeChot=100000,gs.MaGia=(SELECT MaGia FROM BANGGIAVE WHERE MaLoaiGhe=? LIMIT 1) WHERE gs.MaSuat=? AND g.SoGhe=1",tx,type,show);
            execute(c,"INSERT INTO THANHTOAN (MaGiaoDich,PhuongThuc,SoTien,ThoiGianThanhToan) VALUES (?,'Tiền mặt',100000,NOW())",tx);
            issue(c,ticket,qr,1);
        }
    }
    @AfterEach void cleanup() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"DELETE FROM VE WHERE MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhim=?)",film);
            execute(c,"DELETE FROM GHE_SUATCHIEU WHERE MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhim=?)",film);
            execute(c,"DELETE FROM SUATCHIEU WHERE MaPhim=?",film); execute(c,"DELETE FROM THANHTOAN WHERE MaGiaoDich=?",tx); execute(c,"DELETE FROM GIAODICH WHERE MaGiaoDich=?",tx);
            for(String id:List.of(room,otherRoom)) { execute(c,"DELETE FROM GHE WHERE MaPhong=?",id); execute(c,"DELETE FROM PHONGCHIEU WHERE MaPhong=?",id); }
            execute(c,"DELETE FROM BANGGIAVE WHERE MaLoaiGhe=?",type); execute(c,"DELETE FROM LOAIGHE WHERE MaLoaiGhe=?",type);
            execute(c,"DELETE FROM LOAIPHONG WHERE MaLoaiPhong=?",type); execute(c,"DELETE FROM PHIM WHERE MaPhim=?",film);
        }
    }

    @Test void codeAndQrLookupAndValidityCheckAreReadOnly() throws Exception {
        assertEquals(ticket,service.lookup(qr,VeDAO.LoaiMa.MA_QR).ve().maVe());
        assertEquals(ticket,service.lookup(ticket,VeDAO.LoaiMa.MA_VE).ve().maVe());
        assertTrue(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        assertEquals("Chưa sử dụng",state());
    }
    @Test void missingTicketSqlTextAndWrongCaseQrDoNotMatch() {
        assertEquals("Vé không tồn tại",assertThrows(IllegalArgumentException.class,() -> service.lookup("missing",VeDAO.LoaiMa.MA_VE)).getMessage());
        assertThrows(IllegalArgumentException.class,() -> service.lookup("' OR 1=1 --",VeDAO.LoaiMa.MA_VE));
        assertThrows(IllegalArgumentException.class,() -> service.lookup(qr.toUpperCase(Locale.ROOT),VeDAO.LoaiMa.MA_QR));
    }
    @Test void explicitCodeTypeDisambiguatesTicketAndQrNamespaces() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"UPDATE GIAODICH SET TongTien=200000 WHERE MaGiaoDich=?",tx); execute(c,"UPDATE THANHTOAN SET SoTien=200000 WHERE MaGiaoDich=?",tx);
            execute(c,"UPDATE GHE_SUATCHIEU gs JOIN GHE g ON g.MaGhe=gs.MaGhe SET gs.MaGiaoDich=?,gs.TrangThai='Đã bán',gs.GiaVeChot=100000,gs.MaGia=(SELECT MaGia FROM BANGGIAVE WHERE MaLoaiGhe=? LIMIT 1) WHERE gs.MaSuat=? AND g.SoGhe=2",tx,type,show);
            issue(c,qr,"other_"+qr,2);
        }
        assertEquals(qr,service.lookup(qr,VeDAO.LoaiMa.MA_VE).ve().maVe());
        assertEquals(ticket,service.lookup(qr,VeDAO.LoaiMa.MA_QR).ve().maVe());
    }
    @Test void wrongRoomOrShowPreventsConfirmation() throws Exception {
        assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,otherRoom,show).hopLe());
        assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,otherShow).hopLe());
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,otherRoom,show));
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,otherShow));
        assertEquals("Chưa sử dụng",state());
    }
    @Test void unpaidOrMissingPaymentCannotBeUsed() throws Exception {
        try(Connection c=Database.connect()) { execute(c,"DELETE FROM THANHTOAN WHERE MaGiaoDich=?",tx); }
        assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show));
        assertEquals("Chưa sử dụng",state());
    }
    @Test void mismatchedPaymentAndSeatQuoteAreRejected() throws Exception {
        try(Connection c=Database.connect()) { execute(c,"UPDATE THANHTOAN SET SoTien=1 WHERE MaGiaoDich=?",tx); }
        assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        try(Connection c=Database.connect()) {
            execute(c,"UPDATE THANHTOAN SET SoTien=100000 WHERE MaGiaoDich=?",tx);
            execute(c,"UPDATE GHE_SUATCHIEU SET GiaVeChot=90000 WHERE MaSuat=? AND MaGiaoDich=?",show,tx);
        }
        assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show));
        assertEquals("Chưa sử dụng",state());
    }

    @Test void paidStatusDoesNotOverrideIncompleteTransactionTotal() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"UPDATE GIAODICH SET TongTien=90000 WHERE MaGiaoDich=?",tx);
            execute(c,"UPDATE THANHTOAN SET SoTien=90000 WHERE MaGiaoDich=?",tx);
        }
        assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show));
        assertEquals("Chưa sử dụng",state());
    }
    @Test void earlyAndLateEntryKeepTicketUnused() throws Exception {
        time(60); assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show));
        time(-60); assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show)); assertEquals("Chưa sử dụng",state());
    }
    @Test void cancelledShowOrInactiveRoomPreventsAdmission() throws Exception {
        try(Connection c=Database.connect()) { execute(c,"UPDATE SUATCHIEU SET TrangThai='Đã hủy' WHERE MaSuat=?",show); }
        assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show));
        try(Connection c=Database.connect()) { execute(c,"UPDATE SUATCHIEU SET TrangThai='Sắp chiếu' WHERE MaSuat=?",show); execute(c,"UPDATE PHONGCHIEU SET TrangThai='Bảo trì' WHERE MaPhong=?",room); }
        assertFalse(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe()); assertEquals("Chưa sử dụng",state());
    }
    @Test void rechecksPaymentAndTimeAtConfirmationInsteadOfTrustingEarlierCheck() throws Exception {
        assertTrue(service.check(ticket,VeDAO.LoaiMa.MA_VE,room,show).hopLe());
        try(Connection c=Database.connect()) { execute(c,"UPDATE GIAODICH SET TrangThai='Chờ thanh toán' WHERE MaGiaoDich=?",tx); }
        assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show));
        try(Connection c=Database.connect()) { execute(c,"UPDATE GIAODICH SET TrangThai='Đã thanh toán' WHERE MaGiaoDich=?",tx); }
        time(-60); assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show)); assertEquals("Chưa sử dụng",state());
    }
    @Test void confirmsOnceWithoutChangingPriceOrSeatSnapshot() throws Exception {
        var before=service.lookup(ticket,VeDAO.LoaiMa.MA_VE);
        var used=service.confirm(ticket,room,show);
        assertEquals("Đã sử dụng",used.ve().trangThai()); assertEquals(before.ve().giaVe(),used.ve().giaVe());
        assertEquals(before.ve().hangGheLucPhatHanh(),used.ve().hangGheLucPhatHanh());
        assertEquals("Vé đã được sử dụng",assertThrows(IllegalArgumentException.class,() -> service.confirm(ticket,room,show)).getMessage());
    }
    @Test void concurrentConfirmationsProduceExactlyOneSuccess() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch gate=new CountDownLatch(1);
        try {
            Callable<VeDAO.ThongTin> call=() -> { gate.await(); return service.confirm(ticket,room,show); };
            List<Future<VeDAO.ThongTin>> calls=List.of(pool.submit(call),pool.submit(call)); gate.countDown(); int successes=0;
            for(Future<VeDAO.ThongTin> result:calls) {
                try { result.get(15,TimeUnit.SECONDS); successes++; }
                catch(ExecutionException e) { assertInstanceOf(IllegalArgumentException.class,e.getCause()); assertEquals("Vé đã được sử dụng",e.getCause().getMessage()); }
            }
            assertEquals(1,successes); assertEquals("Đã sử dụng",state());
        } finally { pool.shutdownNow(); }
    }
    private String state() throws SQLException { return service.lookup(ticket,VeDAO.LoaiMa.MA_VE).ve().trangThai(); }
    private void time(int offset) throws SQLException {
        try(Connection c=Database.connect()) { execute(c,"UPDATE SUATCHIEU SET ThoiGianBatDau=TIMESTAMPADD(MINUTE,?,NOW()),ThoiGianTraPhong=TIMESTAMPADD(MINUTE,?,NOW()) WHERE MaSuat=?",offset,offset+105,show); }
    }
    private void issue(Connection c,String id,String payload,int seat) throws SQLException {
        execute(c,"INSERT INTO VE (MaVe,MaGiaoDich,MaSuat,MaGhe,MaGia,MaQR,GiaVe,TrangThai,ThoiGianPhatHanh) SELECT ?,MaGiaoDich,MaSuat,gs.MaGhe,MaGia,?,GiaVeChot,'Chưa sử dụng',NOW() FROM GHE_SUATCHIEU gs JOIN GHE g ON g.MaGhe=gs.MaGhe WHERE gs.MaSuat=? AND g.SoGhe=?",id,payload,show,seat);
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException { try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); } }
}
