package cinema.controller.pricing;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.*;
import cinema.model.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PricingValidationTest {
    @Test void moneySupportsWholeVndAndMaximumSqlValueExactly() {
        assertEquals(new BigDecimal("1"),MoneyRules.positive(BigDecimal.ONE,"Invalid"));
        assertEquals(new BigDecimal("999999999999"),MoneyRules.positive(new BigDecimal("999999999999"),"Invalid"));
        assertEquals(new BigDecimal("100000"),MoneyRules.positive(new BigDecimal("100000.000"),"Invalid"));
    }
    @Test void moneyRejectsMissingNegativeZeroFractionAndOverflow() {
        for(BigDecimal value:new BigDecimal[]{null,BigDecimal.ZERO,new BigDecimal("-1"),new BigDecimal("0.5"),new BigDecimal("1000000000000")})
            assertThrows(IllegalArgumentException.class,() -> MoneyRules.positive(value,"Invalid"));
    }
    @Test void staffIsRejectedBeforeReadingOrValidatingMutableData() {
        var staff=new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null);
        var prices=new GiaVeBUS(staff,new BangGiaVeDAO());
        assertEquals("Bạn không có quyền quản lý giá vé",assertThrows(IllegalArgumentException.class,prices::all).getMessage());
        assertEquals("Bạn không có quyền quản lý giá vé",assertThrows(IllegalArgumentException.class,() -> prices.create(0,0,0,null)).getMessage());
        assertEquals("Bạn không có quyền quản lý giá vé",assertThrows(IllegalArgumentException.class,() -> prices.update(0,null)).getMessage());
        var products=new SanPhamBUS(staff,new SanPhamDAO());
        assertEquals("Bạn không có quyền quản lý F&B",assertThrows(IllegalArgumentException.class,() -> products.search("",null)).getMessage());
        assertEquals("Bạn không có quyền quản lý F&B",assertThrows(IllegalArgumentException.class,() -> products.create(null)).getMessage());
        assertEquals("Bạn không có quyền quản lý F&B",assertThrows(IllegalArgumentException.class,() -> products.updateInfo(null)).getMessage());
        assertEquals("Bạn không có quyền quản lý F&B",assertThrows(IllegalArgumentException.class,() -> products.updatePrice("missing",null)).getMessage());
    }
    @Test void invalidProductFieldsAreRejectedBeforeAnyWrite() {
        var products=new SanPhamBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new SanPhamDAO());
        assertThrows(IllegalArgumentException.class,() -> products.create(null));
        assertThrows(IllegalArgumentException.class,() -> products.create(new SanPham("","Name","","Hộp",BigDecimal.ONE,1,"Đang kinh doanh")));
        assertThrows(IllegalArgumentException.class,() -> products.create(new SanPham("SP1","Name","","Hộp",BigDecimal.ONE,-1,"Đang kinh doanh")));
        assertThrows(IllegalArgumentException.class,() -> products.create(new SanPham("SP1","Name","","Hộp",BigDecimal.ONE,1,null)));
        assertThrows(IllegalArgumentException.class,() -> products.create(new SanPham("SP1","Name","","Hộp",BigDecimal.ZERO,1,"Đang kinh doanh")));
    }
}
