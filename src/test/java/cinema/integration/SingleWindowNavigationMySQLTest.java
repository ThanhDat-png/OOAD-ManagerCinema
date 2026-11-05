package cinema.integration;

import cinema.dao.BanVeDAO;
import cinema.view.*;
import cinema.view.ui.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class SingleWindowNavigationMySQLTest {
    private final SalesMySQLTest fixture=new SalesMySQLTest();
    @BeforeEach void setup()throws Exception{fixture.setup();}
    @AfterEach void cleanup()throws Exception{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});fixture.cleanup();}

    @Test void switchingPagesKeepsSameShellCartLeaseAndRunningSaleTimer()throws Exception{
        SwingUtilities.invokeAndWait(()->CinemaWindow.open(SalesMySQLTest.session("NV002")));
        JFrame frame=(JFrame)java.util.Arrays.stream(Frame.getFrames()).filter(Frame::isDisplayable).findFirst().orElseThrow();
        Shell shell=(Shell)frame.getContentPane();
        JList<?> menu=(JList<?>)Design.all(frame).stream().filter(c->c instanceof JList<?> l&&Boolean.TRUE.equals(l.getClientProperty("cinema.nav"))).findFirst().orElseThrow();
        SwingUtilities.invokeAndWait(()->navigate(menu,4));
        BanVePanel sale=(BanVePanel)shell.getClientProperty("cinema.activePage");
        await(()->button(sale,"Tìm suất").isEnabled());
        SwingUtilities.invokeAndWait(()->{
            JComboBox<?> shows=(JComboBox<?>)Design.all(sale).stream().filter(c->c instanceof JComboBox<?> box&&box.getItemCount()>0&&box.getItemAt(0) instanceof BanVeDAO.SuatBan).findFirst().orElseThrow();
            boolean found=false;for(int i=0;i<shows.getItemCount();i++)if(shows.getItemAt(i) instanceof BanVeDAO.SuatBan show&&show.suat().maSuat()==fixture.show){shows.setSelectedIndex(i);found=true;break;}assertTrue(found);
            button(sale,"Chọn suất").doClick();
        });
        await(()->button(sale,"Tìm suất").isEnabled()&&Design.all(sale).stream().anyMatch(c->c instanceof JToggleButton));
        String[] id={null};
        SwingUtilities.invokeAndWait(()->{
            String identity=Design.all(sale).stream().filter(c->c instanceof JLabel l&&l.getText()!=null&&l.getText().startsWith("Mã giao dịch hiện tại:")).map(c->((JLabel)c).getText()).findFirst().orElseThrow();
            id[0]=identity.substring("Mã giao dịch hiện tại: ".length()).split(" ")[0];fixture.ids.add(id[0]);
            Design.all(sale).stream().filter(c->c instanceof JToggleButton b&&b.getText().equals("A1")).map(c->(JToggleButton)c).findFirst().orElseThrow().doClick();
            button(sale,"Giữ ghế / đổi ghế").doClick();
        });
        await(()->button(sale,"Tìm suất").isEnabled());
        var before=new BanVeDAO().inspect(id[0]);assertEquals("Chờ thanh toán",before.giaoDich().trangThai());assertEquals(1,before.ghe().size());
        var timerField=BanVePanel.class.getDeclaredField("refresh");timerField.setAccessible(true);Timer refresh=(Timer)timerField.get(sale);
        SwingUtilities.invokeAndWait(()->{
            assertTrue(refresh.isRunning());navigate(menu,0);
            assertFalse(sale.isShowing());assertTrue(sale.isDisplayable());assertTrue(refresh.isRunning());assertSame(shell,frame.getContentPane());
            navigate(menu,4);assertSame(sale,shell.getClientProperty("cinema.activePage"));assertTrue(sale.isShowing());assertSame(shell,frame.getContentPane());
            assertEquals(1,java.util.Arrays.stream(Frame.getFrames()).filter(Frame::isDisplayable).count());
            Theme.toggle();assertSame(sale,shell.getClientProperty("cinema.activePage"));Theme.toggle();
        });
        var after=new BanVeDAO().inspect(id[0]);assertEquals(before.giaoDich(),after.giaoDich());assertEquals(before.ghe(),after.ghe());assertTrue(refresh.isRunning());
    }

    private static void navigate(JList<?> menu,int index){menu.setSelectedIndex(index);menu.getActionMap().get("cinema.open").actionPerformed(new ActionEvent(menu,ActionEvent.ACTION_PERFORMED,"open"));}
    private static JButton button(Container root,String text){return Design.all(root).stream().filter(c->c instanceof JButton b&&text.equals(b.getText())).map(c->(JButton)c).findFirst().orElseThrow();}
    private static void await(BooleanSupplier condition){assertTimeoutPreemptively(Duration.ofSeconds(20),()->{AtomicBoolean ready=new AtomicBoolean();while(!ready.get()){SwingUtilities.invokeAndWait(()->ready.set(condition.getAsBoolean()));Thread.sleep(25);}});}
}
