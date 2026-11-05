package cinema.view;
import cinema.view.ui.*;
import javax.swing.*;
import java.awt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GatePresentationTest {
    @Test void staffHomeContainsOnlyStaffNavigation()throws Exception{SwingUtilities.invokeAndWait(()->{CinemaWindow.open(new cinema.controller.auth.PhienDangNhap(2,"staff",cinema.model.VaiTro.NHAN_VIEN,null));for(Window window:Window.getWindows())if(window instanceof JFrame frame&&frame.isDisplayable()&&frame.getTitle().endsWith("Nhân viên")){JList<?> menu=Design.all(frame).stream().filter(c->c instanceof JList).map(c->(JList<?>)c).findFirst().orElseThrow();assertEquals(5,menu.getModel().getSize());for(int i=0;i<5;i++){String title=menu.getModel().getElementAt(i).toString();assertFalse(title.startsWith("Quản lý"));assertFalse(title.contains("Báo cáo"));}frame.dispose();}});}

    @Test void resultHeadingsReflectOnlyExistingBusinessMessages()throws Exception{SwingUtilities.invokeAndWait(()->{Theme.install();JLabel status=new JLabel();JTextArea source=new JTextArea("Phim: Phim thử\nPhòng: R1\nGhế: A1\nSuất: 5");ReadOnlyViews.Gate gate=new ReadOnlyViews.Gate(source,status);String[][] cases={{"Vé hợp lệ","HỢP LỆ"},{"Vé đã được sử dụng","ĐÃ SỬ DỤNG"},{"Vé chưa có thanh toán hợp lệ","CHƯA THANH TOÁN"},{"Vé không đúng suất chiếu đang kiểm soát","SAI SUẤT CHIẾU"},{"Ngoài thời gian cho phép vào phòng","CẦN KIỂM TRA LẠI"}};for(String[] item:cases){status.setText(item[0]);assertTrue(Design.all(gate).stream().anyMatch(c->c instanceof JLabel label&&item[1].equals(label.getText())));}assertEquals("Phim: Phim thử\nPhòng: R1\nGhế: A1\nSuất: 5",source.getText());});}
}
