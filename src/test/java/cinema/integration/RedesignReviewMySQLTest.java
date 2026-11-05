package cinema.integration;
import cinema.controller.auth.*;
import cinema.dao.TaiKhoanDAO;
import cinema.model.VaiTro;
import cinema.view.*;
import cinema.view.ui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;
/** Read-only review: opens actual windows through the existing role menu, never saves forms. */
@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class RedesignReviewMySQLTest {
    @AfterEach void close()throws Exception{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();if(!Theme.dark)Theme.toggle();});}
    @Test void allRoleWindowsKeepControlsAndDataWhenSwitchingThemeAtMinimumResolution()throws Exception{
        for(String user:new String[]{"admin_demo","staff_demo"}){
            var session=new DangNhapBUS(new TaiKhoanDAO()).login(user,"Cinema@123".toCharArray());
            SwingUtilities.invokeAndWait(()->CinemaWindow.open(session));
            JFrame home=frames().stream().filter(f->f.getTitle().startsWith("Quản lý rạp chiếu phim")).findFirst().orElseThrow();
            JList<?> menu=(JList<?>)Design.all(home).stream().filter(c->c instanceof JList).findFirst().orElseThrow();
            int count=session.vaiTro()==VaiTro.ADMIN?6:5;assertEquals(count,menu.getModel().getSize());
            SwingUtilities.invokeAndWait(()->review(home,user+"-home"));
            for(int i=0;i<count;i++){
                final int index=i;
                SwingUtilities.invokeAndWait(()->{menu.setSelectedIndex(index);button(home,"Mở chức năng").doClick();});
                assertEquals(1,frames().size(),"Điều hướng không được tạo JFrame mới");
                JFrame child=home;assertSame(menu,Design.all(child).stream().filter(c->c instanceof JList<?> l&&Boolean.TRUE.equals(l.getClientProperty("cinema.nav"))).findFirst().orElseThrow());
                await(()->Design.all(child).stream().noneMatch(c->c instanceof JLabel l&&l.getText()!=null&&l.getText().startsWith("Đang tải")));
                SwingUtilities.invokeAndWait(()->review(child,user+"-"+index));
                if(session.vaiTro()==VaiTro.ADMIN&&i<5){
                    String action=new String[]{"Thêm nhân viên","Thêm phim","Tạo suất chiếu","Thêm phòng",""}[i];
                    JButton add=Design.all(child).stream().filter(c->c instanceof JButton b&&action.equals(b.getText())&&b.isEnabled()).map(c->(JButton)c).findFirst().orElse(null);
                    if(add!=null&&!action.isBlank()){SwingUtilities.invokeLater(add::doClick);await(()->java.util.Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog&&w.isShowing()));SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog dialog&&w.isShowing()){capture(dialog,user+"-form-"+index+"-dark");Theme.toggle();capture(dialog,user+"-form-"+index+"-light");Theme.toggle();dialog.dispose();}});}
                }

            }
            SwingUtilities.invokeAndWait(home::dispose);
        }
    }
    private static void review(JFrame frame,String name){frame.setSize(1366,768);frame.validate();List<JTable> tables=Design.all(frame).stream().filter(c->c instanceof JTable).map(c->(JTable)c).toList();var models=tables.stream().map(JTable::getModel).toList();capture(frame,name+"-dark");Theme.toggle();frame.validate();capture(frame,name+"-light");for(int i=0;i<tables.size();i++)assertSame(models.get(i),tables.get(i).getModel());Theme.toggle();for(Component c:Design.all(frame))if(c instanceof JList<?> list&&Boolean.TRUE.equals(list.getClientProperty("cinema.nav")))assertTrue(list.getWidth()>100);assertTrue(frame.isShowing());}
    private static JButton button(Container root,String text){return Design.all(root).stream().filter(c->c instanceof JButton b&&text.equals(b.getText())).map(c->(JButton)c).findFirst().orElseThrow();}
    private static List<JFrame> frames(){return java.util.Arrays.stream(Frame.getFrames()).filter(f->f instanceof JFrame&&f.isDisplayable()).map(f->(JFrame)f).toList();}
    private static void await(BooleanSupplier condition){assertTimeoutPreemptively(Duration.ofSeconds(20),()->{AtomicBoolean ready=new AtomicBoolean();while(!ready.get()){SwingUtilities.invokeAndWait(()->ready.set(condition.getAsBoolean()));Thread.sleep(25);}});}
    private static void capture(Window window,String name){try{Path directory=Path.of("target/ui-redesign/sprint7"+System.getProperty("cinema.qa.suffix",""));Files.createDirectories(directory);BufferedImage image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();window.paint(graphics);graphics.dispose();ImageIO.write(image,"png",directory.resolve(name+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}}
}
