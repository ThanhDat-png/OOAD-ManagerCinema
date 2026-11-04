package cinema.integration;

import cinema.controller.auth.DangNhapBUS;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import cinema.view.CinemaWindow;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
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
class LichChieuDesktopMySQLTest {
    private final String key=UUID.randomUUID().toString().replace("-","").substring(0,8);
    private final String film="F"+key,room="R"+key;
    private final LocalDate date=LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(30);

    @BeforeEach void setup() throws Exception {
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO PHIM VALUES (?,?,90,'Test',?,0,'test','Đang chiếu','2D')",film,"Phim thử "+key,java.sql.Date.valueOf(date));
            execute(c,"INSERT INTO PHONGCHIEU VALUES (?,1,?,15,'Đang hoạt động')",room,"Phòng thử "+key);
            execute(c,"INSERT INTO GHE (MaPhong,MaLoaiGhe,HangGhe,SoGhe,TrangThai) VALUES (?,1,'A',1,'Sẵn sàng sử dụng')",room);
        }
    }
    @AfterEach void cleanup() throws Exception {
        SwingUtilities.invokeAndWait(() -> { for(Window w:Window.getWindows()) w.dispose(); });
        try(Connection c=Database.connect()) {
            execute(c,"DELETE FROM GHE_SUATCHIEU WHERE MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhim=?)",film);
            execute(c,"DELETE FROM SUATCHIEU WHERE MaPhim=?",film);
            execute(c,"DELETE FROM GHE WHERE MaPhong=?",room);
            execute(c,"DELETE FROM PHONGCHIEU WHERE MaPhong=?",room);
            execute(c,"DELETE FROM PHIM WHERE MaPhim=?",film);
        }
    }

    @Test void adminCreatesChangesAndCancelsScheduleFromDesktop() throws Exception {
        var session=new DangNhapBUS(new TaiKhoanDAO()).login("admin_demo","Cinema@123".toCharArray());
        SwingUtilities.invokeAndWait(() -> {
            CinemaWindow.open(session);
            JFrame home=frame("Quản lý rạp chiếu phim — Admin");
            components(home).stream().filter(c -> c instanceof JList).map(c -> (JList<?>)c).findFirst().orElseThrow().setSelectedIndex(2);
            click(home,"Mở chức năng");
        });
        await(() -> frame("Quản lý lịch chiếu")!=null && button(frame("Quản lý lịch chiếu"),"Tạo suất chiếu").isEnabled());
        JFrame schedule=frame("Quản lý lịch chiếu");
        SwingUtilities.invokeLater(() -> click(schedule,"Tạo suất chiếu"));
        await(() -> dialog("Tạo suất chiếu")!=null);
        SwingUtilities.invokeAndWait(() -> {
            JDialog form=dialog("Tạo suất chiếu");
            for(Component component:components(form)) if(component instanceof JComboBox<?> combo) {
                for(int i=0;i<combo.getItemCount();i++) {
                    Object item=combo.getItemAt(i);
                    if(item instanceof Phim p && film.equals(p.maPhim())) combo.setSelectedIndex(i);
                    if(item instanceof PhongChieu p && room.equals(p.maPhong())) combo.setSelectedIndex(i);
                }
            }
            List<JTextField> fields=textFields(form);
            fields.get(0).setText("2026-02-30"); fields.get(1).setText("10:00");
        });
        SwingUtilities.invokeLater(() -> click(dialog("Tạo suất chiếu"),"Lưu"));
        await(() -> message()!=null);
        SwingUtilities.invokeAndWait(() -> message().dispose());
        SwingUtilities.invokeAndWait(() -> {
            assertNotNull(dialog("Tạo suất chiếu"));
            List<JTextField> fields=textFields(dialog("Tạo suất chiếu"));
            assertEquals("2026-02-30",fields.get(0).getText());
            fields.get(0).setText(date.toString()); click(dialog("Tạo suất chiếu"),"Lưu");
        });
        await(() -> message()!=null); SwingUtilities.invokeAndWait(() -> message().dispose());
        await(() -> components(schedule).stream().anyMatch(c -> c instanceof JTable t && t.getRowCount()>0));
        int[] id=new int[1];
        SwingUtilities.invokeAndWait(() -> {
            JTable table=components(schedule).stream().filter(c -> c instanceof JTable).map(c -> (JTable)c).findFirst().orElseThrow();
            for(int i=0;i<table.getRowCount();i++) if(("Phim thử "+key).equals(table.getValueAt(i,1))) {
                table.setRowSelectionInterval(i,i); id[0]=(Integer)table.getValueAt(i,0); return;
            }
            fail("Created schedule was not shown");
        });
        assertEquals(date.atTime(11,45),new SuatChieuDAO().find(id[0]).thoiGianTraPhong());
        SwingUtilities.invokeLater(() -> click(schedule,"Đổi giờ chiếu"));
        await(() -> dialog("Đổi giờ chiếu")!=null);
        SwingUtilities.invokeAndWait(() -> { textFields(dialog("Đổi giờ chiếu")).get(0).setText("12:00"); click(dialog("Đổi giờ chiếu"),"Hủy"); });
        assertEquals(date.atTime(10,0),new SuatChieuDAO().find(id[0]).thoiGianBatDau());
        SwingUtilities.invokeLater(() -> click(schedule,"Đổi giờ chiếu"));
        await(() -> dialog("Đổi giờ chiếu")!=null);
        SwingUtilities.invokeAndWait(() -> { textFields(dialog("Đổi giờ chiếu")).get(0).setText("12:00"); click(dialog("Đổi giờ chiếu"),"Lưu"); });
        await(() -> message()!=null); SwingUtilities.invokeAndWait(() -> message().dispose());
        await(() -> button(schedule,"Hủy suất chiếu").isEnabled());
        assertEquals(date.atTime(12,0),new SuatChieuDAO().find(id[0]).thoiGianBatDau());
        SwingUtilities.invokeAndWait(() -> {
            JTable table=components(schedule).stream().filter(c -> c instanceof JTable).map(c -> (JTable)c).findFirst().orElseThrow();
            for(int i=0;i<table.getRowCount();i++) if(Integer.valueOf(id[0]).equals(table.getValueAt(i,0))) table.setRowSelectionInterval(i,i);
            screenshot(schedule,"target/uc04-schedule.png");
        });
        SwingUtilities.invokeLater(() -> click(schedule,"Hủy suất chiếu"));
        await(() -> message()!=null);
        SwingUtilities.invokeAndWait(() -> components(message()).stream().filter(c -> c instanceof JOptionPane)
                .map(c -> (JOptionPane)c).findFirst().orElseThrow().setValue(JOptionPane.YES_OPTION));
        await(() -> message()!=null && components(message()).stream().anyMatch(c -> c instanceof JOptionPane p
                && p.getMessage().toString().contains("Hủy suất chiếu thành công")));
        SwingUtilities.invokeAndWait(() -> message().dispose());
        assertEquals("Đã hủy",new SuatChieuDAO().find(id[0]).trangThai());
    }

    private static void execute(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); }
    }
    private static List<Component> components(Container parent) {
        List<Component> result=new ArrayList<>();
        for(Component c:parent.getComponents()) { result.add(c); if(c instanceof Container child) result.addAll(components(child)); } return result;
    }
    private static List<JTextField> textFields(Container parent) {
        return components(parent).stream().filter(c -> c instanceof JTextField).map(c -> (JTextField)c).toList();
    }
    private static JButton button(Container parent,String text) {
        return components(parent).stream().filter(c -> c instanceof JButton b && text.equals(b.getText())).map(c -> (JButton)c).findFirst().orElseThrow();
    }
    private static void click(Container parent,String text) { button(parent,text).doClick(); }
    private static JFrame frame(String title) {
        for(Frame f:JFrame.getFrames()) if(f instanceof JFrame frame && frame.isDisplayable() && title.equals(frame.getTitle())) return frame; return null;
    }
    private static JDialog dialog(String title) {
        for(Window w:Window.getWindows()) if(w instanceof JDialog d && d.isShowing() && title.equals(d.getTitle())) return d; return null;
    }
    private static JDialog message() {
        for(Window w:Window.getWindows()) if(w instanceof JDialog d && d.isShowing()
                && components(d).stream().anyMatch(c -> c instanceof JOptionPane)) return d; return null;
    }
    private static void screenshot(Window window,String name) {
        BufferedImage image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics(); window.paint(graphics); graphics.dispose();
        try { ImageIO.write(image,"png",Path.of(name).toFile()); } catch(java.io.IOException e) { throw new RuntimeException(e); }
    }
    private static void await(java.util.function.BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(15),() -> {
            AtomicBoolean done=new AtomicBoolean();
            while(!done.get()) { SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean())); Thread.sleep(20); }
        });
    }
}
