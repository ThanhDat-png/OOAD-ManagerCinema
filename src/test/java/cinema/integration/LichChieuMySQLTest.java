package cinema.integration;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.schedule.LichChieuBUS;
import cinema.dao.SuatChieuDAO;
import cinema.db.Database;
import cinema.model.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class LichChieuMySQLTest {
    private final String key=UUID.randomUUID().toString().replace("-","").substring(0,7);
    private final String film="F"+key, longFilm="L"+key, film3d="D"+key;
    private final String roomA="A"+key, roomB="B"+key, room3d="C"+key, emptyRoom="E"+key, inactiveRoom="I"+key;
    private final int type=100000+ThreadLocalRandom.current().nextInt(1_000_000_000);
    private final List<String> transactions=new ArrayList<>();
    private final PhienDangNhap adminSession=new PhienDangNhap(1,"test_admin",VaiTro.ADMIN,null);
    private final SuatChieuDAO dao=new SuatChieuDAO();
    private final LichChieuBUS service=new LichChieuBUS(adminSession,dao,Set.of());
    private LocalDateTime start;

    @BeforeEach void setup() throws Exception {
        try(Connection c=Database.connect()) {
            try(PreparedStatement ps=c.prepareStatement("SELECT NOW()");ResultSet rs=ps.executeQuery()) {
                rs.next(); start=rs.getTimestamp(1).toLocalDateTime().plusDays(30).withHour(10).withMinute(0).withSecond(0).withNano(0);
            }
            for(String id:List.of(film,longFilm,film3d)) execute(c,"INSERT INTO PHIM VALUES (?,?,?,'Test',?,0,'test','Đang chiếu',?)",
                    id,id,id.equals(longFilm)?150:90,java.sql.Date.valueOf(start.toLocalDate()),id.equals(film3d)?"3D":"2D");
            execute(c,"INSERT INTO LOAIPHONG VALUES (?,?,'2D'),(?,?,'3D')",type,"Type "+key,type+1,"3D "+key);
            execute(c,"INSERT INTO LOAIGHE VALUES (?,?)",type,"Seat "+key);
            for(String room:List.of(roomA,roomB,room3d,emptyRoom,inactiveRoom)) {
                execute(c,"INSERT INTO PHONGCHIEU VALUES (?,?,?,?,?)",room,room.equals(room3d)?type+1:type,room,
                        room.equals(roomB)?30:15,room.equals(inactiveRoom)?"Bảo trì":"Đang hoạt động");
                if(!room.equals(emptyRoom)) {
                    execute(c,"INSERT INTO GHE (MaPhong,MaLoaiGhe,HangGhe,SoGhe,TrangThai) VALUES (?,?,'A',1,'Sẵn sàng sử dụng'),(?,?,'A',2,'Bảo trì')",room,type,room,type);
                }
            }
            execute(c,"INSERT INTO BANGGIAVE (MaLoaiGhe,MaLoaiThoiDiem,MaLoaiPhong,GiaVe) VALUES (?,1,?,100000)",type,type);
        }
    }

    @AfterEach void cleanup() throws Exception {
        try(Connection c=Database.connect()) {
            for(String table:List.of("VE","GHE_SUATCHIEU")) execute(c,"DELETE FROM "+table+" WHERE MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhim IN (?,?,?))",film,longFilm,film3d);
            execute(c,"DELETE FROM SUATCHIEU WHERE MaPhim IN (?,?,?)",film,longFilm,film3d);
            for(String tx:transactions) { execute(c,"DELETE FROM THANHTOAN WHERE MaGiaoDich=?",tx); execute(c,"DELETE FROM GIAODICH WHERE MaGiaoDich=?",tx); }
            execute(c,"DELETE FROM BANGGIAVE WHERE MaLoaiPhong IN (?,?)",type,type+1);
            for(String room:List.of(roomA,roomB,room3d,emptyRoom,inactiveRoom)) {
                execute(c,"DELETE FROM GHE WHERE MaPhong=?",room); execute(c,"DELETE FROM PHONGCHIEU WHERE MaPhong=?",room);
            }
            execute(c,"DELETE FROM LOAIGHE WHERE MaLoaiGhe=?",type);
            execute(c,"DELETE FROM LOAIPHONG WHERE MaLoaiPhong IN (?,?)",type,type+1);
            execute(c,"DELETE FROM PHIM WHERE MaPhim IN (?,?,?)",film,longFilm,film3d);
        }
    }

    @Test void createsSeatsAndIncludesCleaningTime() throws Exception {
        SuatChieu show=service.create(film,roomA,start);
        assertEquals(start.plusMinutes(105),show.thoiGianTraPhong());
        assertEquals(2,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=?",show.maSuat()));
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND TrangThai='Trống'",show.maSuat()));
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND TrangThai='Không khả dụng'",show.maSuat()));
        assertEquals(1,service.list(start.toLocalDate(),roomA).size());
    }

    @Test void rejectsOverlapButAllowsExactRoomReturnBoundary() throws Exception {
        SuatChieu first=service.create(film,roomA,start);
        assertEquals("Lịch chiếu bị trùng",assertThrows(IllegalArgumentException.class,
                () -> service.create(film,roomA,start.plusMinutes(104))).getMessage());
        service.create(film,roomA,first.thoiGianTraPhong());
        assertThrows(IllegalArgumentException.class,() -> service.changeTime(first.maSuat(),start.plusMinutes(30).toLocalTime()));
        assertEquals(start,service.detail(first.maSuat()).thoiGianBatDau());
    }

    @Test void detectsOverlapAcrossMidnight() throws Exception {
        LocalDateTime late=start.withHour(23).withMinute(30);
        service.create(film,roomA,late);
        assertThrows(IllegalArgumentException.class,() -> service.create(film,roomA,late.plusMinutes(60)));
    }

    @Test void rejectsInactiveEmptyIncompatibleStoppedFilmAndPastStart() throws Exception {
        assertThrows(IllegalArgumentException.class,() -> service.create(film,inactiveRoom,start));
        assertThrows(IllegalArgumentException.class,() -> service.create(film,emptyRoom,start));
        assertThrows(IllegalArgumentException.class,() -> service.create(film,room3d,start));
        assertThrows(IllegalArgumentException.class,() -> service.create(film,roomA,start.minusDays(60)));
        try(Connection c=Database.connect()) { execute(c,"UPDATE PHIM SET TrangThai='Ngừng chiếu' WHERE MaPhim=?",film); }
        assertThrows(IllegalArgumentException.class,() -> service.create(film,roomA,start));
        assertEquals(0,count("SELECT COUNT(*) FROM SUATCHIEU WHERE MaPhim=?",film));
    }

    @Test void changingFilmRecomputesEndAndChecksOverlapWithoutComparingToItself() throws Exception {
        SuatChieu first=service.create(film,roomA,start);
        SuatChieu next=service.create(film,roomA,start.plusMinutes(120));
        assertThrows(IllegalArgumentException.class,() -> service.changeFilm(first.maSuat(),longFilm));
        assertEquals(film,service.detail(first.maSuat()).maPhim());
        service.cancel(next.maSuat());
        assertEquals(start.plusMinutes(165),service.changeFilm(first.maSuat(),longFilm).thoiGianTraPhong());
    }

    @Test void dateTimeChangesRecomputePeriodAndHolidayOverridesWeekend() throws Exception {
        LocalDate weekday=start.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        SuatChieu show=service.create(film,roomA,weekday.atTime(10,0));
        assertEquals(1,show.maLoaiThoiDiem());
        LocalDate saturday=weekday.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        assertEquals(2,service.changeDate(show.maSuat(),saturday).maLoaiThoiDiem());
        LichChieuBUS holidayService=new LichChieuBUS(adminSession,dao,Set.of(saturday.plusWeeks(1)));
        SuatChieu holiday=holidayService.changeDate(show.maSuat(),saturday.plusWeeks(1));
        assertEquals(3,holiday.maLoaiThoiDiem());
        SuatChieu later=holidayService.changeTime(show.maSuat(),LocalTime.of(23,30));
        assertEquals(later.thoiGianBatDau().plusMinutes(105),later.thoiGianTraPhong());
        assertEquals(3,later.maLoaiThoiDiem());
    }

    @Test void roomChangeCancelsUnpaidHoldAndRebuildsSeats() throws Exception {
        SuatChieu show=service.create(film,roomA,start);
        String tx=hold(show,"Chờ thanh toán");
        SuatChieu moved=service.changeRoom(show.maSuat(),roomB);
        assertEquals(roomB,moved.maPhong()); assertEquals(start.plusMinutes(120),moved.thoiGianTraPhong());
        assertEquals(2,count("SELECT COUNT(*) FROM GHE_SUATCHIEU s JOIN GHE g ON g.MaGhe=s.MaGhe WHERE s.MaSuat=? AND g.MaPhong=?",show.maSuat(),roomB));
        assertEquals(1,count("SELECT COUNT(*) FROM GIAODICH WHERE MaGiaoDich=? AND TrangThai='Đã hủy'",tx));
        assertEquals(0,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich IS NOT NULL",show.maSuat()));
    }

    @Test void failedRoomChangeKeepsScheduleAndHoldUnchanged() throws Exception {
        SuatChieu show=service.create(film,roomA,start); service.create(film,roomB,start);
        String tx=hold(show,"Chờ thanh toán");
        assertThrows(IllegalArgumentException.class,() -> service.changeRoom(show.maSuat(),roomB));
        assertEquals(roomA,service.detail(show.maSuat()).maPhong());
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich=? AND TrangThai='Đang giữ'",show.maSuat(),tx));
        assertEquals(1,count("SELECT COUNT(*) FROM GIAODICH WHERE MaGiaoDich=? AND TrangThai='Chờ thanh toán'",tx));
    }

    @Test void cancelReleasesRoomAndHoldsButKeepsScheduleHistory() throws Exception {
        SuatChieu show=service.create(film,roomA,start); String tx=hold(show,"Chờ thanh toán");
        service.cancel(show.maSuat());
        assertEquals("Đã hủy",service.detail(show.maSuat()).trangThai());
        assertEquals(2,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND TrangThai='Không khả dụng'",show.maSuat()));
        assertEquals(1,count("SELECT COUNT(*) FROM GIAODICH WHERE MaGiaoDich=? AND TrangThai='Đã hủy'",tx));
        service.create(film,roomA,start);
        assertThrows(IllegalArgumentException.class,() -> service.cancel(show.maSuat()));
        assertThrows(IllegalArgumentException.class,() -> service.changeDate(show.maSuat(),start.toLocalDate().plusDays(1)));
    }

    @Test void processingAndPaidAwaitingTicketBlockAllChanges() throws Exception {
        SuatChieu show=service.create(film,roomA,start); String tx=hold(show,"Đang thanh toán");
        assertThrows(IllegalArgumentException.class,() -> service.editable(show.maSuat()));
        assertThrows(IllegalArgumentException.class,() -> service.cancel(show.maSuat()));
        assertThrows(IllegalArgumentException.class,() -> service.changeTime(show.maSuat(),LocalTime.of(12,0)));
        try(Connection c=Database.connect()) { execute(c,"UPDATE GIAODICH SET TrangThai='Đã thanh toán' WHERE MaGiaoDich=?",tx); }
        assertThrows(IllegalArgumentException.class,() -> service.changeRoom(show.maSuat(),roomB));
        assertEquals(start,service.detail(show.maSuat()).thoiGianBatDau());
    }

    @Test void paymentRecordBlocksEvenIfTransactionStatusIsInconsistent() throws Exception {
        SuatChieu show=service.create(film,roomA,start); String tx=hold(show,"Chờ thanh toán");
        try(Connection c=Database.connect()) { execute(c,"INSERT INTO THANHTOAN (MaGiaoDich,PhuongThuc,SoTien,ThoiGianThanhToan) VALUES (?,'Tiền mặt',100000,NOW())",tx); }
        assertThrows(IllegalArgumentException.class,() -> service.cancel(show.maSuat()));
        assertEquals(1,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich=?",show.maSuat(),tx));
    }

    @Test void soldSeatAndIssuedTicketArePreserved() throws Exception {
        SuatChieu show=service.create(film,roomA,start); String tx=hold(show,"Đã thanh toán");
        try(Connection c=Database.connect()) {
            execute(c,"UPDATE GHE_SUATCHIEU SET TrangThai='Đã bán',MaGia=(SELECT MaGia FROM BANGGIAVE WHERE MaLoaiPhong=? LIMIT 1),GiaVeChot=100000 WHERE MaSuat=? AND MaGiaoDich=?",type,show.maSuat(),tx);
            execute(c,"INSERT INTO VE (MaVe,MaGiaoDich,MaSuat,MaGhe,MaGia,MaQR,GiaVe,TrangThai,ThoiGianPhatHanh) SELECT ?,MaGiaoDich,MaSuat,MaGhe,MaGia,?,GiaVeChot,'Chưa sử dụng',NOW() FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich=?",tx,tx,show.maSuat(),tx);
        }
        assertThrows(IllegalArgumentException.class,() -> service.changeFilm(show.maSuat(),longFilm));
        assertThrows(IllegalArgumentException.class,() -> service.cancel(show.maSuat()));
        assertEquals(1,count("SELECT COUNT(*) FROM VE WHERE MaSuat=?",show.maSuat()));
        assertEquals(film,service.detail(show.maSuat()).maPhim());
    }

    @Test void startedShowAndWrongRoleAreRejected() throws Exception {
        SuatChieu show=service.create(film,roomA,start);
        try(Connection c=Database.connect()) { execute(c,"UPDATE SUATCHIEU SET ThoiGianBatDau=DATE_SUB(NOW(),INTERVAL 30 MINUTE),ThoiGianTraPhong=DATE_ADD(NOW(),INTERVAL 60 MINUTE) WHERE MaSuat=?",show.maSuat()); }
        assertThrows(IllegalArgumentException.class,() -> service.changeTime(show.maSuat(),LocalTime.of(12,0)));
        assertThrows(IllegalArgumentException.class,() -> service.cancel(show.maSuat()));
        LichChieuBUS staff=new LichChieuBUS(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),dao,Set.of());
        assertThrows(IllegalArgumentException.class,() -> staff.create(film,roomA,start));
        assertThrows(IllegalArgumentException.class,() -> staff.list(null,null));
        assertThrows(IllegalArgumentException.class,() -> staff.changeRoom(show.maSuat(),roomB));
        assertThrows(IllegalArgumentException.class,() -> staff.cancel(show.maSuat()));
    }

    @Test void concurrentCreatesCannotBothPassRoomConflictCheck() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch startGate=new CountDownLatch(1);
        try {
            Callable<SuatChieu> task=() -> { startGate.await(); return service.create(film,roomA,start); };
            List<Future<SuatChieu>> calls=List.of(pool.submit(task),pool.submit(task)); startGate.countDown();
            int successes=0,conflicts=0;
            for(Future<SuatChieu> call:calls) {
                try { call.get(15,TimeUnit.SECONDS); successes++; }
                catch(ExecutionException e) { assertInstanceOf(IllegalArgumentException.class,e.getCause()); conflicts++; }
            }
            assertEquals(1,successes); assertEquals(1,conflicts);
            assertEquals(1,count("SELECT COUNT(*) FROM SUATCHIEU WHERE MaPhim=?",film));
        } finally { pool.shutdownNow(); }
    }

    private String hold(SuatChieu show,String state) throws Exception {
        String tx="T"+UUID.randomUUID().toString().replace("-","").substring(0,19); transactions.add(tx);
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO GIAODICH VALUES (?,'NV001',NOW(),100000,?)",tx,state);
            execute(c,"UPDATE GHE_SUATCHIEU SET TrangThai='Đang giữ',MaGiaoDich=?,ThoiGianHetHanGiu=DATE_ADD(NOW(),INTERVAL 5 MINUTE),GiaVeChot=100000 WHERE MaSuat=? AND TrangThai='Trống' LIMIT 1",tx,show.maSuat());
        }
        return tx;
    }
    private static int count(String sql,Object... args) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(sql)) {
            bind(ps,args); try(ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) { bind(ps,args); ps.executeUpdate(); }
    }
    private static void bind(PreparedStatement ps,Object... args) throws SQLException {
        for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]);
    }
}
