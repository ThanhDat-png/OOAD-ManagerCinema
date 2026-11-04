package cinema.controller.admission;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.VeDAO;
import cinema.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VeRulesTest {
    private final LocalDateTime start=LocalDateTime.of(2030,1,1,10,0);
    private final VeBUS service=new VeBUS(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),new VeDAO(),new ThoiGianVaoPhong(15,15));
    private VeDAO.ThongTin info(LocalDateTime now,String state,boolean paid,boolean sold,String showState,String roomState) {
        Ve ticket=new Ve("V1","G1",1,1,1,"qr1",new BigDecimal("100000"),state,start.minusDays(1),"A",1,"Thường");
        return new VeDAO.ThongTin(ticket,"Phim","P1","Phòng",start,showState,roomState,paid,sold,now);
    }
    @Test void admissionWindowIncludesBothFifteenMinuteBoundaries() {
        for(LocalDateTime at:new LocalDateTime[]{start.minusMinutes(15),start,start.plusMinutes(15)})
            assertDoesNotThrow(() -> service.validate(info(at,"Chưa sử dụng",true,true,"Sắp chiếu","Đang hoạt động"),"P1",1));
        for(LocalDateTime at:new LocalDateTime[]{start.minusMinutes(15).minusSeconds(1),start.plusMinutes(15).plusSeconds(1)})
            assertEquals("Ngoài thời gian cho phép vào phòng",assertThrows(IllegalArgumentException.class,
                    () -> service.validate(info(at,"Chưa sử dụng",true,true,"Sắp chiếu","Đang hoạt động"),"P1",1)).getMessage());
    }
    @Test void rejectsUsedUnpaidWrongSeatCancelledAndClosedRoom() {
        assertEquals("Vé đã được sử dụng",assertThrows(IllegalArgumentException.class,
                () -> service.validate(info(start,"Đã sử dụng",true,true,"Sắp chiếu","Đang hoạt động"),"P1",1)).getMessage());
        assertThrows(IllegalArgumentException.class,() -> service.validate(info(start,"Chưa sử dụng",false,true,"Sắp chiếu","Đang hoạt động"),"P1",1));
        assertThrows(IllegalArgumentException.class,() -> service.validate(info(start,"Chưa sử dụng",true,false,"Sắp chiếu","Đang hoạt động"),"P1",1));
        assertThrows(IllegalArgumentException.class,() -> service.validate(info(start,"Chưa sử dụng",true,true,"Đã hủy","Đang hoạt động"),"P1",1));
        assertThrows(IllegalArgumentException.class,() -> service.validate(info(start,"Chưa sử dụng",true,true,"Sắp chiếu","Bảo trì"),"P1",1));
    }
    @Test void rejectsRoomAndShowDifferentFromControlContext() {
        var ticket=info(start,"Chưa sử dụng",true,true,"Sắp chiếu","Đang hoạt động");
        assertThrows(IllegalArgumentException.class,() -> service.validate(ticket,"P2",1));
        assertThrows(IllegalArgumentException.class,() -> service.validate(ticket,"P1",2));
    }
    @Test void roleAndMissingCodeAreCheckedBeforeDatabase() {
        var admin=new VeBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new VeDAO(),new ThoiGianVaoPhong(15,15));
        assertEquals("Bạn không có quyền kiểm tra và xác nhận vé",assertThrows(IllegalArgumentException.class,() -> admin.lookup("V1",VeDAO.LoaiMa.MA_VE)).getMessage());
        assertThrows(IllegalArgumentException.class,() -> admin.confirm("V1","P1",1));
        assertThrows(IllegalArgumentException.class,() -> service.lookup(" ",VeDAO.LoaiMa.MA_VE));
        assertThrows(IllegalArgumentException.class,() -> service.lookup("x".repeat(101),VeDAO.LoaiMa.MA_QR));
        assertThrows(IllegalArgumentException.class,() -> service.lookup("V1",null));
        assertThrows(IllegalArgumentException.class,() -> service.check("V1",VeDAO.LoaiMa.MA_VE,null,0));
    }
    @Test void validatesWindowConfiguration() {
        assertThrows(IllegalArgumentException.class,() -> new ThoiGianVaoPhong(-1,15));
        assertThrows(IllegalArgumentException.class,() -> new ThoiGianVaoPhong(15,1441));
        assertThrows(IllegalArgumentException.class,() -> new VeBUS(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),new VeDAO(),null));
    }
}
