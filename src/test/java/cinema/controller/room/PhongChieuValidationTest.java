package cinema.controller.room;
import cinema.controller.auth.PhienDangNhap;
import cinema.dao.*;
import cinema.model.VaiTro;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhongChieuValidationTest {
    @Test void missingRoomOrSeatStateIsRejectedBeforeDatabaseAccess() {
        var service=new PhongChieuBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new PhongChieuDAO(),new GheDAO());
        assertThrows(IllegalArgumentException.class,() -> service.create("Room",1,15,null));
        assertThrows(IllegalArgumentException.class,() -> service.updateState("R1",null));
        assertThrows(IllegalArgumentException.class,() -> service.addSeat("R1","A",1,1,null));
        assertThrows(IllegalArgumentException.class,() -> service.updateSeat(1,"A",1,null));
    }
}
