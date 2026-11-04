package cinema.integration;

import cinema.controller.auth.DangNhapBUS;
import cinema.dao.TaiKhoanDAO;
import cinema.view.DangNhapWindow;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests", matches="true")
class DesktopMySQLTest {
    @AfterEach void close() throws Exception {
        SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) w.dispose(); });
    }

    private void openStaff(String username, String workspace, String staffTitle) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DangNhapWindow login = new DangNhapWindow(new DangNhapBUS(new TaiKhoanDAO()));
            login.setVisible(true);
            for (Component c : components(login)) {
                if (c instanceof JPasswordField field) field.setText("Cinema@123");
                else if (c instanceof JTextField field) field.setText(username);
            }
            click(login, "Đăng nhập");
        });
        await(() -> frame(workspace) != null);
        SwingUtilities.invokeAndWait(() -> click(frame(workspace), "Mở chức năng"));
        await(() -> frame(staffTitle) != null && components(frame(staffTitle)).stream()
                .anyMatch(c -> c instanceof JTable t && t.getRowCount() > 0));
    }

    @Test void adminOpensAndSearchesEmployeeListThroughRealLogin() throws Exception {
        openStaff("admin_demo", "Quản lý rạp chiếu phim — Admin", "Quản lý nhân viên");
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = frame("Quản lý nhân viên");
            for (Component c : components(frame)) if (c instanceof JTextField field) field.setText("NV002");
            click(frame, "Tìm kiếm");
        });
        await(() -> components(frame("Quản lý nhân viên")).stream()
                .anyMatch(c -> c instanceof JTable t && t.getRowCount() == 1 && "NV002".equals(t.getValueAt(0, 0))));
        SwingUtilities.invokeAndWait(() -> screenshot(frame("Quản lý nhân viên"), "target/uc02-admin.png"));
    }

    @Test void employeeSeesOnlyOwnProfileThroughRealLogin() throws Exception {
        openStaff("staff_demo", "Quản lý rạp chiếu phim — Nhân viên", "Thông tin cá nhân");
        SwingUtilities.invokeAndWait(() -> {
            List<Component> components = components(frame("Thông tin cá nhân"));
            JTable table = components.stream().filter(c -> c instanceof JTable).map(c -> (JTable)c).findFirst().orElseThrow();
            assertEquals(1, table.getRowCount());
            assertEquals("NV002", table.getValueAt(0, 0));
            assertTrue(components.stream().noneMatch(c -> c instanceof JButton b && b.getText().contains("Xóa")));
            screenshot(frame("Thông tin cá nhân"), "target/uc02-staff.png");
        });
    }

    private static JFrame frame(String title) {
        for (Frame frame : JFrame.getFrames())
            if (frame instanceof JFrame f && f.isDisplayable() && title.equals(f.getTitle())) return f;
        return null;
    }

    private static List<Component> components(Container parent) {
        List<Component> result = new ArrayList<>();
        for (Component c : parent.getComponents()) {
            result.add(c);
            if (c instanceof Container child) result.addAll(components(child));
        }
        return result;
    }

    private static void click(Container parent, String title) {
        components(parent).stream().filter(c -> c instanceof JButton b && title.equals(b.getText()))
                .map(c -> (JButton)c).findFirst().orElseThrow().doClick();
    }

    private static void screenshot(JFrame frame, String path) {
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        frame.paint(graphics); graphics.dispose();
        try { ImageIO.write(image, "png", new File(path)); }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    private static void await(java.util.function.BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            AtomicBoolean done = new AtomicBoolean();
            while (!done.get()) {
                SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean()));
                Thread.sleep(20);
            }
        });
    }
}
