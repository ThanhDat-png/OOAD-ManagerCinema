package cinema.integration;

import cinema.controller.auth.*;
import cinema.controller.room.PhongChieuBUS;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import cinema.view.CinemaWindow;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class PhongChieuDesktopMySQLTest {
    private final PhongChieuBUS service=new PhongChieuBUS(new PhienDangNhap(1,"test",VaiTro.ADMIN,null),new PhongChieuDAO(),new GheDAO());
    private PhongChieu room;
    @BeforeEach void setup() throws Exception {
        room=service.create("GUI room "+UUID.randomUUID().toString().substring(0,8),1,15,"Đang hoạt động");
        service.addSeat(room.maPhong(),"A",1,1,"Sẵn sàng sử dụng");
    }
    @AfterEach void cleanup() throws Exception {
        SwingUtilities.invokeAndWait(() -> { for(Window w:Window.getWindows()) w.dispose(); });
        if(room==null) return;
        try(Connection c=Database.connect()) {
            execute(c,"DELETE FROM GHE WHERE MaPhong=?",room.maPhong()); execute(c,"DELETE FROM PHONGCHIEU WHERE MaPhong=?",room.maPhong());
        }
    }
    private JFrame open(String username,String title,int index) throws Exception {
        var session=new DangNhapBUS(new TaiKhoanDAO()).login(username,"Cinema@123".toCharArray());
        SwingUtilities.invokeAndWait(() -> {
            CinemaWindow.open(session); JFrame home=frame("Quản lý rạp chiếu phim — "+session.vaiTro().getTen());
            components(home).stream().filter(c -> c instanceof JList).map(c -> (JList<?>)c).findFirst().orElseThrow().setSelectedIndex(index);
            click(home,"Mở chức năng");
        });
        await(() -> frame(title)!=null && button(frame(title),"Tìm kiếm").isEnabled());
        JFrame frame=frame(title);
        SwingUtilities.invokeAndWait(() -> { textFields(frame).get(0).setText(room.maPhong()); click(frame,"Tìm kiếm"); });
        await(() -> components(frame).stream().anyMatch(c -> c instanceof JTable t && t.getRowCount()==1 && room.maPhong().equals(t.getValueAt(0,0))));
        SwingUtilities.invokeAndWait(() -> components(frame).stream().filter(c -> c instanceof JTable).map(c -> (JTable)c).findFirst().orElseThrow().setRowSelectionInterval(0,0));
        return frame;
    }
    private void map(JFrame frame) throws Exception {
        SwingUtilities.invokeLater(() -> click(frame,"Xem phòng và sơ đồ ghế"));
        await(() -> dialog("Sơ đồ ghế")!=null && components(dialog("Sơ đồ ghế")).stream().anyMatch(c -> c instanceof JButton b && b.getText().startsWith("A1 ·")));
    }

    @Test void adminAddsClassifiesMaintainsSeatAndEditsRoomThroughDesktop() throws Exception {
        JFrame frame=open("admin_demo","Quản lý phòng chiếu",3); map(frame);
        SwingUtilities.invokeLater(() -> click(dialog("Sơ đồ ghế"),"Thêm ghế")); await(() -> dialog("Thêm ghế")!=null);
        SwingUtilities.invokeAndWait(() -> {
            JDialog form=dialog("Thêm ghế"); textFields(form).get(0).setText("A"); textFields(form).get(1).setText("2");
            for(Component c:components(form)) if(c instanceof JComboBox<?> combo)
                for(int i=0;i<combo.getItemCount();i++) if(combo.getItemAt(i) instanceof LoaiGhe type && type.maLoaiGhe()==1) combo.setSelectedIndex(i);
            click(form,"Lưu");
        }); dismissSuccess();
        await(() -> seatButton("A2 ·")!=null && button(dialog("Sơ đồ ghế"),"Phân loại ghế").isEnabled());
        SwingUtilities.invokeAndWait(() -> seatButton("A2 ·").doClick());
        SwingUtilities.invokeLater(() -> click(dialog("Sơ đồ ghế"),"Phân loại ghế")); await(() -> dialog("Phân loại ghế")!=null);
        SwingUtilities.invokeAndWait(() -> {
            JDialog form=dialog("Phân loại ghế");
            for(Component c:components(form)) if(c instanceof JComboBox<?> combo)
                for(int i=0;i<combo.getItemCount();i++) if(combo.getItemAt(i) instanceof LoaiGhe type && "Ghế VIP".equals(type.tenLoaiGhe())) combo.setSelectedIndex(i);
            click(form,"Lưu");
        }); dismissSuccess();
        await(() -> seatButton("A2 · Ghế VIP")!=null && button(dialog("Sơ đồ ghế"),"Cập nhật ghế").isEnabled());
        SwingUtilities.invokeAndWait(() -> seatButton("A2 ·").doClick());
        SwingUtilities.invokeLater(() -> click(dialog("Sơ đồ ghế"),"Cập nhật ghế")); await(() -> dialog("Cập nhật ghế")!=null);
        SwingUtilities.invokeAndWait(() -> {
            JDialog form=dialog("Cập nhật ghế");
            for(Component c:components(form)) if(c instanceof JComboBox<?> combo && combo.getItemAt(0) instanceof String) combo.setSelectedItem("Bảo trì");
            click(form,"Lưu");
        }); dismissSuccess();
        await(() -> seatButton("A2 · Ghế VIP · Bảo trì")!=null);
        SwingUtilities.invokeAndWait(() -> { screenshot(dialog("Sơ đồ ghế"),"target/uc05-map.png"); click(dialog("Sơ đồ ghế"),"Trở về"); });
        assertEquals(2,service.seats(room.maPhong()).size());
        SwingUtilities.invokeLater(() -> click(frame,"Cập nhật phòng")); await(() -> dialog("Cập nhật phòng")!=null);
        SwingUtilities.invokeAndWait(() -> { textFields(dialog("Cập nhật phòng")).get(0).setText("Cancel rename"); click(dialog("Cập nhật phòng"),"Hủy"); });
        assertEquals(room.tenPhong(),service.detail(room.maPhong()).tenPhong());
        SwingUtilities.invokeLater(() -> click(frame,"Cập nhật phòng")); await(() -> dialog("Cập nhật phòng")!=null);
        SwingUtilities.invokeAndWait(() -> { textFields(dialog("Cập nhật phòng")).get(0).setText("Renamed "+room.maPhong()); click(dialog("Cập nhật phòng"),"Lưu"); });
        dismissSuccess(); await(() -> button(frame,"Tìm kiếm").isEnabled());
        assertEquals("Renamed "+room.maPhong(),service.detail(room.maPhong()).tenPhong());
        SwingUtilities.invokeAndWait(() -> screenshot(frame,"target/uc05-rooms.png"));
    }

    @Test void staffSeesRoomAndSeatMapWithoutMutationButtons() throws Exception {
        JFrame frame=open("staff_demo","Tra cứu phòng chiếu",2);
        SwingUtilities.invokeAndWait(() -> assertTrue(components(frame).stream().noneMatch(c -> c instanceof JButton b && "Thêm phòng chiếu".equals(b.getText()))));
        map(frame);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(components(dialog("Sơ đồ ghế")).stream().noneMatch(c -> c instanceof JButton b && "Thêm ghế".equals(b.getText())));
            seatButton("A1 ·").doClick(); click(dialog("Sơ đồ ghế"),"Trở về");
        });
        assertEquals(1,service.seats(room.maPhong()).size());
    }
    private static JButton seatButton(String prefix) {
        JDialog map=dialog("Sơ đồ ghế"); if(map==null) return null;
        return components(map).stream().filter(c -> c instanceof JButton b && b.getText().startsWith(prefix)).map(c -> (JButton)c).findFirst().orElse(null);
    }
    private static void dismissSuccess() throws Exception {
        await(() -> message()!=null); SwingUtilities.invokeAndWait(() -> message().dispose());
    }
    private static JFrame frame(String title) {
        for(Frame f:JFrame.getFrames()) if(f instanceof JFrame frame && frame.isDisplayable() && title.equals(frame.getTitle())) return frame; return null;
    }
    private static JDialog dialog(String title) {
        for(Window w:Window.getWindows()) if(w instanceof JDialog d && d.isShowing() && title.equals(d.getTitle())) return d; return null;
    }
    private static JDialog message() {
        for(Window w:Window.getWindows()) if(w instanceof JDialog d && d.isShowing() && components(d).stream().anyMatch(c -> c instanceof JOptionPane)) return d; return null;
    }
    private static List<Component> components(Container parent) {
        List<Component> result=new ArrayList<>(); for(Component c:parent.getComponents()) { result.add(c); if(c instanceof Container child) result.addAll(components(child)); } return result;
    }
    private static List<JTextField> textFields(Container parent) { return components(parent).stream().filter(c -> c instanceof JTextField).map(c -> (JTextField)c).toList(); }
    private static JButton button(Container parent,String text) { return components(parent).stream().filter(c -> c instanceof JButton b && text.equals(b.getText())).map(c -> (JButton)c).findFirst().orElseThrow(); }
    private static void click(Container parent,String text) { button(parent,text).doClick(); }
    private static void screenshot(Window window,String name) {
        BufferedImage image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics(); window.paint(graphics); graphics.dispose();
        try { ImageIO.write(image,"png",Path.of(name).toFile()); } catch(java.io.IOException e) { throw new RuntimeException(e); }
    }
    private static void await(java.util.function.BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(15),() -> {
            AtomicBoolean done=new AtomicBoolean(); while(!done.get()) { SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean())); Thread.sleep(20); }
        });
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException { try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); } }
}
