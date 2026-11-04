package cinema.integration;
import cinema.controller.auth.*;
import cinema.dao.*;
import cinema.db.Database;
import cinema.view.CinemaWindow;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
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
class SalesDesktopMySQLTest {
    private final SalesMySQLTest fixture=new SalesMySQLTest();
    @BeforeEach void setup() throws Exception {fixture.setup();}
    @AfterEach void cleanup() throws Exception {SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});fixture.cleanup();}
    private JFrame open() throws Exception {
        var session=new DangNhapBUS(new TaiKhoanDAO()).login("staff_demo","Cinema@123".toCharArray());
        SwingUtilities.invokeAndWait(()->{CinemaWindow.open(session);JFrame home=frame("Quản lý rạp chiếu phim — Nhân viên");components(home).stream().filter(c->c instanceof JList).map(c->(JList<?>)c).findFirst().orElseThrow().setSelectedIndex(4);click(home,"Mở chức năng");});
        await(()->frame("Quản lý bán vé")!=null && ready(frame("Quản lý bán vé")));JFrame sales=frame("Quản lý bán vé");
        SwingUtilities.invokeAndWait(()->{
            JComboBox<?> shows=components(sales).stream().filter(c->c instanceof JComboBox<?> box && box.getItemCount()>0 && box.getItemAt(0) instanceof BanVeDAO.SuatBan).map(c->(JComboBox<?>)c).findFirst().orElseThrow();
            for(int i=0;i<shows.getItemCount();i++)if(shows.getItemAt(i) instanceof BanVeDAO.SuatBan show && show.suat().maSuat()==fixture.show){shows.setSelectedIndex(i);break;}
            click(sales,"Chọn suất");
        });await(()->ready(sales) && components(sales).stream().anyMatch(c->c instanceof JToggleButton b && b.getText().startsWith("A1")));
        SwingUtilities.invokeAndWait(()->{
            String label=components(sales).stream().filter(c->c instanceof JLabel l && l.getText()!=null && l.getText().startsWith("Mã giao dịch hiện tại:")).map(c->((JLabel)c).getText()).findFirst().orElseThrow();
            fixture.ids.add(label.substring("Mã giao dịch hiện tại: ".length()).split(" ")[0]);
            components(sales).stream().filter(c->c instanceof JToggleButton b && b.getText().startsWith("A1")).map(c->(JToggleButton)c).findFirst().orElseThrow().doClick();click(sales,"Giữ ghế / đổi ghế");
        });await(()->ready(sales) && text(sales).contains("Chờ thanh toán"));
        SwingUtilities.invokeAndWait(()->click(sales,"Tính tiền"));await(()->ready(sales) && text(sales).contains("Tổng tiền: 100000"));return sales;
    }
    @Test void normalSalePaymentPreviewAndReadOnlyHistoryThroughMainMenu() throws Exception {
        JFrame sales=open();
        SwingUtilities.invokeAndWait(()->{var chair=components(sales).stream().filter(c->c instanceof JToggleButton b && b.getText().startsWith("A2")).map(c->(JToggleButton)c).findFirst().orElseThrow();chair.doClick();click(sales,"Tính tiền");assertTrue(hasMessage(sales,"Vui lòng bấm Giữ ghế / đổi ghế"));chair.doClick();});
        SwingUtilities.invokeLater(()->click(sales,"Thanh toán / tiếp tục"));await(()->dialog("Xác nhận thanh toán tại quầy")!=null);
        SwingUtilities.invokeAndWait(()->{
            JDialog d=dialog("Xác nhận thanh toán tại quầy");components(d).stream().filter(c->c instanceof JComboBox).map(c->(JComboBox<?>)c).findFirst().orElseThrow().setSelectedIndex(1);
            components(d).stream().filter(c->c instanceof JCheckBox).map(c->(JCheckBox)c).findFirst().orElseThrow().doClick();click(d,"Hoàn tất giao dịch");
        });await(()->ready(sales) && dialog("Vé đã lưu")!=null);
        SwingUtilities.invokeAndWait(()->{assertTrue(text(sales).contains("Đã thanh toán"));screenshot(sales,"target/uc08-sales.png");screenshot(dialog("Vé đã lưu"),"target/uc08-preview.png");dialog("Vé đã lưu").dispose();});
        String id=fixture.ids.get(0);assertEquals(1,new BanVeDAO().tickets(id).size());
        SwingUtilities.invokeAndWait(()->{
            components(sales).stream().filter(c->c instanceof JTabbedPane tabs && tabs.getTabCount()==2).map(c->(JTabbedPane)c).findFirst().orElseThrow().setSelectedIndex(1);
            components(sales).stream().filter(c->c instanceof JTextField f && f.getColumns()==22).map(c->(JTextField)c).findFirst().orElseThrow().setText(id);click(sales,"Tra cứu");
        });await(()->ready(sales) && components(sales).stream().anyMatch(c->c instanceof JList<?> list && list.getModel().getSize()==1));
        SwingUtilities.invokeAndWait(()->components(sales).stream().filter(c->c instanceof JList<?> list && list.getModel().getSize()==1).map(c->(JList<?>)c).findFirst().orElseThrow().setSelectedIndex(0));await(()->ready(sales) && text(sales).contains("Vé đã lưu:"));
        assertEquals("Đã thanh toán",new BanVeDAO().inspect(id).giaoDich().trangThai());
    }
    @Test void closingPaymentDialogPreservesProcessingAndRecoveryLoadsSameTransaction() throws Exception {
        JFrame sales=open();SwingUtilities.invokeLater(()->click(sales,"Thanh toán / tiếp tục"));await(()->dialog("Xác nhận thanh toán tại quầy")!=null);
        SwingUtilities.invokeAndWait(()->dialog("Xác nhận thanh toán tại quầy").dispose());await(()->ready(sales));String id=fixture.ids.get(0);
        try(Connection c=Database.connect();var ps=c.prepareStatement("UPDATE GHE_SUATCHIEU SET ThoiGianHetHanGiu=NOW()-INTERVAL 1 MINUTE WHERE MaGiaoDich=?")){ps.setString(1,id);ps.executeUpdate();}
        SwingUtilities.invokeAndWait(()->click(sales,"Cập nhật ghế"));await(()->ready(sales));assertEquals("Đang thanh toán",new BanVeDAO().inspect(id).giaoDich().trangThai());
        SwingUtilities.invokeAndWait(()->{click(sales,"Giao dịch mới");assertTrue(hasMessage(sales,"Hoàn tất hoặc hủy giao dịch hiện tại"));click(sales,"Kiểm tra mã hiện tại");});await(()->ready(sales));
        assertEquals(1,new BanVeDAO().inspect(id).ghe().size());assertEquals(0,new BanVeDAO().tickets(id).size());
    }
    private static boolean ready(Container parent){return button(parent,"Tìm suất").isEnabled();}
    private static String text(Container parent){return components(parent).stream().filter(c->c instanceof JTextArea).map(c->((JTextArea)c).getText()).reduce("",(a,b)->a+"\n"+b);}
    private static boolean hasMessage(Container parent,String prefix){return components(parent).stream().anyMatch(c->c instanceof JLabel l && l.getText()!=null && l.getText().startsWith(prefix));}
    private static JFrame frame(String title){for(Frame f:Frame.getFrames())if(f instanceof JFrame frame && frame.isDisplayable() && title.equals(frame.getTitle()))return frame;return null;}
    private static JDialog dialog(String title){for(Window w:Window.getWindows())if(w instanceof JDialog d && d.isShowing() && title.equals(d.getTitle()))return d;return null;}
    private static List<Component> components(Container parent){List<Component> result=new ArrayList<>();for(Component c:parent.getComponents()){result.add(c);if(c instanceof Container child)result.addAll(components(child));}return result;}
    private static JButton button(Container parent,String name){return components(parent).stream().filter(c->c instanceof JButton b && name.equals(b.getText())).map(c->(JButton)c).findFirst().orElseThrow();}
    private static void click(Container parent,String name){button(parent,name).doClick();}
    private static void await(BooleanSupplier condition){assertTimeoutPreemptively(Duration.ofSeconds(20),()->{AtomicBoolean done=new AtomicBoolean();while(!done.get()){SwingUtilities.invokeAndWait(()->done.set(condition.getAsBoolean()));Thread.sleep(25);}});}
    private static void screenshot(Window window,String file){BufferedImage image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();window.paint(g);g.dispose();try{ImageIO.write(image,"png",Path.of(file).toFile());}catch(Exception e){throw new RuntimeException(e);}}
}
