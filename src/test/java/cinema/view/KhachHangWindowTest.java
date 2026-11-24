package cinema.view;

import cinema.controller.auth.DangNhapBUS;
import cinema.controller.auth.PhienDangNhap;
import cinema.controller.customer.KhachHangBUS;
import cinema.controller.sales.BanVeBUS;
import cinema.dao.BanVeDAO;
import cinema.dao.KhachHangDAO;
import cinema.dao.TaiKhoanDAO;
import cinema.model.*;
import cinema.view.ui.Design;
import java.awt.Component;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KhachHangWindowTest {
    private static final String REF = "CAFEBABE0123456789ABCDEF";
    private static final Phim PHIM = new Phim(
            "PH001", "Mắt biếc", 117, "Victor Vũ", LocalDate.of(2026, 11, 1),
            13, "data/posters/11111111-1111-4111-8111-111111111101.jpg", "Đang chiếu", "2D", List.of());
    private static final SuatChieu SUAT = new SuatChieu(
            15, "PH001", "PHONG01", 1,
            LocalDateTime.of(2026, 11, 25, 19, 30),
            LocalDateTime.of(2026, 11, 25, 21, 45), "Sắp chiếu");

    @AfterEach
    void closeWindows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Window w : Window.getWindows()) w.dispose();
        });
    }

    @Test
    void customerBrowsesFilmSelectsSeatsAndReceivesReferenceCodeInSwing() throws Exception {
        KhachHangDAO dao = new KhachHangDAO() {
            @Override public List<ChonGheKhach.PhimChieu> danhSachPhim() {
                return List.of(new ChonGheKhach.PhimChieu(PHIM, "Tình cảm"));
            }
            @Override public Optional<ChonGheKhach.PhimChieu> chiTietPhim(String maPhim) {
                return Optional.of(new ChonGheKhach.PhimChieu(PHIM, "Tình cảm"));
            }
            @Override public List<ChonGheKhach.SuatKhach> suatChieuCuaPhim(String maPhim) {
                return List.of(new ChonGheKhach.SuatKhach(SUAT, PHIM, "Phòng chiếu 1"));
            }
            @Override public Optional<ChonGheKhach.SuatKhach> chiTietSuat(int maSuat) {
                return Optional.of(new ChonGheKhach.SuatKhach(SUAT, PHIM, "Phòng chiếu 1"));
            }
            @Override public List<ChonGheKhach.GheSanh> soDoGhe(int maSuat) {
                return List.of(
                        new ChonGheKhach.GheSanh(101, "A", 1, "A1", "Ghế thường", new BigDecimal("85000"), true),
                        new ChonGheKhach.GheSanh(102, "A", 2, "A2", "Ghế thường", new BigDecimal("85000"), false));
            }
            @Override public String taoMaChonGhe(int maSuat, Set<Integer> maGhe) {
                assertEquals(15, maSuat);
                assertEquals(Set.of(101), maGhe);
                return REF;
            }
            @Override public Optional<ChonGheKhach> traCuuMa(String maChonGhe) {
                return Optional.of(new ChonGheKhach(
                        REF, 15, "PH001", "Mắt biếc", "PHONG01", "Phòng chiếu 1",
                        SUAT.thoiGianBatDau(), SUAT.thoiGianBatDau().minusMinutes(30),
                        null, List.of(new ChonGheKhach.GheChon(101, "A1", "Ghế thường", new BigDecimal("85000"))),
                        new BigDecimal("85000"), false, false, false));
            }
        };

        KhachHangWindow[] win = new KhachHangWindow[1];
        SwingUtilities.invokeAndWait(() -> {
            win[0] = new KhachHangWindow(new KhachHangBUS(dao));
            win[0].setSize(1200, 780);
            win[0].setVisible(true);
        });
        await(() -> labels(win[0]).contains("Mắt biếc"));
        SwingUtilities.invokeAndWait(() -> capture(win[0], "customer-home.png"));

        SwingUtilities.invokeAndWait(() -> clickButtonStartingWith(win[0], "Xem giờ chiếu"));
        await(() -> buttons(win[0]).stream().anyMatch(b -> b.getText() != null && b.getText().contains("19:30")));
        SwingUtilities.invokeAndWait(() -> capture(win[0], "customer-film.png"));

        SwingUtilities.invokeAndWait(() -> clickButtonContaining(win[0], "19:30"));
        await(() -> toggleSeat(win[0], "A1") != null);

        SwingUtilities.invokeAndWait(() -> {
            JToggleButton a1 = toggleSeat(win[0], "A1");
            JToggleButton a2 = toggleSeat(win[0], "A2");
            assertNotNull(a1);
            assertNotNull(a2);
            assertTrue(a1.isEnabled());
            assertFalse(a2.isEnabled());
            a1.doClick();
        });
        await(() -> labels(win[0]).contains("85.000") || labels(win[0]).contains("85,000") || labels(win[0]).contains("85000"));
        SwingUtilities.invokeAndWait(() -> capture(win[0], "customer-seats.png"));

        SwingUtilities.invokeAndWait(() -> clickButtonStartingWith(win[0], "Lấy mã đặt vé"));
        await(() -> labels(win[0]).contains(REF));
        SwingUtilities.invokeAndWait(() -> capture(win[0], "customer-selection.png"));
    }

    @Test
    void loginWindowExposesCustomerCinemaEntryAndCounterClaimsSelection() throws Exception {
        DangNhapWindow[] login = new DangNhapWindow[1];
        SwingUtilities.invokeAndWait(() -> {
            login[0] = new DangNhapWindow(new DangNhapBUS(new TaiKhoanDAO() {
                @Override public Optional<TaiKhoan> findByUsername(String name) { return Optional.empty(); }
            }));
            login[0].setSize(1100, 720);
            login[0].setVisible(true);
            AbstractButton openCustomer = Design.all(login[0]).stream()
                    .filter(c -> c instanceof AbstractButton b && b.getText() != null && b.getText().contains("Xem phim và lịch chiếu"))
                    .map(c -> (AbstractButton) c).findFirst().orElseThrow();
            openCustomer.doClick();
        });
        await(() -> Arrays.stream(Window.getWindows()).anyMatch(w -> w instanceof KhachHangWindow && w.isDisplayable()));

        NhanVien nv = new NhanVien("NV002", "Trần Thị Bình", LocalDate.of(1999, 5, 10),
                "0901234567", "binh@cinema.local", "TP.HCM", "Nhân viên bán vé");
        PhienDangNhap staffSession = new PhienDangNhap(2, "staff_demo", VaiTro.NHAN_VIEN, nv);
        PhongChieu phong = new PhongChieu("PHONG01", new LoaiPhong(1, "Phòng 2D", "2D"),
                "Phòng chiếu 1", 15, "Đang hoạt động");
        Ghe ghe = new Ghe(101, "PHONG01", new LoaiGhe(1, "Ghế thường"), "A", 1, "Sẵn sàng sử dụng");
        GiaoDich gd = new GiaoDich("G9999999999999999999", "NV002",
                LocalDateTime.now(), new BigDecimal("85000"), "Chờ thanh toán");
        GheSuatChieu gsc = new GheSuatChieu(15, ghe, gd.maGiaoDich(), "Đang giữ",
                LocalDateTime.now().plusMinutes(5), 1, new BigDecimal("85000"));
        BanVeDAO.GioHang claimedCart = new BanVeDAO.GioHang(gd, SUAT, phong, "Mắt biếc", List.of(gsc), null);

        BanVeDAO fakeSalesDao = new BanVeDAO() {
            @Override public List<SuatBan> search(String name, LocalDate date, LocalTime time) {
                return List.of(new SuatBan(SUAT, "Mắt biếc", "Phòng chiếu 1"));
            }
            @Override public List<GheSuatChieu> map(int show) { return List.of(gsc); }
            @Override public GioHang inspect(String id) { return claimedCart; }
        };
        KhachHangDAO fakeCustomerDao = new KhachHangDAO() {
            @Override public List<ChonGheKhach.YeuCauCho> danhSachChoTiepNhan() {
                return List.of(new ChonGheKhach.YeuCauCho(REF, 15, "Mắt biếc", "Phòng chiếu 1", SUAT.thoiGianBatDau(), "A1"));
            }
            @Override public ChonGheKhach.KetQuaTiepNhan tiepNhanMa(String maChonGhe, String maNV, String current, int holdMinutes) {
                return new ChonGheKhach.KetQuaTiepNhan(claimedCart, 15, false);
            }
        };

        JFrame[] host = new JFrame[1];
        BanVePanel[] sales = new BanVePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            sales[0] = new BanVePanel(staffSession, new BanVeBUS(staffSession, fakeSalesDao, fakeCustomerDao, 5));
            host[0] = new JFrame("Test Sales");
            host[0].setContentPane(sales[0]);
            host[0].setSize(1200, 780);
            host[0].setVisible(true);
        });
        await(() -> buttons(sales[0]).stream().anyMatch(b -> "Tìm suất".equals(b.getText()) && b.isEnabled()));

        SwingUtilities.invokeLater(() -> clickButtonStartingWith(sales[0], "Nhận mã khách"));
        await(() -> Arrays.stream(Window.getWindows()).anyMatch(w -> w instanceof JDialog d && d.isShowing() && d.getTitle().contains("Nhận mã chọn ghế")));

        SwingUtilities.invokeAndWait(() -> {
            JDialog dialog = Arrays.stream(Window.getWindows())
                    .filter(w -> w instanceof JDialog d && d.isShowing() && d.getTitle().contains("Nhận mã chọn ghế"))
                    .map(w -> (JDialog) w).findFirst().orElseThrow();
            JList<?> list = Design.all(dialog).stream().filter(c -> c instanceof JList<?> l && l.getModel().getSize() > 0)
                    .map(c -> (JList<?>) c).findFirst().orElseThrow();
            list.setSelectedIndex(0);
            clickButtonStartingWith(dialog, "Nhận yêu cầu");
        });
        await(() -> textAreas(sales[0]).contains("G9999999999999999999") && textAreas(sales[0]).contains("85000 VND"));
    }

    private static String labels(java.awt.Container root) {
        return Design.all(root).stream().filter(c -> c instanceof JLabel)
                .map(c -> ((JLabel) c).getText()).filter(Objects::nonNull).reduce("", (a, b) -> a + " " + b);
    }

    private static String textAreas(java.awt.Container root) {
        return Design.all(root).stream().filter(c -> c instanceof JTextArea)
                .map(c -> ((JTextArea) c).getText()).filter(Objects::nonNull).reduce("", (a, b) -> a + "\n" + b);
    }

    private static List<AbstractButton> buttons(java.awt.Container root) {
        return Design.all(root).stream().filter(c -> c instanceof AbstractButton)
                .map(c -> (AbstractButton) c).toList();
    }

    private static JToggleButton toggleSeat(java.awt.Container root, String seatCode) {
        return Design.all(root).stream()
                .filter(c -> c instanceof JToggleButton b && (seatCode.equals(b.getText()) || seatCode.equals(b.getClientProperty("seatCode"))))
                .map(c -> (JToggleButton) c).findFirst().orElse(null);
    }

    private static void clickButtonStartingWith(java.awt.Container root, String prefix) {
        buttons(root).stream().filter(b -> b.isShowing() && b.getText() != null && b.getText().startsWith(prefix))
                .findFirst().orElseThrow(() -> new AssertionError("Missing button starting with: " + prefix)).doClick();
    }

    private static void clickButtonContaining(java.awt.Container root, String text) {
        buttons(root).stream().filter(b -> b.isShowing() && b.getText() != null && b.getText().contains(text))
                .findFirst().orElseThrow(() -> new AssertionError("Missing button containing: " + text)).doClick();
    }

    private static void capture(Window window, String name) {
        try {
            var dir = java.nio.file.Path.of("target", "ui-redesign", "customer");
            java.nio.file.Files.createDirectories(dir);
            var img = new java.awt.image.BufferedImage(
                    window.getWidth(), window.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics();
            window.paint(g);
            g.dispose();
            javax.imageio.ImageIO.write(img, "png", dir.resolve(name).toFile());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void await(BooleanSupplier condition) throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            AtomicBoolean ok = new AtomicBoolean();
            while (!ok.get()) {
                SwingUtilities.invokeAndWait(() -> ok.set(condition.getAsBoolean()));
                Thread.sleep(20);
            }
        });
    }
}
