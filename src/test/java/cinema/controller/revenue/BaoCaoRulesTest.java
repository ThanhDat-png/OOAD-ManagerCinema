package cinema.controller.revenue;
import cinema.controller.auth.PhienDangNhap;
import cinema.dao.BaoCaoDAO;
import cinema.model.VaiTro;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BaoCaoRulesTest {
    private final BaoCaoBUS bus=new BaoCaoBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new BaoCaoDAO());
    @Test void loginAndAdminAreRequired(){assertThrows(IllegalArgumentException.class,()->new BaoCaoBUS(null,new BaoCaoDAO()));assertThrows(IllegalArgumentException.class,()->new BaoCaoBUS(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),new BaoCaoDAO()));}
    @Test void emptyPartialReversedAndUnsupportedDatesAreRejected(){LocalDate day=LocalDate.of(2026,11,5);assertThrows(IllegalArgumentException.class,()->new KhoangThongKe(null,day));assertThrows(IllegalArgumentException.class,()->new KhoangThongKe(day,null));assertThrows(IllegalArgumentException.class,()->new KhoangThongKe(day,day.minusDays(1)));assertThrows(IllegalArgumentException.class,()->KhoangThongKe.nam(999));assertThrows(IllegalArgumentException.class,()->KhoangThongKe.nam(10000));assertThrows(IllegalArgumentException.class,()->bus.theoThoiGian(null));}
    @Test void dayMonthLeapYearAndDatabaseBoundariesAreInclusive(){LocalDate leap=LocalDate.of(2024,2,29);assertEquals(new KhoangThongKe(leap,leap),KhoangThongKe.ngay(leap));assertEquals(leap,KhoangThongKe.thang(YearMonth.of(2024,2)).denNgay());assertEquals(28,KhoangThongKe.thang(YearMonth.of(2023,2)).denNgay().getDayOfMonth());assertEquals(LocalDate.of(9999,12,31),KhoangThongKe.nam(9999).denNgay());assertEquals(LocalDate.of(1000,1,1),KhoangThongKe.nam(1000).tuNgay());}
    @Test void invalidFiltersAreRejectedBeforeDatabaseAccess(){var range=KhoangThongKe.nam(2026);assertThrows(IllegalArgumentException.class,()->bus.ve(range,null,0));assertThrows(IllegalArgumentException.class,()->bus.theoPhim(range,"x".repeat(11)));assertThrows(IllegalArgumentException.class,()->bus.sanPham(null,"Sai trạng thái",null));}
}
