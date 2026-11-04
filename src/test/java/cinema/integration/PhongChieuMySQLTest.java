package cinema.integration;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.room.PhongChieuBUS;
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
class PhongChieuMySQLTest {
    private final String key=UUID.randomUUID().toString().replace("-","").substring(0,8),film="F"+key;
    private final int type=100000+ThreadLocalRandom.current().nextInt(1_000_000_000);
    private final List<String> rooms=new ArrayList<>(),transactions=new ArrayList<>();
    private final PhienDangNhap session=new PhienDangNhap(1,"test_admin",VaiTro.ADMIN,null);
    private final PhongChieuBUS service=new PhongChieuBUS(session,new PhongChieuDAO(),new GheDAO());
    private final LichChieuBUS schedule=new LichChieuBUS(session,new SuatChieuDAO(),Set.of());
    private PhongChieu room;
    private Ghe seat;
    private final LocalDateTime start=LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(30).atTime(10,0);

    @BeforeEach void setup() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO LOAIPHONG VALUES (?,?,'3D')",type,"3D "+key);
            execute(c,"INSERT INTO LOAIGHE VALUES (?,?),(?,?)",type,"Thường "+key,type+1,"VIP "+key);
            execute(c,"INSERT INTO PHIM VALUES (?,?,90,'Test',?,0,'test','Đang chiếu','2D')",film,film,java.sql.Date.valueOf(start.toLocalDate()));
            execute(c,"INSERT INTO BANGGIAVE (MaLoaiGhe,MaLoaiThoiDiem,MaLoaiPhong,GiaVe) VALUES (?,1,1,100000)",type);
        }
        room=create("Room %_ "+key,"Đang hoạt động");
        seat=service.addSeat(room.maPhong()," a ",1,type,"Sẵn sàng sử dụng");
    }
    @AfterEach void cleanup() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"DELETE FROM VE WHERE MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhim=?)",film);
            execute(c,"DELETE FROM GHE_SUATCHIEU WHERE MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhim=?)",film);
            execute(c,"DELETE FROM SUATCHIEU WHERE MaPhim=?",film);
            for(String tx:transactions) { execute(c,"DELETE FROM THANHTOAN WHERE MaGiaoDich=?",tx); execute(c,"DELETE FROM GIAODICH WHERE MaGiaoDich=?",tx); }
            for(String id:rooms) { execute(c,"DELETE FROM GHE WHERE MaPhong=?",id); execute(c,"DELETE FROM PHONGCHIEU WHERE MaPhong=?",id); }
            execute(c,"DELETE FROM BANGGIAVE WHERE MaLoaiGhe IN (?,?)",type,type+1);
            execute(c,"DELETE FROM LOAIGHE WHERE MaLoaiGhe IN (?,?)",type,type+1);
            execute(c,"DELETE FROM LOAIPHONG WHERE MaLoaiPhong=?",type);
            execute(c,"DELETE FROM PHIM WHERE MaPhim=?",film);
        }
    }
    private PhongChieu create(String name,String state) throws SQLException {
        PhongChieu added=service.create(name,1,15,state); rooms.add(added.maPhong()); return added;
    }
    private SuatChieu show() throws SQLException { return schedule.create(film,room.maPhong(),start); }

    @Test void createSearchAndDuplicateNameAreChecked() throws Exception {
        assertEquals(10,room.maPhong().length());
        assertEquals(1,service.search(room.maPhong(),1,"Đang hoạt động").size());
        assertTrue(service.search("%_",null,null).stream().anyMatch(r -> r.maPhong().equals(room.maPhong())));
        assertTrue(service.search("' OR 1=1 --",null,null).isEmpty());
        assertFalse(service.search("",null,null).isEmpty());
        assertThrows(IllegalArgumentException.class,() -> service.create(room.tenPhong(),1,15,"Đang hoạt động"));
        assertThrows(IllegalArgumentException.class,() -> service.create(" ",1,15,"Đang hoạt động"));
    }

    @Test void activationRequiresUsableSeatAndClosedRoomCannotBeScheduled() throws Exception {
        PhongChieu empty=create("Empty "+key,"Bảo trì");
        assertThrows(IllegalArgumentException.class,() -> service.updateState(empty.maPhong(),"Đang hoạt động"));
        service.addSeat(empty.maPhong(),"A",1,type,"Sẵn sàng sử dụng");
        service.updateState(empty.maPhong(),"Đang hoạt động");
        service.updateState(empty.maPhong(),"Tạm ngừng hoạt động");
        assertThrows(IllegalArgumentException.class,() -> schedule.create(film,empty.maPhong(),start));
    }

    @Test void liveShowsBlockTypeCleaningAndClosureButAllowRename() throws Exception {
        SuatChieu show=show();
        service.updateInfo(room.maPhong(),"Renamed "+key,1,15);
        assertThrows(IllegalArgumentException.class,() -> service.updateInfo(room.maPhong(),"Changed",type,15));
        assertThrows(IllegalArgumentException.class,() -> service.updateInfo(room.maPhong(),"Changed",1,30));
        assertThrows(IllegalArgumentException.class,() -> service.updateState(room.maPhong(),"Bảo trì"));
        assertEquals("Renamed "+key,service.detail(room.maPhong()).tenPhong());
        schedule.cancel(show.maSuat());
        service.updateInfo(room.maPhong(),"3D room "+key,type,30); service.updateState(room.maPhong(),"Bảo trì");
        assertEquals(type,service.detail(room.maPhong()).loaiPhong().maLoaiPhong());
    }

    @Test void addsSeatToLiveUnsoldShowAndRejectsDuplicateWithoutPartialWrite() throws Exception {
        SuatChieu show=show();
        Ghe added=service.addSeat(room.maPhong(),"A",2,type,"Bảo trì");
        assertEquals(2,service.seats(room.maPhong()).size());
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGhe=? AND TrangThai='Không khả dụng'",show.maSuat(),added.maGhe()));
        assertThrows(IllegalArgumentException.class,() -> service.addSeat(room.maPhong(),"A",2,type,"Sẵn sàng sử dụng"));
        assertThrows(IllegalArgumentException.class,() -> service.addSeat(room.maPhong(),"???",0,type,"Sẵn sàng sử dụng"));
        assertEquals(2,service.seats(room.maPhong()).size());
        assertEquals(2,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=?",show.maSuat()));
    }

    @Test void maintenanceAndReactivationUpdateOnlyLiveUnsoldSeats() throws Exception {
        SuatChieu show=show();
        service.updateSeat(seat.maGhe(),"A",1,"Bảo trì");
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND TrangThai='Không khả dụng'",show.maSuat()));
        service.updateSeat(seat.maGhe(),"A",1,"Sẵn sàng sử dụng");
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND TrangThai='Trống'",show.maSuat()));
    }

    @Test void changingHeldSeatCancelsPendingTransactionAndClearsQuote() throws Exception {
        SuatChieu show=show(); String tx=hold(show,"Chờ thanh toán");
        service.classifySeat(seat.maGhe(),type+1);
        assertEquals(1,count("SELECT COUNT(*) FROM GIAODICH WHERE MaGiaoDich=? AND TrangThai='Đã hủy'",tx));
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich IS NULL AND GiaVeChot IS NULL AND TrangThai='Trống'",show.maSuat()));
    }


    @Test void failedSeatMoveRollsBackReservationCancellation() throws Exception {
        service.addSeat(room.maPhong(),"A",2,type,"Sẵn sàng sử dụng");
        SuatChieu show=show(); String tx=hold(show,"Chờ thanh toán");
        assertThrows(IllegalArgumentException.class,() -> service.updateSeat(seat.maGhe(),"A",2,"Sẵn sàng sử dụng"));
        assertEquals(1,service.seat(seat.maGhe()).soGhe());
        assertEquals(1,count("SELECT COUNT(*) FROM GIAODICH WHERE MaGiaoDich=? AND TrangThai='Chờ thanh toán'",tx));
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich=? AND TrangThai='Đang giữ' AND GiaVeChot=100000",show.maSuat(),tx));
    }

    @Test void liveSaleBlocksStructureAndSoldSeatButAllowsMaintainingAnotherSeat() throws Exception {
        Ghe other=service.addSeat(room.maPhong(),"A",2,type,"Sẵn sàng sử dụng");
        SuatChieu show=show(); issue(show);
        assertThrows(IllegalArgumentException.class,() -> service.addSeat(room.maPhong(),"A",3,type,"Sẵn sàng sử dụng"));
        assertThrows(IllegalArgumentException.class,() -> service.updateSeat(other.maGhe(),"B",2,"Sẵn sàng sử dụng"));
        assertThrows(IllegalArgumentException.class,() -> service.classifySeat(other.maGhe(),type+1));
        assertThrows(IllegalArgumentException.class,() -> service.updateSeat(seat.maGhe(),"A",1,"Bảo trì"));
        service.updateSeat(other.maGhe(),"A",2,"Bảo trì");
        assertEquals(1,count("SELECT COUNT(*) FROM VE WHERE MaSuat=?",show.maSuat()));
    }

    @Test void paymentInProgressBlocksSeatChangeWithoutTouchingHold() throws Exception {
        SuatChieu show=show(); String tx=hold(show,"Đang thanh toán");
        assertThrows(IllegalArgumentException.class,() -> service.updateSeat(seat.maGhe(),"A",1,"Bảo trì"));
        assertThrows(IllegalArgumentException.class,() -> service.addSeat(room.maPhong(),"A",2,type,"Sẵn sàng sử dụng"));
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich=? AND TrangThai='Đang giữ'",show.maSuat(),tx));
    }

    @Test void endedTicketKeepsOriginalSeatLabelsAndPriceAfterPhysicalSeatChanges() throws Exception {
        SuatChieu show=show(); String ticket=issue(show);
        try(Connection c=Database.connect()) { execute(c,"UPDATE SUATCHIEU SET ThoiGianBatDau=DATE_SUB(NOW(),INTERVAL 2 HOUR),ThoiGianTraPhong=DATE_SUB(NOW(),INTERVAL 1 HOUR) WHERE MaSuat=?",show.maSuat()); }
        service.updateSeat(seat.maGhe(),"B",3,"Sẵn sàng sử dụng"); service.classifySeat(seat.maGhe(),type+1);
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("SELECT * FROM VE WHERE MaVe=?")) {
            ps.setString(1,ticket);
            try(ResultSet rs=ps.executeQuery()) {
                assertTrue(rs.next()); assertEquals("A",rs.getString("HangGheLucPhatHanh")); assertEquals(1,rs.getInt("SoGheLucPhatHanh"));
                assertEquals("Thường "+key,rs.getString("TenLoaiGheLucPhatHanh")); assertEquals(100000,rs.getInt("GiaVe"));
            }
            execute(c,"UPDATE VE SET SoGheLucPhatHanh=99 WHERE MaVe=?",ticket);
        }
        assertEquals(1,count("SELECT COUNT(*) FROM VE WHERE MaVe=? AND SoGheLucPhatHanh=1",ticket));
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND TrangThai='Đã bán'",show.maSuat()));
    }

    @Test void staffCanReadRoomAndMapButCannotMutate() throws Exception {
        PhongChieuBUS staff=new PhongChieuBUS(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),new PhongChieuDAO(),new GheDAO());
        assertEquals(1,staff.search(room.maPhong(),null,null).size()); assertEquals(1,staff.seats(room.maPhong()).size());
        assertThrows(IllegalArgumentException.class,() -> staff.create("No",1,15,"Đang hoạt động"));
        assertThrows(IllegalArgumentException.class,() -> staff.updateInfo(room.maPhong(),"No",1,15));
        assertThrows(IllegalArgumentException.class,() -> staff.updateState(room.maPhong(),"Bảo trì"));
        assertThrows(IllegalArgumentException.class,() -> staff.addSeat(room.maPhong(),"A",2,type,"Sẵn sàng sử dụng"));
        assertThrows(IllegalArgumentException.class,() -> staff.updateSeat(seat.maGhe(),"B",1,"Bảo trì"));
        assertThrows(IllegalArgumentException.class,() -> staff.classifySeat(seat.maGhe(),type+1));
    }

    @Test void concurrentDuplicateSeatsAreSerializedAndOnlyOneIsAdded() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch gate=new CountDownLatch(1);
        try {
            Callable<Ghe> task=() -> { gate.await(); return service.addSeat(room.maPhong(),"A",2,type,"Sẵn sàng sử dụng"); };
            var first=pool.submit(task); var second=pool.submit(task); gate.countDown(); int succeeded=0;
            for(Future<Ghe> result:List.of(first,second)) {
                try { result.get(15,TimeUnit.SECONDS); succeeded++; }
                catch(ExecutionException e) { assertInstanceOf(IllegalArgumentException.class,e.getCause()); }
            }
            assertEquals(1,succeeded); assertEquals(2,service.seats(room.maPhong()).size());
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentScheduleAndMaintenanceKeepSeatAvailabilityConsistent() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch gate=new CountDownLatch(1);
        try {
            Future<?> creating=pool.submit(() -> { gate.await(); return show(); });
            Future<?> maintaining=pool.submit(() -> { gate.await(); return service.updateSeat(seat.maGhe(),"A",1,"Bảo trì"); }); gate.countDown();
            maintaining.get(15,TimeUnit.SECONDS);
            try { creating.get(15,TimeUnit.SECONDS); } catch(ExecutionException e) { assertInstanceOf(IllegalArgumentException.class,e.getCause()); }
            assertEquals("Bảo trì",service.seat(seat.maGhe()).trangThai());
            assertEquals(0,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaGhe=? AND TrangThai='Trống'",seat.maGhe()));
        } finally { pool.shutdownNow(); }
    }

    private String hold(SuatChieu show,String state) throws Exception {
        String tx="T"+UUID.randomUUID().toString().replace("-","").substring(0,19); transactions.add(tx);
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO GIAODICH VALUES (?,'NV001',NOW(),100000,?)",tx,state);
            execute(c,"UPDATE GHE_SUATCHIEU SET MaGiaoDich=?,TrangThai='Đang giữ',GiaVeChot=100000,ThoiGianHetHanGiu=DATE_ADD(NOW(),INTERVAL 5 MINUTE) WHERE MaSuat=? AND MaGhe=?",tx,show.maSuat(),seat.maGhe());
        }
        return tx;
    }
    private String issue(SuatChieu show) throws Exception {
        String tx=hold(show,"Đã thanh toán");
        try(Connection c=Database.connect()) {
            execute(c,"UPDATE GHE_SUATCHIEU SET TrangThai='Đã bán',MaGia=(SELECT MaGia FROM BANGGIAVE WHERE MaLoaiGhe=? LIMIT 1) WHERE MaSuat=? AND MaGhe=?",type,show.maSuat(),seat.maGhe());
            execute(c,"INSERT INTO THANHTOAN (MaGiaoDich,PhuongThuc,SoTien,ThoiGianThanhToan) VALUES (?,'Tiền mặt',100000,NOW())",tx);
            execute(c,"INSERT INTO VE (MaVe,MaGiaoDich,MaSuat,MaGhe,MaGia,MaQR,GiaVe,TrangThai,ThoiGianPhatHanh) SELECT ?,MaGiaoDich,MaSuat,MaGhe,MaGia,?,GiaVeChot,'Chưa sử dụng',NOW() FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGhe=?",tx,tx,show.maSuat(),seat.maGhe());
        }
        return tx;
    }
    private static int count(String sql,Object... args) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(sql)) {
            bind(ps,args); try(ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException { try(PreparedStatement ps=c.prepareStatement(sql)) { bind(ps,args); ps.executeUpdate(); } }
    private static void bind(PreparedStatement ps,Object... args) throws SQLException { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); }
}
