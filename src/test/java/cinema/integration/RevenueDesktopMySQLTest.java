package cinema.integration;
import cinema.controller.auth.*;
import cinema.dao.TaiKhoanDAO;
import cinema.model.VaiTro;
import cinema.view.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class RevenueDesktopMySQLTest {
    private final RevenueMySQLTest data=new RevenueMySQLTest();
    @BeforeEach void setup()throws Exception{data.setup();}
    @AfterEach void cleanup()throws Exception{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});data.cleanup();}
    private JFrame open()throws Exception {
        var session=new DangNhapBUS(new TaiKhoanDAO()).login("admin_demo","Cinema@123".toCharArray());
        SwingUtilities.invokeAndWait(()->{CinemaWindow.open(session);JFrame home=frame("Quản lý rạp chiếu phim — Admin");components(home).stream().filter(c->c instanceof JList).map(c->(JList<?>)c).findFirst().orElseThrow().setSelectedIndex(5);button(home,"Mở chức năng").doClick();});
        await(()->frame("Báo cáo và thống kê")!=null && ready(frame("Báo cáo và thống kê")));return frame("Báo cáo và thống kê");
    }
    @Test void adminRunsEveryReportFromMainMenuAndSourcesStayUnchanged()throws Exception {
        String before=data.snapshot();JFrame frame=open();
        SwingUtilities.invokeAndWait(()->{combo(frame,"time.period").setSelectedIndex(1);field(frame,"time.from").setText("1456-02");buttonNamed(frame,"time.run").doClick();});
        await(()->ready(frame) && table(frame,"time").getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{assertEquals(3L,table(frame,"time").getValueAt(0,0));assertEquals(1L,table(frame,"time").getValueAt(0,3));money("500000",table(frame,"time").getValueAt(0,4));assertFalse(table(frame,"time").isCellEditable(0,0));screenshot(frame,"target/uc09-time.png");tabs(frame).setSelectedIndex(1);select(frame,"film.filter",data.fixture.film);dates(frame,"film","1456-02-01","1456-02-29");buttonNamed(frame,"film.run").doClick();});
        await(()->ready(frame) && table(frame,"film").getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{assertEquals(4L,table(frame,"film").getValueAt(0,2));money("400000",table(frame,"film").getValueAt(0,3));screenshot(frame,"target/uc09-film.png");tabs(frame).setSelectedIndex(2);select(frame,"staff.filter","NV002");dates(frame,"staff","1456-02-01","1456-02-29");buttonNamed(frame,"staff.run").doClick();});
        await(()->ready(frame) && table(frame,"staff").getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{assertEquals(3L,table(frame,"staff").getValueAt(0,2));money("500000",table(frame,"staff").getValueAt(0,6));screenshot(frame,"target/uc09-staff.png");tabs(frame).setSelectedIndex(3);combo(frame,"summary.type").setSelectedIndex(1);select(frame,"tickets.film",data.fixture.film);select(frame,"tickets.show",Integer.toString(data.fixture.show));dates(frame,"tickets","1456-03-01","1456-03-01");buttonNamed(frame,"tickets.run").doClick();});
        await(()->ready(frame) && table(frame,"tickets").getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{assertEquals(4L,table(frame,"tickets").getValueAt(0,0));assertEquals(1L,table(frame,"tickets").getValueAt(0,2));screenshot(frame,"target/uc09-tickets.png");combo(frame,"summary.type").setSelectedIndex(2);select(frame,"stock.product",data.stockB);combo(frame,"stock.state").setSelectedIndex(1);combo(frame,"stock.available").setSelectedIndex(2);buttonNamed(frame,"stock.run").doClick();});
        await(()->ready(frame) && table(frame,"stock").getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{assertEquals(data.stockB,table(frame,"stock").getValueAt(0,0));assertEquals(0,table(frame,"stock").getValueAt(0,4));screenshot(frame,"target/uc09-stock.png");});assertEquals(before,data.snapshot());
    }
    @Test void invalidCriteriaClearStaleResultsAndStaffCannotOpenReports()throws Exception {
        JFrame frame=open();SwingUtilities.invokeAndWait(()->{buttonNamed(frame,"time.run").doClick();assertTrue(label(frame,"time.message").getText().contains("Vui lòng chọn đầy đủ"));combo(frame,"time.period").setSelectedIndex(1);field(frame,"time.from").setText("1456-02");buttonNamed(frame,"time.run").doClick();});
        await(()->ready(frame) && table(frame,"time").getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{field(frame,"time.from").setText("1456-13");assertEquals(0,table(frame,"time").getRowCount());buttonNamed(frame,"time.run").doClick();assertTrue(label(frame,"time.message").getText().contains("không đúng định dạng"));combo(frame,"time.period").setSelectedIndex(3);dates(frame,"time","1456-03-01","1456-02-28");buttonNamed(frame,"time.run").doClick();assertEquals("Khoảng thời gian không hợp lệ",label(frame,"time.message").getText());field(frame,"time.to").setText("");buttonNamed(frame,"time.run").doClick();assertTrue(label(frame,"time.message").getText().contains("đầy đủ"));dates(frame,"time","1455-01-01","1455-12-31");buttonNamed(frame,"time.run").doClick();});
        await(()->ready(frame) && table(frame,"time").getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{assertEquals(0L,table(frame,"time").getValueAt(0,0));money("0",table(frame,"time").getValueAt(0,4));assertThrows(IllegalArgumentException.class,()->new BaoCaoPanel(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null)));});
    }
    private static void dates(Container root,String name,String from,String to){field(root,name+".from").setText(from);field(root,name+".to").setText(to);}
    private static boolean ready(Container root){return buttonNamed(root,"time.run").isEnabled();}
    private static void money(String expected,Object actual){assertEquals(0,new BigDecimal(expected).compareTo((BigDecimal)actual));}
    private static Component named(Container root,String name){return components(root).stream().filter(c->name.equals(c.getName())).findFirst().orElseThrow(()->new IllegalArgumentException(name));}
    private static JButton buttonNamed(Container root,String name){return (JButton)named(root,name);}
    private static JComboBox<?> combo(Container root,String name){return (JComboBox<?>)named(root,name);}
    private static JTextField field(Container root,String name){return (JTextField)named(root,name);}
    private static JLabel label(Container root,String name){return (JLabel)named(root,name);}
    private static JTable table(Container root,String name){return (JTable)named(root,name+".table");}
    private static JTabbedPane tabs(Container root){return components(root).stream().filter(c->c instanceof JTabbedPane).map(c->(JTabbedPane)c).findFirst().orElseThrow();}
    private static void select(Container root,String name,String id){JComboBox<?> box=combo(root,name);for(int i=0;i<box.getItemCount();i++)if(box.getItemAt(i).toString().startsWith(id+" · ")){box.setSelectedIndex(i);return;}fail("Không có lựa chọn "+id);}
    private static JFrame frame(String title){for(Frame f:Frame.getFrames())if(f instanceof JFrame frame && frame.isDisplayable() && title.equals(frame.getTitle()))return frame;return null;}
    private static JButton button(Container root,String text){return components(root).stream().filter(c->c instanceof JButton b && text.equals(b.getText())).map(c->(JButton)c).findFirst().orElseThrow();}
    private static List<Component> components(Container root){List<Component> result=new ArrayList<>();for(Component c:root.getComponents()){result.add(c);if(c instanceof Container child)result.addAll(components(child));}return result;}
    private static void await(BooleanSupplier condition){assertTimeoutPreemptively(Duration.ofSeconds(20),()->{AtomicBoolean done=new AtomicBoolean();while(!done.get()){SwingUtilities.invokeAndWait(()->done.set(condition.getAsBoolean()));Thread.sleep(25);}});}
    private static void screenshot(Window window,String file){var image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();window.paint(g);g.dispose();try{ImageIO.write(image,"png",Path.of(file).toFile());}catch(Exception e){throw new RuntimeException(e);}}
}
