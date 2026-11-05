package cinema.integration;
import cinema.controller.auth.PhienDangNhap;
import cinema.controller.revenue.*;
import cinema.controller.sales.BanVeBUS;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.VaiTro;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class RevenueMySQLTest {
    final SalesMySQLTest fixture=new SalesMySQLTest();
    final BaoCaoBUS bus=new BaoCaoBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new BaoCaoDAO());
    private final BanVeBUS sales=new BanVeBUS(SalesMySQLTest.session("NV002"),new BanVeDAO(),5);
    final KhoangThongKe feb=KhoangThongKe.thang(YearMonth.of(1456,2));
    final String zeroFilm="Z"+fixture.key,stockA="A"+fixture.key,stockB="B"+fixture.key,stockC="C"+fixture.key;
    String first,second,partial,pending,processing,cancelled;
    @BeforeEach void setup()throws Exception {
        fixture.setup();int type=new GheDAO().find(fixture.a).loaiGhe().maLoaiGhe();List<Integer> seats=new ArrayList<>(List.of(fixture.a,fixture.b));
        for(int i=3;i<=8;i++)seats.add(new GheDAO().insert(fixture.room,"A",i,type,"Sẵn sàng sử dụng").maGhe());
        first=paid(seats.subList(0,2));sales.issue(first);second=paid(seats.subList(2,3));sales.issue(second);partial=paid(seats.subList(3,5));
        sql("INSERT INTO VE (MaVe,MaGiaoDich,MaSuat,MaGhe,MaGia,MaQR,GiaVe,TrangThai,ThoiGianPhatHanh) SELECT ?,MaGiaoDich,MaSuat,MaGhe,MaGia,?,GiaVeChot,'Chưa sử dụng','1456-03-01 08:00:00' FROM GHE_SUATCHIEU WHERE MaGiaoDich=? LIMIT 1","V"+fixture.key,"RQR"+fixture.key,partial);
        pending=hold(seats.subList(5,6));sales.quote(pending);processing=hold(seats.subList(6,7));sales.quote(processing);sales.beginPayment(processing);cancelled=hold(seats.subList(7,8));sales.cancel(cancelled);
        sql("UPDATE GHE_SUATCHIEU SET ThoiGianHetHanGiu=NOW()-INTERVAL 1 MINUTE WHERE MaGiaoDich=?",pending);
        sql("UPDATE THANHTOAN SET ThoiGianThanhToan='1456-02-28 00:00:00' WHERE MaGiaoDich=?",first);
        sql("UPDATE THANHTOAN SET ThoiGianThanhToan='1456-02-29 00:00:00' WHERE MaGiaoDich=?",second);
        sql("UPDATE THANHTOAN SET ThoiGianThanhToan='1456-02-28 23:59:59' WHERE MaGiaoDich=?",partial);
        sql("UPDATE VE SET ThoiGianPhatHanh='1456-03-01 08:00:00' WHERE MaSuat=?",fixture.show);
        sql("UPDATE VE SET TrangThai='Đã sử dụng' WHERE MaGiaoDich=?",second);
        sql("INSERT INTO PHIM SELECT ?,?,ThoiLuong,DaoDien,NgayKhoiChieu,GioiHanTuoi,Poster,'Ngừng chiếu',DinhDangChieu FROM PHIM WHERE MaPhim=?",zeroFilm,"Không bán "+fixture.key,fixture.film);
        sql("INSERT INTO SANPHAM VALUES (?,?,'test','Ly',10000,7,'Đang kinh doanh'),(?,?,'test','Hộp',20000,0,'Đang kinh doanh'),(?,?,'test','Chai',30000,2,'Ngừng kinh doanh')",stockA,stockA,stockB,stockB,stockC,stockC);
    }
    @AfterEach void cleanup()throws Exception {
        sql("DELETE FROM SANPHAM WHERE MaSP IN (?,?,?)",stockA,stockB,stockC);sql("DELETE FROM PHIM WHERE MaPhim=?",zeroFilm);fixture.cleanup();
    }
    private String hold(List<Integer> seats)throws Exception{String id=BanVeBUS.newId();fixture.ids.add(id);sales.hold(id,fixture.show,seats);return id;}
    private String paid(List<Integer> seats)throws Exception{String id=hold(seats);sales.quote(id);sales.beginPayment(id);sales.complete(id,"Tiền mặt",new BigDecimal(100000L*seats.size()),true);return id;}
    void sql(String sql,Object... args)throws Exception{try(Connection c=Database.connect();var ps=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);ps.executeUpdate();}}
    private static void money(String expected,BigDecimal actual){assertEquals(0,new BigDecimal(expected).compareTo(actual));}
    @Test void transactionMoneyIsNotMultipliedByMultipleTicketsAndPendingIssuanceIsVisible()throws Exception {
        var value=bus.theoThoiGian(feb);assertEquals(3,value.soGiaoDich());assertEquals(5,value.soVeBan());assertEquals(4,value.soVeDaPhatHanh());assertEquals(1,value.soVeChoPhatHanh());assertEquals(1,value.giaoDichChoVe());money("500000",value.doanhThu());
    }
    @Test void paymentDateBoundariesIncludeBothEndsAndExcludeNextMidnight()throws Exception {
        var value=bus.theoThoiGian(KhoangThongKe.ngay(LocalDate.of(1456,2,28)));assertEquals(2,value.soGiaoDich());money("400000",value.doanhThu());
        assertEquals(1,bus.theoThoiGian(KhoangThongKe.ngay(LocalDate.of(1456,2,29))).soGiaoDich());assertEquals(0,bus.theoThoiGian(KhoangThongKe.ngay(LocalDate.of(1456,3,1))).soGiaoDich());
        money("500000",bus.theoThoiGian(KhoangThongKe.nam(1456)).doanhThu());
    }
    @Test void filmUsesSavedIssuedTicketPricesAndIncludesZeroSalesFilm()throws Exception {
        var row=bus.theoPhim(feb,fixture.film).get(0);assertEquals(4,row.soVe());money("400000",row.doanhThu());
        int type=new GheDAO().find(fixture.a).loaiGhe().maLoaiGhe();sql("UPDATE BANGGIAVE SET GiaVe=900000 WHERE MaLoaiGhe=?",type);money("400000",bus.theoPhim(feb,fixture.film).get(0).doanhThu());
        var zero=bus.theoPhim(feb,zeroFilm).get(0);assertEquals(0,zero.soVe());money("0",zero.doanhThu());assertTrue(bus.theoPhim(feb,null).stream().anyMatch(r->r.maPhim().equals(zeroFilm)));
    }
    @Test void employeeUsesSavedTransactionOwnerAndZeroEmployeesAreIncluded()throws Exception {
        var row=bus.theoNhanVien(feb,"NV002").get(0);assertEquals("NV002",row.maNV());assertEquals(3,row.tongHop().soGiaoDich());money("500000",row.tongHop().doanhThu());
        assertEquals(0,bus.theoNhanVien(feb,"NV001").get(0).tongHop().soGiaoDich());assertTrue(bus.theoNhanVien(feb,null).size()>=2);
        sql("UPDATE GIAODICH SET MaNV='NV001' WHERE MaGiaoDich=?",second);money("400000",bus.theoNhanVien(feb,"NV002").get(0).tongHop().doanhThu());money("100000",bus.theoNhanVien(feb,"NV001").get(0).tongHop().doanhThu());
    }
    @Test void missingMismatchedAndUnconfirmedPaymentsAreNotCounted()throws Exception {
        sql("UPDATE THANHTOAN SET SoTien=1 WHERE MaGiaoDich=?",first);assertEquals(2,bus.theoThoiGian(feb).soGiaoDich());money("300000",bus.theoThoiGian(feb).doanhThu());
        sql("DELETE FROM THANHTOAN WHERE MaGiaoDich=?",second);sql("UPDATE GIAODICH SET TrangThai='Đã thanh toán' WHERE MaGiaoDich=?",processing);assertEquals(1,bus.theoThoiGian(feb).soGiaoDich());money("200000",bus.theoThoiGian(feb).doanhThu());
    }
    @Test void brokenQuoteOrTicketCannotInflateRevenue()throws Exception {
        sql("UPDATE GHE_SUATCHIEU SET GiaVeChot=1 WHERE MaGiaoDich=?",first);money("300000",bus.theoThoiGian(feb).doanhThu());
        sql("UPDATE VE SET GiaVe=1 WHERE MaGiaoDich=?",second);money("200000",bus.theoThoiGian(feb).doanhThu());assertEquals(1,bus.theoPhim(feb,fixture.film).get(0).soVe());
    }
    @Test void ticketStatisticsUsePublicationDateAndCurrentStateWithFilmShowFilters()throws Exception {
        var march=KhoangThongKe.ngay(LocalDate.of(1456,3,1));var value=bus.ve(march,fixture.film,fixture.show);assertEquals(4,value.tongVe());assertEquals(3,value.chuaSuDung());assertEquals(1,value.daSuDung());
        assertEquals(0,bus.ve(feb,fixture.film,null).tongVe());assertEquals(0,bus.ve(march,zeroFilm,null).tongVe());assertEquals(0,bus.ve(march,null,Integer.MAX_VALUE).tongVe());
    }
    @Test void stockFiltersAreIndependentAndCombined()throws Exception {
        assertEquals(7,bus.sanPham(stockA,"Đang kinh doanh",true).get(0).soLuongTon());assertTrue(bus.sanPham(stockA,null,false).isEmpty());assertEquals(0,bus.sanPham(stockB,null,false).get(0).soLuongTon());
        assertEquals(1,bus.sanPham(stockC,"Ngừng kinh doanh",true).size());assertTrue(bus.sanPham(stockC,"Đang kinh doanh",null).isEmpty());assertTrue(bus.sanPham("missing",null,null).isEmpty());
        assertTrue(bus.sanPham(null,null,null).stream().anyMatch(s->stockA.equals(s.maSP())));
    }
    @Test void reportingDoesNotReleaseExpiredHoldsOrChangeAnySourceRows()throws Exception {
        String before=snapshot();bus.theoThoiGian(feb);bus.theoPhim(feb,null);bus.theoNhanVien(feb,null);bus.ve(KhoangThongKe.nam(1456),null,null);bus.sanPham(null,null,null);assertEquals(before,snapshot());
        assertEquals("Chờ thanh toán",new BanVeDAO().inspect(pending).giaoDich().trangThai());assertEquals(1,new BanVeDAO().inspect(pending).ghe().size());
    }
    @Test void issuanceRecoveryChangesTicketCountsButNeverAddsPaymentRevenue()throws Exception {
        sales.issue(partial);var total=bus.theoThoiGian(feb);assertEquals(5,total.soVeDaPhatHanh());assertEquals(0,total.giaoDichChoVe());money("500000",total.doanhThu());money("500000",bus.theoPhim(feb,fixture.film).get(0).doanhThu());
    }
    @Test void emptyRangesGiveZeroAndLiteralSqlFilterDoesNotEscapeItsParameter()throws Exception {
        var empty=KhoangThongKe.nam(1455);var value=bus.theoThoiGian(empty);assertEquals(0,value.soGiaoDich());assertEquals(0,value.soVeBan());money("0",value.doanhThu());assertEquals(0,bus.theoPhim(empty,fixture.film).get(0).soVe());assertEquals(0,bus.ve(empty,null,null).tongVe());
        assertEquals(0,bus.theoThoiGian(KhoangThongKe.nam(1000)).soGiaoDich());assertEquals(0,bus.theoThoiGian(KhoangThongKe.nam(9999)).soGiaoDich());
        assertTrue(bus.theoPhim(feb,"' OR 1=1").isEmpty());assertTrue(bus.theoNhanVien(feb,"' OR 1=1").isEmpty());
    }
    @Test void aggregateMoneyCanExceedSingleTransactionLimitWithoutRounding()throws Exception {
        for(String id:List.of(first,partial)){
            sql("UPDATE GHE_SUATCHIEU SET GiaVeChot=400000000000 WHERE MaGiaoDich=?",id);sql("UPDATE VE SET GiaVe=400000000000 WHERE MaGiaoDich=?",id);sql("UPDATE GIAODICH SET TongTien=800000000000 WHERE MaGiaoDich=?",id);sql("UPDATE THANHTOAN SET SoTien=800000000000 WHERE MaGiaoDich=?",id);
        }
        money("1600000100000",bus.theoThoiGian(feb).doanhThu());money("1200000100000",bus.theoPhim(feb,fixture.film).get(0).doanhThu());
    }
    String snapshot()throws Exception {
        StringBuilder result=new StringBuilder();String placeholders=String.join(",",Collections.nCopies(fixture.ids.size(),"?"));
        try(Connection c=Database.connect()) {
            for(String table:List.of("GIAODICH","THANHTOAN","VE","GHE_SUATCHIEU")){
                try(var ps=c.prepareStatement("SELECT * FROM "+table+" WHERE MaGiaoDich IN ("+placeholders+") ORDER BY MaGiaoDich")){
                    for(int i=0;i<fixture.ids.size();i++)ps.setString(i+1,fixture.ids.get(i));try(var rs=ps.executeQuery()){List<String> rows=new ArrayList<>();while(rs.next()){StringBuilder row=new StringBuilder();for(int col=1;col<=rs.getMetaData().getColumnCount();col++)row.append(rs.getString(col)).append('|');rows.add(row.toString());}Collections.sort(rows);result.append(table).append(rows);}
                }
            }
            try(var ps=c.prepareStatement("SELECT * FROM SANPHAM WHERE MaSP IN (?,?,?) ORDER BY MaSP")){ps.setString(1,stockA);ps.setString(2,stockB);ps.setString(3,stockC);try(var rs=ps.executeQuery()){while(rs.next())for(int col=1;col<=rs.getMetaData().getColumnCount();col++)result.append(rs.getString(col)).append('|');}}
        }return result.toString();
    }
}
