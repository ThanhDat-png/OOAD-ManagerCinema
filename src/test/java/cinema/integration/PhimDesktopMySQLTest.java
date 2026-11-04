package cinema.integration;

import cinema.controller.auth.*;
import cinema.controller.film.*;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import cinema.view.CinemaWindow;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
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
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests", matches="true")
class PhimDesktopMySQLTest {
    @TempDir Path temporary;
    private final PosterStore posters = new PosterStore();
    private PhimBUS admin;
    private Phim film;

    @BeforeEach void setup() throws Exception {
        PhienDangNhap session = new DangNhapBUS(new TaiKhoanDAO()).login("admin_demo", "Cinema@123".toCharArray());
        admin = new PhimBUS(session, new PhimDAO(), new TheLoaiDAO(), posters);
        Path image = temporary.resolve("poster.png");
        BufferedImage data = new BufferedImage(160, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = data.createGraphics(); g.setColor(Color.DARK_GRAY); g.fillRect(0, 0, 160, 240);
        g.setColor(Color.WHITE); g.drawString("UC03 test", 40, 120); g.dispose();
        ImageIO.write(data, "png", image.toFile());
        film = admin.create(new Phim(null, "GUI " + UUID.randomUUID(), 90, "Đạo diễn thử", LocalDate.of(2026, 11, 1),
                13, null, "Đang chiếu", "2D", List.of(admin.genres().get(0))), image);
    }

    @AfterEach void cleanup() throws Exception {
        SwingUtilities.invokeAndWait(() -> { for (Window window : Window.getWindows()) window.dispose(); });
        if (film == null) return;
        try (Connection c = Database.connect()) {
            for (String table : new String[]{"PHIM_THELOAI", "PHIM"}) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE MaPhim=?")) {
                    ps.setString(1, film.maPhim()); ps.executeUpdate();
                }
            }
        }
        posters.discard(film.poster());
    }

    private JFrame open(String username, String title) throws Exception {
        PhienDangNhap session = new DangNhapBUS(new TaiKhoanDAO()).login(username, "Cinema@123".toCharArray());
        SwingUtilities.invokeAndWait(() -> {
            CinemaWindow.open(session);
            JFrame home = frame("Quản lý rạp chiếu phim — " + session.vaiTro().getTen());
            components(home).stream().filter(c -> c instanceof JList).map(c -> (JList<?>)c).findFirst().orElseThrow().setSelectedIndex(1);
            click(home, "Mở chức năng");
        });
        await(() -> frame(title) != null && components(frame(title)).stream()
                .anyMatch(c -> c instanceof JTable table && table.getRowCount() > 0));
        return frame(title);
    }

    private void selectFilm(JFrame frame) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = components(frame).stream().filter(c -> c instanceof JTable).map(c -> (JTable)c).findFirst().orElseThrow();
            for (int i = 0; i < table.getRowCount(); i++)
                if (film.maPhim().equals(table.getValueAt(i, 0))) { table.setRowSelectionInterval(i, i); return; }
            fail("Missing fixture film");
        });
    }

    @Test void adminRenamesThroughEditorAndCancelPreservesSavedName() throws Exception {
        JFrame frame = open("admin_demo", "Quản lý phim"); selectFilm(frame);
        SwingUtilities.invokeLater(() -> click(frame, "Sửa tên phim"));
        await(() -> editor() != null);
        SwingUtilities.invokeAndWait(() -> {
            text(editor()).setText("Hủy thay đổi"); click(editor(), "Hủy");
        });
        assertEquals(film.tenPhim(), admin.detail(film.maPhim()).tenPhim());
        SwingUtilities.invokeLater(() -> click(frame, "Sửa tên phim"));
        await(() -> editor() != null);
        SwingUtilities.invokeAndWait(() -> { text(editor()).setText("Tên đã đổi " + film.maPhim()); click(editor(), "Lưu"); });
        await(() -> messageDialog() != null);
        SwingUtilities.invokeAndWait(() -> messageDialog().dispose());
        await(() -> components(frame).stream().anyMatch(c -> c instanceof JTable t
                && java.util.stream.IntStream.range(0, t.getRowCount()).anyMatch(i -> ("Tên đã đổi " + film.maPhim()).equals(t.getValueAt(i, 1)))));
        assertEquals("Tên đã đổi " + film.maPhim(), admin.detail(film.maPhim()).tenPhim());
        SwingUtilities.invokeAndWait(() -> screenshot(frame, "target/uc03-admin.png"));
    }

    @Test void employeeCanViewDetailPosterWithoutMutationControls() throws Exception {
        JFrame frame = open("staff_demo", "Tra cứu phim"); selectFilm(frame);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(components(frame).stream().noneMatch(c -> c instanceof JButton b && "Thêm phim".equals(b.getText())));
            assertTrue(components(frame).stream().noneMatch(c -> c instanceof JButton b && "Ngừng chiếu".equals(b.getText())));
        });
        SwingUtilities.invokeLater(() -> click(frame, "Xem chi tiết"));
        await(() -> detail() != null && components(detail()).stream().anyMatch(c -> c instanceof JLabel l && l.getIcon() != null));
        SwingUtilities.invokeAndWait(() -> { screenshot(detail(), "target/uc03-detail.png"); click(detail(), "Trở về"); });
        assertEquals(film.tenPhim(), admin.detail(film.maPhim()).tenPhim());
    }

    private static JTextField text(Container parent) {
        return components(parent).stream().filter(c -> c instanceof JTextField).map(c -> (JTextField)c).findFirst().orElseThrow();
    }
    private static JFrame frame(String title) {
        for (Frame frame : JFrame.getFrames()) if (frame instanceof JFrame f && f.isDisplayable() && title.equals(f.getTitle())) return f;
        return null;
    }
    private static JDialog editor() {
        for (Window w : Window.getWindows()) if (w instanceof JDialog d && d.isShowing()
                && components(d).stream().anyMatch(c -> c instanceof JButton b && "Lưu".equals(b.getText()))) return d;
        return null;
    }
    private static JDialog detail() {
        for (Window w : Window.getWindows()) if (w instanceof JDialog d && d.isShowing()
                && components(d).stream().anyMatch(c -> c instanceof JButton b && "Trở về".equals(b.getText()))) return d;
        return null;
    }
    private static JDialog messageDialog() {
        for (Window w : Window.getWindows()) if (w instanceof JDialog d && d.isShowing()
                && components(d).stream().anyMatch(c -> c instanceof JOptionPane)) return d;
        return null;
    }
    private static List<Component> components(Container parent) {
        List<Component> result = new ArrayList<>();
        for (Component c : parent.getComponents()) { result.add(c); if (c instanceof Container child) result.addAll(components(child)); }
        return result;
    }
    private static void click(Container parent, String title) {
        components(parent).stream().filter(c -> c instanceof JButton b && title.equals(b.getText()))
                .map(c -> (JButton)c).findFirst().orElseThrow().doClick();
    }
    private static void screenshot(Window window, String name) {
        BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); window.paint(g); g.dispose();
        try { ImageIO.write(image, "png", Path.of(name).toFile()); } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }
    private static void await(java.util.function.BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            AtomicBoolean done = new AtomicBoolean();
            while (!done.get()) { SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean())); Thread.sleep(20); }
        });
    }
}
