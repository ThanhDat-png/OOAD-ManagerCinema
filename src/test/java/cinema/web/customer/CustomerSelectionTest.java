package cinema.web.customer;

import cinema.web.db.WebDb;
import cinema.web.staff.Admissions;
import cinema.web.staff.CounterSales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class CustomerSelectionTest {
    @TempDir Path files;
    private WebDb db;
    private CustomerSelections customer;
    private CounterSales sales;
    private int show;
    private int seat;

    @BeforeEach
    void setup() {
        db = new WebDb(new MockEnvironment().withProperty("CINEMA_SQLITE_PATH", files.resolve("public.sqlite").toString()));
        customer = new CustomerSelections(db);
        sales = new CounterSales(db);
        show = ((Number) sales.shows("", "", "").get(0).get("MaSuat")).intValue();
        seat = ((Number) customer.seats(show).stream().filter(row -> Boolean.TRUE.equals(row.get("Available")))
                .findFirst().orElseThrow().get("MaGhe")).intValue();
    }

    @Test
    void selectionStaysUnpaidAndUnheldUntilStaffClaims() throws Exception {
        String reference = customer.create(show, List.of(seat));
        assertNotNull(customer.find(reference));
        assertFalse(customer.find(reference).claimed());
        assertEquals(reference, sales.incomingCustomerSelections().get(0).get("Reference"));
        assertTrue(db.query("SELECT * FROM GIAODICH").isEmpty());
        assertTrue(db.query("SELECT * FROM THANHTOAN").isEmpty());
        assertTrue((Boolean) customer.seats(show).stream()
                .filter(row -> ((Number) row.get("MaGhe")).intValue() == seat)
                .findFirst().orElseThrow().get("Available"));

        var claimed = sales.claimCustomerSelection(reference, "NV002", null);
        assertEquals(claimed, sales.claimCustomerSelection(reference, "NV002", claimed.orderId()));
        assertTrue(customer.find(reference).claimed());
        assertTrue(sales.incomingCustomerSelections().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> sales.claimCustomerSelection(reference, "NV003", null));
        assertThrows(IllegalArgumentException.class, () -> sales.issue(claimed.orderId(), "NV002"));

        sales.startPayment(claimed.orderId(), "NV002");
        sales.pay(claimed.orderId(), "NV002", "Tiền mặt", true);
        sales.issue(claimed.orderId(), "NV002");
        assertTrue(sales.claimCustomerSelection(reference, "NV002", null).paid());
        Admissions admissions = new Admissions(db);
        var ticket = admissions.tickets(claimed.orderId()).get(0);
        assertTrue(admissions.qr(ticket.get("MaVe").toString()).length > 200);
    }

    @Test
    void concurrentReferencesCanOnlyClaimTheSeatOnce() throws Exception {
        String first = customer.create(show, List.of(seat));
        String second = customer.create(show, List.of(seat));
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> { start.await(); return claim(first, "NV002"); });
            var b = executor.submit(() -> { start.await(); return claim(second, "NV003"); });
            start.countDown();
            assertNotEquals(a.get(), b.get());
            assertEquals(1, db.query("SELECT * FROM GIAODICH").size());
            assertEquals(1, db.query("SELECT * FROM WEB_CUSTOMER_SELECTION WHERE MaGiaoDich IS NOT NULL").size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void expiredReferenceCannotCreateStaffOrder() {
        String reference = customer.create(show, List.of(seat));
        db.update("UPDATE WEB_CUSTOMER_SELECTION SET ExpiresAt='2000-01-01 00:00:00' WHERE Reference=?", reference);
        assertNull(customer.find(reference));
        assertTrue(sales.incomingCustomerSelections().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> sales.claimCustomerSelection(reference, "NV002", null));
        assertTrue(db.query("SELECT * FROM GIAODICH").isEmpty());
    }

    @Test
    void caseInsensitiveLookupAndReleaseOnCancelExpireAndShowChange() {
        String reference = customer.create(show, List.of(seat));
        var lower = customer.find(reference.toLowerCase(java.util.Locale.ROOT));
        assertNotNull(lower);
        assertEquals(reference, lower.reference());
        assertFalse(lower.showChanged());

        var firstClaim = sales.claimCustomerSelection(reference, "NV002", null);
        sales.cancel(firstClaim.orderId(), "NV002", false);
        assertFalse(customer.find(reference).claimed());
        assertFalse(sales.incomingCustomerSelections().isEmpty());

        var secondClaim = sales.claimCustomerSelection(reference, "NV003", null);
        db.update("UPDATE GHE_SUATCHIEU SET ThoiGianHetHanGiu='2000-01-01 00:00:00' WHERE MaGiaoDich=?", secondClaim.orderId());
        assertFalse(customer.find(reference).claimed());
        assertFalse(sales.incomingCustomerSelections().isEmpty());

        var thirdClaim = sales.claimCustomerSelection(reference, "NV002", null);
        sales.cancel(thirdClaim.orderId(), "NV002", false);
        db.update("UPDATE SUATCHIEU SET TrangThai='Đã hủy' WHERE MaSuat=?", show);
        assertTrue(customer.find(reference).showChanged());
    }

    private boolean claim(String reference, String employee) {
        try {
            sales.claimCustomerSelection(reference, employee, null);
            return true;
        } catch (IllegalArgumentException conflict) {
            return false;
        }
    }
}
