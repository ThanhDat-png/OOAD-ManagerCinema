package cinema.integration;

import cinema.controller.admission.*;
import cinema.controller.auth.PhienDangNhap;
import cinema.controller.sales.*;
import cinema.controller.schedule.LichChieuBUS;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class SalesMySQLTest {
    final String key=UUID.randomUUID().toString().replace("-","").substring(0,8),film="F"+key,room="R"+key;
    private final int type=100000+ThreadLocalRandom.current().nextInt(1_000_000_000);
    final List<String> ids=new ArrayList<>();
    private final BanVeDAO dao=new BanVeDAO();
    private final BanVeBUS bus=new BanVeBUS(session("NV002"),dao,5),other=new BanVeBUS(session("NV001"),dao,5);
    int show,a,b;
    static PhienDangNhap session(String employee) {return new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,new NhanVien(employee,"Test",null,null,null,null,"Nhân viên"));}
    @BeforeEach void setup() throws Exception {
        LocalDate date=LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(30);
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO LOAIGHE VALUES (?,?)",type,"Seat "+key);execute(c,"INSERT INTO LOAIPHONG VALUES (?,?,'2D')",type,"Type "+key);
            execute(c,"INSERT INTO PHIM VALUES (?,?,90,'Test',?,0,'test','Đang chiếu','2D')",film,film,java.sql.Date.valueOf(date));
            execute(c,"INSERT INTO PHONGCHIEU VALUES (?,?,?,15,'Đang hoạt động')",room,type,room);
            execute(c,"INSERT INTO GHE (MaPhong,MaLoaiGhe,HangGhe,SoGhe,TrangThai) VALUES (?,?,'A',1,'Sẵn sàng sử dụng'),(?,?,'A',2,'Sẵn sàng sử dụng')",room,type,room,type);
            for(int period:List.of(1,2,3))execute(c,"INSERT INTO BANGGIAVE (MaLoaiGhe,MaLoaiPhong,MaLoaiThoiDiem,GiaVe) VALUES (?,?,?,100000)",type,type,period);
            try(var ps=c.prepareStatement("SELECT MaGhe FROM GHE WHERE MaPhong=? ORDER BY SoGhe")){ps.setString(1,room);try(var rs=ps.executeQuery()){rs.next();a=rs.getInt(1);rs.next();b=rs.getInt(1);}}
        }
        show=new LichChieuBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new SuatChieuDAO(),Set.of()).create(film,room,date.atTime(10,0)).maSuat();
    }
    @AfterEach void cleanup() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"DELETE FROM VE WHERE MaSuat=?",show);execute(c,"DELETE FROM GHE_SUATCHIEU WHERE MaSuat=?",show);execute(c,"DELETE FROM SUATCHIEU WHERE MaPhim=?",film);
            for(String id:ids){execute(c,"DELETE FROM THANHTOAN WHERE MaGiaoDich=?",id);execute(c,"DELETE FROM GIAODICH WHERE MaGiaoDich=?",id);}
            execute(c,"DELETE FROM GHE WHERE MaPhong=?",room);execute(c,"DELETE FROM PHONGCHIEU WHERE MaPhong=?",room);
            execute(c,"DELETE FROM BANGGIAVE WHERE MaLoaiGhe=?",type);execute(c,"DELETE FROM LOAIGHE WHERE MaLoaiGhe=?",type);execute(c,"DELETE FROM LOAIPHONG WHERE MaLoaiPhong=?",type);execute(c,"DELETE FROM PHIM WHERE MaPhim=?",film);
        }
    }
    private String id(){String id=BanVeBUS.newId();ids.add(id);return id;}
    private String held(int... seats) throws Exception {String id=id();bus.hold(id,show,Arrays.stream(seats).boxed().toList());return id;}
    private String processing(int... seats) throws Exception {String id=held(seats);bus.quote(id);bus.beginPayment(id);return id;}
    private String paid(int... seats) throws Exception {String id=processing(seats);bus.complete(id,"Tiền mặt",new BigDecimal(100000L*seats.length),true);return id;}
    private void sql(String sql,Object... args)throws Exception{try(Connection c=Database.connect()){execute(c,sql,args);}}
    private int count(String table,String id)throws Exception{try(Connection c=Database.connect();var ps=c.prepareStatement("SELECT COUNT(*) FROM "+table+" WHERE MaGiaoDich=?")){ps.setString(1,id);try(var rs=ps.executeQuery()){rs.next();return rs.getInt(1);}}}
    private static void execute(Connection c,String sql,Object... args)throws SQLException{try(var ps=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);ps.executeUpdate();}}
    @Test void fiveMinuteHoldReplayDoesNotRenewLeaseOrLoseQuote() throws Exception {
        String id=held(a,b);var first=bus.inspect(id);var expiry=first.ghe().get(0).thoiGianHetHanGiu();
        assertTrue(Duration.between(first.giaoDich().thoiGianTao(),expiry).toSeconds()<=300);
        assertTrue(Duration.between(first.giaoDich().thoiGianTao(),expiry).toSeconds()>=299);
        bus.quote(id);var replay=bus.hold(id,show,List.of(a,b));assertEquals(expiry,replay.ghe().get(0).thoiGianHetHanGiu());assertEquals(0,new BigDecimal("200000").compareTo(replay.giaoDich().tongTien()));
        assertEquals(0,count("THANHTOAN",id));assertEquals(0,count("VE",id));
    }
    @Test void selectingDifferentSeatsResetsQuoteAndReleasesPreviousSeats() throws Exception {
        String id=held(a);bus.quote(id);var changed=bus.hold(id,show,List.of(b));assertEquals(0,changed.giaoDich().tongTien().signum());assertNull(changed.ghe().get(0).maGia());
        assertEquals("Trống",bus.seats(show).stream().filter(g->g.ghe().maGhe()==a).findFirst().orElseThrow().trangThai());
    }
    @Test void competingHoldsHaveOneWinner() throws Exception {
        String one=id(),two=id();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {var tasks=List.<Callable<Boolean>>of(()->{start.await();try{bus.hold(one,show,List.of(a));return true;}catch(IllegalArgumentException e){return false;}},()->{start.await();try{other.hold(two,show,List.of(a));return true;}catch(IllegalArgumentException e){return false;}});
            var f=tasks.stream().map(pool::submit).toList();start.countDown();int wins=0;for(var result:f)if(result.get(15,TimeUnit.SECONDS))wins++;assertEquals(1,wins);
        } finally {pool.shutdownNow();}
    }
    @Test void unavailableSeatRollsBackWholeReselection() throws Exception {
        String first=held(a),second=id();other.hold(second,show,List.of(b));bus.quote(first);
        assertThrows(IllegalArgumentException.class,()->bus.hold(first,show,List.of(a,b)));assertEquals(1,bus.inspect(first).ghe().size());assertEquals(0,new BigDecimal("100000").compareTo(bus.inspect(first).giaoDich().tongTien()));
    }
    @Test void pendingExpiryReleasesButInspectionIsReadOnly() throws Exception {
        String id=held(a);sql("UPDATE GHE_SUATCHIEU SET ThoiGianHetHanGiu=NOW()-INTERVAL 1 SECOND WHERE MaGiaoDich=?",id);
        assertEquals("Chờ thanh toán",bus.inspect(id).giaoDich().trangThai());assertEquals(1,bus.inspect(id).ghe().size());
        bus.seats(show);assertEquals("Hết hạn",bus.inspect(id).giaoDich().trangThai());assertTrue(bus.inspect(id).ghe().isEmpty());assertThrows(IllegalArgumentException.class,()->bus.beginPayment(id));
    }

    @Test void selectingAStaleSearchResultRechecksEligibility() throws Exception {
        assertTrue(bus.search(film,null,null).stream().anyMatch(s->s.suat().maSuat()==show));
        sql("UPDATE SUATCHIEU SET TrangThai='Đã hủy' WHERE MaSuat=?",show);
        assertThrows(IllegalArgumentException.class,()->bus.selectShow(show));assertTrue(bus.search(film,null,null).isEmpty());
    }
    @Test void deadlineCappedAtShowStart() throws Exception {
        sql("UPDATE SUATCHIEU SET ThoiGianBatDau=NOW()+INTERVAL 2 MINUTE,ThoiGianTraPhong=NOW()+INTERVAL 2 HOUR WHERE MaSuat=?",show);
        var order=bus.inspect(held(a));assertEquals(order.suat().thoiGianBatDau(),order.ghe().get(0).thoiGianHetHanGiu());
    }
    @Test void processingDoesNotExpireEvenAfterShowStartsAndCannotSilentlyCancel() throws Exception {
        String id=processing(a);sql("UPDATE GHE_SUATCHIEU SET ThoiGianHetHanGiu=NOW()-INTERVAL 1 MINUTE WHERE MaGiaoDich=?",id);
        sql("UPDATE SUATCHIEU SET ThoiGianBatDau=NOW()-INTERVAL 1 MINUTE WHERE MaSuat=?",show);
        bus.seats(show);assertEquals("Đang thanh toán",bus.inspect(id).giaoDich().trangThai());assertThrows(IllegalArgumentException.class,()->bus.cancel(id));
        bus.complete(id,"Tiền mặt",new BigDecimal("100000"),true);assertEquals(1,bus.issue(id).size());
    }
    @Test void explicitUnpaidCancellationReleasesProcessingSeats() throws Exception {
        String id=processing(a,b);bus.cancelPaymentBeforeReceivingMoney(id);assertEquals("Đã hủy",bus.inspect(id).giaoDich().trangThai());assertTrue(bus.inspect(id).ghe().isEmpty());assertEquals(0,count("THANHTOAN",id));
    }
    @Test void quoteMissingPriceRollsBackAndCurrentPriceChangesPreserveCompleteQuote() throws Exception {
        String id=held(a,b);sql("DELETE FROM BANGGIAVE WHERE MaLoaiGhe=?",type);
        assertThrows(IllegalArgumentException.class,()->bus.quote(id));assertTrue(bus.inspect(id).ghe().stream().allMatch(g->g.maGia()==null));
        int period=bus.inspect(id).suat().maLoaiThoiDiem();sql("INSERT INTO BANGGIAVE (MaLoaiGhe,MaLoaiPhong,MaLoaiThoiDiem,GiaVe) VALUES (?,?,?,100000)",type,type,period);
        bus.quote(id);sql("UPDATE BANGGIAVE SET GiaVe=150000 WHERE MaLoaiGhe=?",type);assertEquals(0,new BigDecimal("200000").compareTo(bus.quote(id).giaoDich().tongTien()));
    }
    @Test void noPaymentOrIssueBeforeValidQuoteAndConfirmation() throws Exception {
        String id=held(a);assertThrows(IllegalArgumentException.class,()->bus.beginPayment(id));assertThrows(IllegalArgumentException.class,()->bus.issue(id));bus.quote(id);bus.beginPayment(id);
        assertThrows(IllegalArgumentException.class,()->bus.complete(id,"Tiền mặt",new BigDecimal("100000"),false));
        for(String amount:List.of("99999","100001","-1","100000.5"))assertThrows(IllegalArgumentException.class,()->bus.complete(id,"Tiền mặt",new BigDecimal(amount),true));
        assertThrows(IllegalArgumentException.class,()->bus.complete(id,"missing",new BigDecimal("100000"),true));assertEquals(0,count("THANHTOAN",id));
    }
    @Test void concurrentPaymentAndIssuanceRetriesNeverDuplicate() throws Exception {
        String id=processing(a,b);var pool=Executors.newFixedThreadPool(2);
        try {
            for(var result:pool.invokeAll(List.<Callable<BanVeDAO.GioHang>>of(()->bus.complete(id,"Tiền mặt",new BigDecimal("200000"),true),()->bus.complete(id,"Tiền mặt",new BigDecimal("200000"),true))))assertEquals("Đã thanh toán",result.get().giaoDich().trangThai());
            var results=pool.invokeAll(List.<Callable<List<VeDAO.ThongTin>>>of(()->bus.issue(id),()->bus.issue(id)));assertEquals(results.get(0).get().stream().map(v->v.ve().maVe()).toList(),results.get(1).get().stream().map(v->v.ve().maVe()).toList());
        } finally {pool.shutdownNow();}
        assertEquals(1,count("THANHTOAN",id));assertEquals(2,count("VE",id));assertThrows(IllegalArgumentException.class,()->bus.complete(id,"Thẻ",new BigDecimal("200000"),true));assertThrows(IllegalArgumentException.class,()->bus.cancelPaymentBeforeReceivingMoney(id));
    }
    @Test void failedIssuanceKeepsPaymentAndCanResumeWithoutNewMoney() throws Exception {
        String id=paid(a,b);sql("UPDATE SUATCHIEU SET TrangThai='Đã hủy' WHERE MaSuat=?",show);assertThrows(IllegalArgumentException.class,()->bus.issue(id));assertEquals("Đã thanh toán",bus.inspect(id).giaoDich().trangThai());assertEquals(1,count("THANHTOAN",id));assertEquals(0,count("VE",id));
        sql("UPDATE SUATCHIEU SET TrangThai='Sắp chiếu' WHERE MaSuat=?",show);assertEquals(2,bus.issue(id).size());assertEquals(2,bus.issue(id).size());assertEquals(1,count("THANHTOAN",id));
    }

    @Test void lateUnissuedSaleProtectsSeatLabelsUntilIssuanceThenSnapshotsRemain() throws Exception {
        String id=paid(a);sql("UPDATE SUATCHIEU SET ThoiGianBatDau=NOW()-INTERVAL 3 HOUR,ThoiGianTraPhong=NOW()-INTERVAL 1 HOUR WHERE MaSuat=?",show);
        assertThrows(IllegalArgumentException.class,()->new GheDAO().updateInfo(a,"Z",9,"Sẵn sàng sử dụng"));
        var first=bus.issue(id).get(0);new GheDAO().updateInfo(a,"Z",9,"Sẵn sàng sử dụng");var reprint=bus.issue(id).get(0);
        assertEquals("A",reprint.ve().hangGheLucPhatHanh());assertEquals(1,reprint.ve().soGheLucPhatHanh());assertEquals(first.ve().maVe(),reprint.ve().maVe());
    }
    @Test void partialExistingTicketsArePreservedAndOnlyMissingTicketsIssued() throws Exception {
        String id=paid(a,b);
        sql("INSERT INTO VE (MaVe,MaGiaoDich,MaSuat,MaGhe,MaGia,MaQR,GiaVe,TrangThai,ThoiGianPhatHanh) SELECT ?,MaGiaoDich,MaSuat,MaGhe,MaGia,?,GiaVeChot,'Chưa sử dụng',NOW() FROM GHE_SUATCHIEU WHERE MaGiaoDich=? AND MaGhe=?","V"+key,"qr-"+key,id,a);
        var result=bus.issue(id);assertEquals(2,result.size());assertTrue(result.stream().anyMatch(v->v.ve().maVe().equals("V"+key)));assertEquals(1,count("THANHTOAN",id));
    }
    @Test void historyCannotPrintUnpaidOrCorruptedTickets() throws Exception {
        String id=paid(a);bus.issue(id);sql("UPDATE THANHTOAN SET SoTien=1 WHERE MaGiaoDich=?",id);
        assertEquals(1,bus.savedTickets(id).size());assertThrows(IllegalArgumentException.class,()->bus.printableTickets(id));
    }
    @Test void ownershipAndReadOnlyHistorySearch() throws Exception {
        String id=held(a);assertThrows(IllegalArgumentException.class,()->other.quote(id));assertThrows(IllegalArgumentException.class,()->other.cancel(id));assertEquals(1,other.inspect(id).ghe().size());
        assertEquals(id,other.transactions(id,null,null).get(0).giaoDich().maGiaoDich());assertThrows(IllegalArgumentException.class,()->bus.transactions("",null,null));assertThrows(IllegalArgumentException.class,()->bus.transactions("",LocalDate.now(),LocalDate.now().minusDays(1)));
        String paid=paid(b);String ticket=bus.issue(paid).get(0).ve().maVe();assertEquals(paid,bus.transactions(ticket,null,null).get(0).giaoDich().maGiaoDich());assertEquals(1,other.savedTickets(paid).size());
    }
    @Test void completedSaleIntegratesWithAdmissionAndReprintPreservesUsedState() throws Exception {
        sql("UPDATE SUATCHIEU SET ThoiGianBatDau=NOW()+INTERVAL 5 MINUTE,ThoiGianTraPhong=NOW()+INTERVAL 2 HOUR WHERE MaSuat=?",show);
        String id=paid(a);var original=bus.issue(id).get(0);assertTrue(original.daThanhToan());assertTrue(original.gheDaBan());
        var gate=new VeBUS(session("NV002"),new VeDAO(),new ThoiGianVaoPhong(15,15));assertTrue(gate.check(original.ve().maQR(),VeDAO.LoaiMa.MA_QR,room,show).hopLe());gate.confirm(original.ve().maVe(),room,show);
        var reprinted=bus.issue(id).get(0);assertEquals(original.ve().maQR(),reprinted.ve().maQR());assertEquals(original.ve().maVe(),reprinted.ve().maVe());assertEquals("Đã sử dụng",reprinted.ve().trangThai());
        javax.imageio.ImageIO.write(InVe.preview(reprinted),"png",new java.io.File("target/uc08-ticket.png"));assertEquals(1,count("THANHTOAN",id));
    }
}
