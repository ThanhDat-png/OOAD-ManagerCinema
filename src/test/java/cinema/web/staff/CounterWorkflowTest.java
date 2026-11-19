package cinema.web.staff;

import cinema.web.db.WebDb;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CounterWorkflowTest {
    @TempDir Path files;
    private WebDb db;
    private CounterSales sales;
    private Admissions admissions;
    private int show,seat,second;

    @BeforeEach void setup() {
        db=new WebDb(new MockEnvironment().withProperty("CINEMA_SQLITE_PATH",files.resolve("counter.sqlite").toString())
                .withProperty("CINEMA_DEMO_DATA","true"));
        sales=new CounterSales(db);
        admissions=new Admissions(db);
        var slot=sales.shows("","","").get(0);
        show=CounterSales.number(slot,"MaSuat");
        var seats=sales.seats(show).stream().filter(row->"Trống".equals(row.get("TrangThai"))).toList();
        seat=CounterSales.number(seats.get(0),"MaGhe");
        second=CounterSales.number(seats.get(1),"MaGhe");
    }
    private void paid(String id) {
        sales.hold(id,"NV002",show,List.of(seat));
        sales.quote(id,"NV002");
        sales.startPayment(id,"NV002");
        sales.pay(id,"NV002","Tiền mặt",true);
    }
    @Test void seatRacePaymentRecoveryAndRepeatAdmission() throws Exception {
        var start=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        String winner;
        try {
            List<Future<Boolean>> outcomes=new ArrayList<>();
            for(String id:List.of("G1","G2")) outcomes.add(executor.submit(()->{
                start.await();
                try { sales.hold(id,"NV002",show,List.of(seat)); return true; }
                catch(IllegalArgumentException taken) { return false; }
            }));
            start.countDown();
            boolean first=outcomes.get(0).get(), next=outcomes.get(1).get();
            assertNotEquals(first,next);
            winner=first?"G1":"G2";
        } finally { executor.shutdownNow(); }
        assertThrows(IllegalArgumentException.class,()->sales.quote(winner,"NV003"));
        sales.quote(winner,"NV002");
        var price=sales.order(winner).get("TongTien");
        sales.startPayment(winner,"NV002");
        db.update("UPDATE GHE_SUATCHIEU SET ThoiGianHetHanGiu='2000-01-01 00:00:00' WHERE MaGiaoDich=?",winner);
        sales.seats(show);
        assertEquals(CounterSales.PAYING,sales.order(winner).get("TrangThai"));
        assertThrows(IllegalArgumentException.class,()->sales.cancel(winner,"NV002",false));
        assertThrows(IllegalArgumentException.class,()->sales.pay(winner,"NV002","Tiền mặt",false));
        sales.pay(winner,"NV002","Tiền mặt",true);
        sales.pay(winner,"NV002","Tiền mặt",true);
        sales.issue(winner,"NV002");
        var ticket=admissions.tickets(winner).get(0);
        sales.issue(winner,"NV002");
        assertEquals(1,admissions.tickets(winner).size());
        assertEquals(price,ticket.get("GiaVe"));
        assertEquals(ticket.get("MaVe"),admissions.lookup(ticket.get("MaQR").toString()).get("MaVe"));
        assertEquals(1,sales.search(ticket.get("MaVe").toString(),"","").size());
        assertTrue(admissions.qr(ticket.get("MaVe").toString()).length>200);
        db.update("UPDATE SUATCHIEU SET ThoiGianBatDau=? WHERE MaSuat=?",LocalDateTime.now(CounterSales.CINEMA_ZONE).format(CounterSales.SQL_TIME),show);
        ticket=admissions.tickets(winner).get(0);
        assertNotNull(admissions.failure(ticket,"wrong",show));
        String room=ticket.get("MaPhong").toString(), code=ticket.get("MaVe").toString();
        assertNull(admissions.failure(ticket,room,show));
        admissions.confirm(code,room,show);
        assertThrows(IllegalArgumentException.class,()->admissions.confirm(code,room,show));
    }
    @Test void expiryConflictsAndInvalidChangesLeaveSeatsConsistent() {
        sales.hold("G1","NV002",show,List.of(seat));
        sales.hold("G2","NV003",show,List.of(second));
        assertThrows(IllegalArgumentException.class,()->sales.hold("G1","NV002",show,List.of(second)));
        assertEquals(seat,CounterSales.number(sales.selected("G1").get(0),"MaGhe"));
        assertThrows(IllegalArgumentException.class,()->sales.issue("G1","NV002"));
        db.update("UPDATE GHE_SUATCHIEU SET ThoiGianHetHanGiu='2000-01-01 00:00:00' WHERE MaGiaoDich='G1'");
        sales.seats(show);
        assertEquals("Hết hạn",sales.order("G1").get("TrangThai"));
        assertTrue(sales.selected("G1").isEmpty());
        sales.cancel("G2","NV003",false);
        assertTrue(sales.selected("G2").isEmpty());
        paid("G3");
        assertThrows(IllegalArgumentException.class,()->sales.cancel("G3","NV002",true));
        assertThrows(IllegalArgumentException.class,()->sales.search("","",""));
        assertThrows(IllegalArgumentException.class,()->sales.search("","2026-11-10","2026-11-01"));
    }
}
