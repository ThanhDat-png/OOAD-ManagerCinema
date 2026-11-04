package cinema.integration;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.pricing.*;
import cinema.controller.schedule.LichChieuBUS;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class PricingMySQLTest {
    private final String key=UUID.randomUUID().toString().replace("-","").substring(0,8);
    private final String code="P"+key,emptyCode="E"+key,film="F"+key,room="R"+key;
    private final int type=100000+ThreadLocalRandom.current().nextInt(1_000_000_000);
    private final List<String> transactions=new ArrayList<>();
    private final PhienDangNhap session=new PhienDangNhap(1,"admin",VaiTro.ADMIN,null);
    private final GiaVeBUS prices=new GiaVeBUS(session,new BangGiaVeDAO());
    private final SanPhamBUS products=new SanPhamBUS(session,new SanPhamDAO());
    private int priceId;

    @BeforeEach void setup() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO LOAIGHE VALUES (?,?)",type,"Seat "+key);
            execute(c,"INSERT INTO LOAIPHONG VALUES (?,?,'2D')",type,"Room "+key);
        }
        priceId=prices.create(type,type,1,new BigDecimal("100000"));
        products.create(product(code,"Product %_ "+key,10,new BigDecimal("50000")));
    }
    @AfterEach void cleanup() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"DELETE FROM VE WHERE MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhim=?)",film);
            execute(c,"DELETE FROM GHE_SUATCHIEU WHERE MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhim=?)",film);
            execute(c,"DELETE FROM SUATCHIEU WHERE MaPhim=?",film);
            for(String tx:transactions) { execute(c,"DELETE FROM THANHTOAN WHERE MaGiaoDich=?",tx); execute(c,"DELETE FROM GIAODICH WHERE MaGiaoDich=?",tx); }
            execute(c,"DELETE FROM GHE WHERE MaPhong=?",room); execute(c,"DELETE FROM PHONGCHIEU WHERE MaPhong=?",room); execute(c,"DELETE FROM PHIM WHERE MaPhim=?",film);
            execute(c,"DELETE FROM BANGGIAVE WHERE MaLoaiGhe=?",type); execute(c,"DELETE FROM LOAIGHE WHERE MaLoaiGhe=?",type); execute(c,"DELETE FROM LOAIPHONG WHERE MaLoaiPhong=?",type);
            execute(c,"DELETE FROM SANPHAM WHERE MaSP IN (?,?)",code,emptyCode);
        }
    }
    private SanPham product(String id,String name,int quantity,BigDecimal price) {
        return new SanPham(id,name,"Mô tả thử nghiệm","Hộp",price,quantity,"Đang kinh doanh");
    }

    @Test void duplicateConditionsAndInvalidReferencesCannotCreateExtraPrice() throws Exception {
        assertEquals("Giá vé cho điều kiện này đã tồn tại",assertThrows(IllegalArgumentException.class,
                () -> prices.create(type,type,1,new BigDecimal("120000"))).getMessage());
        assertThrows(IllegalArgumentException.class,() -> prices.create(Integer.MAX_VALUE,type,1,BigDecimal.ONE));
        assertEquals(1,prices.all().stream().filter(p -> p.loaiGhe().maLoaiGhe()==type).count());
    }
    @Test void priceUpdateChangesOnlyAmountAndRejectsInvalidAmounts() throws Exception {
        BangGiaVe before=prices.detail(priceId); prices.update(priceId,new BigDecimal("150000"));
        BangGiaVe after=prices.detail(priceId);
        assertEquals(new BigDecimal("150000"),after.giaVe()); assertEquals(before.loaiGhe(),after.loaiGhe());
        assertEquals(before.loaiPhong(),after.loaiPhong()); assertEquals(before.loaiThoiDiem(),after.loaiThoiDiem());
        assertThrows(IllegalArgumentException.class,() -> prices.update(priceId,new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class,() -> prices.update(Integer.MAX_VALUE,BigDecimal.ONE));
        assertEquals(new BigDecimal("150000"),prices.detail(priceId).giaVe());
    }
    @Test void productCodeIsUniqueAndInvalidCreateDoesNotOverwriteExisting() throws Exception {
        SanPham before=products.detail(code);
        assertThrows(IllegalArgumentException.class,() -> products.create(product(code.toLowerCase(Locale.ROOT),"Other",0,BigDecimal.ONE)));
        assertThrows(IllegalArgumentException.class,() -> products.create(product(emptyCode,"Invalid",-1,BigDecimal.ONE)));
        assertEquals(before,products.detail(code)); assertThrows(IllegalArgumentException.class,() -> products.detail(emptyCode));
    }
    @Test void editingProductInfoKeepsCodeAndPriceAndAllowsZeroInventory() throws Exception {
        products.updateInfo(new SanPham(code,"Tên mới","","Ly",new BigDecimal("999"),0,"Ngừng kinh doanh"));
        SanPham after=products.detail(code);
        assertEquals(code,after.maSP()); assertEquals(new BigDecimal("50000"),after.giaBan());
        assertEquals("Tên mới",after.tenSP()); assertEquals("Ly",after.donViTinh()); assertEquals(0,after.soLuongTon());
        assertEquals("Ngừng kinh doanh",after.tinhTrangKinhDoanh());
    }
    @Test void priceChangePreservesEveryOtherProductField() throws Exception {
        SanPham before=products.detail(code); products.updatePrice(code,new BigDecimal("75000"));
        SanPham after=products.detail(code);
        assertEquals(new SanPham(before.maSP(),before.tenSP(),before.moTa(),before.donViTinh(),new BigDecimal("75000"),before.soLuongTon(),before.tinhTrangKinhDoanh()),after);
        products.updatePrice(code,new BigDecimal("75000"));
        assertEquals(10,products.detail(code).soLuongTon());
    }
    @Test void badProductEditsPreservePriorData() throws Exception {
        SanPham before=products.detail(code);
        assertThrows(IllegalArgumentException.class,() -> products.updateInfo(product(code,"Changed",-1,new BigDecimal("50000"))));
        assertThrows(IllegalArgumentException.class,() -> products.updatePrice(code,BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,() -> products.updatePrice(code,new BigDecimal("1000000000000")));
        assertEquals(before,products.detail(code));
    }
    @Test void inventoryFilterAndSearchAreReadOnlyAndEscapeWildcardInput() throws Exception {
        products.create(product(emptyCode,"Empty "+key,0,new BigDecimal("20000")));
        assertEquals(1,products.search(code,true).size()); assertTrue(products.search(code,false).isEmpty());
        assertEquals(1,products.search(emptyCode,false).size()); assertTrue(products.search(emptyCode,true).isEmpty());
        assertTrue(products.search("%_",null).stream().anyMatch(p -> p.maSP().equals(code)));
        assertTrue(products.search("' OR 1=1 --",null).isEmpty());
        assertFalse(products.search("",null).isEmpty());
        assertEquals(10,products.detail(code).soLuongTon()); assertEquals(0,products.detail(emptyCode).soLuongTon());
    }
    @Test void concurrentInfoAndPriceUpdatesDoNotOverwriteEachOther() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch gate=new CountDownLatch(1);
        try {
            Future<?> info=pool.submit(() -> { gate.await(); products.updateInfo(product(code,"Updated concurrently",99,new BigDecimal("50000"))); return null; });
            Future<?> price=pool.submit(() -> { gate.await(); products.updatePrice(code,new BigDecimal("80000")); return null; });
            gate.countDown(); info.get(10,TimeUnit.SECONDS); price.get(10,TimeUnit.SECONDS);
            assertEquals("Updated concurrently",products.detail(code).tenSP()); assertEquals(99,products.detail(code).soLuongTon());
            assertEquals(new BigDecimal("80000"),products.detail(code).giaBan());
        } finally { pool.shutdownNow(); }
    }
    @Test void concurrentPriceCreationHasOneWinnerForEachCondition() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch gate=new CountDownLatch(1);
        try {
            Callable<Integer> call=() -> { gate.await(); return prices.create(type,type,2,new BigDecimal("130000")); };
            List<Future<Integer>> results=List.of(pool.submit(call),pool.submit(call)); gate.countDown(); int successes=0;
            for(Future<Integer> result:results) {
                try { result.get(10,TimeUnit.SECONDS); successes++; }
                catch(ExecutionException e) { assertInstanceOf(IllegalArgumentException.class,e.getCause()); }
            }
            assertEquals(1,successes); assertEquals(2,prices.all().stream().filter(p -> p.loaiGhe().maLoaiGhe()==type).count());
        } finally { pool.shutdownNow(); }
    }
    @Test void updatingPricePreservesPaidTicketTotalsAndPendingQuotes() throws Exception {
        LocalDate date=LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(30).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO PHIM VALUES (?,?,90,'Test',?,0,'test','Đang chiếu','2D')",film,film,java.sql.Date.valueOf(date));
            execute(c,"INSERT INTO PHONGCHIEU VALUES (?,?,?,15,'Đang hoạt động')",room,type,room);
            execute(c,"INSERT INTO GHE (MaPhong,MaLoaiGhe,HangGhe,SoGhe,TrangThai) VALUES (?,?,'A',1,'Sẵn sàng sử dụng'),(?,?,'A',2,'Sẵn sàng sử dụng')",room,type,room,type);
        }
        SuatChieu show=new LichChieuBUS(session,new SuatChieuDAO(),Set.of()).create(film,room,date.atTime(10,0));
        for(int seat=1;seat<=2;seat++) {
            String tx="T"+UUID.randomUUID().toString().replace("-","").substring(0,19); transactions.add(tx);
            try(Connection c=Database.connect()) {
                execute(c,"INSERT INTO GIAODICH VALUES (?,'NV001',NOW(),100000,?)",tx,seat==1?"Đã thanh toán":"Chờ thanh toán");
                execute(c,"UPDATE GHE_SUATCHIEU gs JOIN GHE g ON g.MaGhe=gs.MaGhe SET gs.MaGiaoDich=?,gs.MaGia=?,gs.GiaVeChot=100000,gs.TrangThai=? WHERE gs.MaSuat=? AND g.SoGhe=?",
                        tx,priceId,seat==1?"Đã bán":"Đang giữ",show.maSuat(),seat);
                if(seat==1) {
                    execute(c,"INSERT INTO THANHTOAN (MaGiaoDich,PhuongThuc,SoTien,ThoiGianThanhToan) VALUES (?,'Tiền mặt',100000,NOW())",tx);
                    execute(c,"INSERT INTO VE (MaVe,MaGiaoDich,MaSuat,MaGhe,MaGia,MaQR,GiaVe,TrangThai,ThoiGianPhatHanh) SELECT ?,MaGiaoDich,MaSuat,MaGhe,MaGia,?,GiaVeChot,'Chưa sử dụng',NOW() FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich=?",tx,tx,show.maSuat(),tx);
                }
            }
        }
        prices.update(priceId,new BigDecimal("180000"));
        assertEquals(new BigDecimal("180000"),prices.detail(priceId).giaVe());
        assertEquals(2,count("SELECT COUNT(*) FROM GHE_SUATCHIEU WHERE MaSuat=? AND GiaVeChot=100000",show.maSuat()));
        assertEquals(1,count("SELECT COUNT(*) FROM VE WHERE MaSuat=? AND GiaVe=100000",show.maSuat()));
        for(String tx:transactions) assertEquals(1,count("SELECT COUNT(*) FROM GIAODICH WHERE MaGiaoDich=? AND TongTien=100000",tx));
        assertEquals(1,count("SELECT COUNT(*) FROM THANHTOAN WHERE MaGiaoDich=? AND SoTien=100000",transactions.get(0)));
    }
    private static int count(String sql,Object... args) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(sql)) { bind(ps,args); try(ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getInt(1); } }
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException { try(PreparedStatement ps=c.prepareStatement(sql)) { bind(ps,args); ps.executeUpdate(); } }
    private static void bind(PreparedStatement ps,Object... args) throws SQLException { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); }
}
