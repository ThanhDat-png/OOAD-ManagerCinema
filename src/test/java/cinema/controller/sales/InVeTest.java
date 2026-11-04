package cinema.controller.sales;
import cinema.dao.VeDAO;
import cinema.model.*;
import cinema.controller.auth.PhienDangNhap;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import java.math.BigDecimal;
import java.time.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InVeTest {
    @Test void longUnicodeTitlesProduceCompleteScannableQrAndPrintUsesSavedTickets() throws Exception {
        Ve v=new Ve("V123","G123",1,1,1,"qr-persisted-value",new BigDecimal("100000"),"Chưa sử dụng",LocalDateTime.now(),"A",1,"Ghế thường");
        var info=new VeDAO.ThongTin(v,"Phim tiếng Việt dài ".repeat(30),"R1","Phòng 1",LocalDateTime.now(),"Sắp chiếu","Đang hoạt động",true,true,LocalDateTime.now());
        var image=InVe.preview(info);assertTrue(image.getHeight()>560);
        assertEquals(v.maQR(),new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText());
        var print=new InVe(List.of(info));var paper=new Paper();paper.setSize(600,800);paper.setImageableArea(20,20,560,760);var format=new PageFormat();format.setPaper(paper);
        var target=new BufferedImage(600,800,BufferedImage.TYPE_INT_RGB);var graphics=target.createGraphics();try{assertEquals(Printable.PAGE_EXISTS,print.print(graphics,format,0));assertEquals(Printable.NO_SUCH_PAGE,print.print(graphics,format,1));assertEquals(Printable.NO_SUCH_PAGE,print.print(graphics,format,-1));}finally{graphics.dispose();}
    }
    @Test void permissionsAndHoldConfigurationAreValidated() {
        var admin=new BanVeBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new cinema.dao.BanVeDAO(),5);
        assertThrows(IllegalArgumentException.class,()->admin.search("",null,null));assertThrows(IllegalArgumentException.class,()->admin.quote("G1"));
        var noProfile=new BanVeBUS(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),new cinema.dao.BanVeDAO(),5);assertThrows(IllegalArgumentException.class,noProfile::employeeId);
        assertThrows(IllegalArgumentException.class,()->new BanVeBUS(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),new cinema.dao.BanVeDAO(),0));
        assertEquals(5,noProfile.holdMinutes());
    }
}
