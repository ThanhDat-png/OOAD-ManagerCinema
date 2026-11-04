package cinema.view;

import cinema.controller.auth.DangNhapBUS;
import cinema.controller.auth.PasswordHasher;
import cinema.dao.TaiKhoanDAO;
import cinema.model.TaiKhoan;
import cinema.model.VaiTro;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DangNhapWindowTest {
    private static final String HASH = PasswordHasher.hash("Cinema@123".toCharArray());

    @AfterEach void closeWindows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Window window : Window.getWindows()) window.dispose();
        });
    }

    private DangNhapWindow submit(VaiTro role, String status, String password) throws Exception {
        DangNhapWindow[] login = new DangNhapWindow[1];
        SwingUtilities.invokeAndWait(() -> {
            TaiKhoanDAO dao = new TaiKhoanDAO() {
                @Override public Optional<TaiKhoan> findByUsername(String name) {
                    return Optional.of(new TaiKhoan(1, name, HASH, status, role, null));
                }
            };
            login[0] = new DangNhapWindow(new DangNhapBUS(dao));
            login[0].setVisible(true);
            List<Component> components = components(login[0]);
            components.stream().filter(c -> c instanceof JTextField && !(c instanceof JPasswordField))
                    .forEach(c -> ((JTextField)c).setText("demo"));
            components.stream().filter(c -> c instanceof JPasswordField)
                    .forEach(c -> ((JPasswordField)c).setText(password));
            components.stream().filter(c -> c instanceof JButton)
                    .forEach(c -> ((JButton)c).doClick());
        });
        return login[0];
    }

    @Test void opensRoleWorkspaceFromLoginButton() throws Exception {
        for (VaiTro role : VaiTro.values()) {
            DangNhapWindow login = submit(role, "Đang hoạt động", "Cinema@123");
            await(() -> !login.isDisplayable());
            SwingUtilities.invokeAndWait(() -> {
                JFrame workspace = java.util.Arrays.stream(JFrame.getFrames())
                        .filter(f -> f instanceof JFrame && f.isDisplayable() && f.getTitle().endsWith(role.getTen()))
                        .map(f -> (JFrame)f).findFirst().orElseThrow();
                JList<?> list = components(workspace).stream().filter(c -> c instanceof JList)
                        .map(c -> (JList<?>)c).findFirst().orElseThrow();
                assertEquals(role == VaiTro.ADMIN ? 6 : 5, list.getModel().getSize());
                workspace.dispose();
            });
        }
    }

    @Test void rejectsLockedAccountAndKeepsLoginOpen() throws Exception {
        DangNhapWindow login = submit(VaiTro.NHAN_VIEN, "Bị khóa", "Cinema@123");
        await(() -> components(login).stream().filter(c -> c instanceof JButton)
                .allMatch(Component::isEnabled));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(login.isDisplayable());
            assertTrue(components(login).stream().filter(c -> c instanceof JLabel)
                    .anyMatch(c -> ((JLabel)c).getText().contains("Tài khoản không được phép truy cập")));
            components(login).stream().filter(c -> c instanceof JPasswordField)
                    .forEach(c -> assertEquals(0, ((JPasswordField)c).getPassword().length));
        });
    }

    @Test void demoSqlHashesUseApplicationFormat() throws Exception {
        String sql = Files.readString(Path.of("Database", "002_demo_uc01.sql"));
        var hashes = java.util.regex.Pattern.compile("pbkdf2-sha256\\$[^']+").matcher(sql);
        int count = 0;
        while (hashes.find()) {
            assertTrue(PasswordHasher.verify("Cinema@123".toCharArray(), hashes.group()));
            count++;
        }
        assertEquals(4, count);
    }

    private static List<Component> components(Container parent) {
        List<Component> result = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            result.add(component);
            if (component instanceof Container child) result.addAll(components(child));
        }
        return result;
    }

    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            AtomicBoolean done = new AtomicBoolean();
            while (!done.get()) {
                SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean()));
                Thread.sleep(20);
            }
        });
    }
}
