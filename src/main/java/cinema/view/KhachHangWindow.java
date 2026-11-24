package cinema.view;

import cinema.controller.auth.DangNhapBUS;
import cinema.controller.customer.KhachHangBUS;
import cinema.controller.film.PosterStore;
import cinema.dao.TaiKhoanDAO;
import cinema.model.ChonGheKhach;
import cinema.model.Phim;
import cinema.view.ui.Icons;
import cinema.view.ui.Theme;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTextFieldUI;

public final class KhachHangWindow extends JFrame {
    static { Theme.install(); }

    private static final Color BG = Color.decode("#11100F");
    private static final Color SURFACE = Color.decode("#1A1816");
    private static final Color RAISED = Color.decode("#262320");
    private static final Color LINE = Color.decode("#3B3631");
    private static final Color TEXT = Color.decode("#F7F1E8");
    private static final Color MUTED = Color.decode("#B5ABA0");
    private static final Color ACCENT = Color.decode("#F0A463");
    private static final Color ACCENT_INK = Color.decode("#1B1209");
    private static final Color DANGER = Color.decode("#F59E9A");
    private static final Color SUCCESS = Color.decode("#6EE7B7");

    private static final Color SEAT_AVAIL_FILL = Color.decode("#2B2724");
    private static final Color SEAT_AVAIL_BORDER = Color.decode("#544D46");
    private static final Color SEAT_UNAVAIL_FILL = Color.decode("#141210");
    private static final Color SEAT_UNAVAIL_BORDER = Color.decode("#2A2623");
    private static final Color SEAT_UNAVAIL_TEXT = Color.decode("#5A534C");

    private static final DateTimeFormatter SHOW_FMT = DateTimeFormatter.ofPattern("dd/MM · HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_PILL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter EXPIRY_FMT = DateTimeFormatter.ofPattern("HH:mm · dd/MM/uuuu");
    private static final DateTimeFormatter RELEASE_FMT = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final KhachHangBUS service;
    private final PosterStore posters = new PosterStore();
    private final Map<String, BufferedImage> posterCache = new HashMap<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JLabel statusBanner = new JLabel(" ");
    private final JTextField lookupInput = new JTextField(20) {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), arc = 10;
            g2.setColor(BG);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.setColor(hasFocus() ? ACCENT : LINE);
            g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D gp = (Graphics2D) g.create();
                gp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gp.setColor(MUTED);
                gp.setFont(Theme.font(12));
                FontMetrics fm = gp.getFontMetrics();
                gp.drawString("Nhập mã 24 ký tự...", 12, (h + fm.getAscent() - fm.getDescent()) / 2);
                gp.dispose();
            }
        }
    };
    private boolean busy;

    public static void open() {
        for (Window w : Window.getWindows()) {
            if (w instanceof KhachHangWindow existing && existing.isDisplayable()) {
                existing.setVisible(true);
                existing.toFront();
                existing.loadHome();
                return;
            }
        }
        KhachHangWindow window = new KhachHangWindow();
        window.setVisible(true);
    }

    public KhachHangWindow() {
        this(new KhachHangBUS());
    }

    public KhachHangWindow(KhachHangBUS service) {
        super("Phim tại rạp — Cinema");
        this.service = Objects.requireNonNull(service);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                closeCustomerWindow();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.putClientProperty("cinema.standaloneTheme", true);

        root.add(buildHeader(), BorderLayout.NORTH);
        body.setBackground(BG);
        root.add(body, BorderLayout.CENTER);

        statusBanner.setForeground(MUTED);
        statusBanner.setFont(Theme.font(13));
        statusBanner.setBorder(new EmptyBorder(10, 28, 12, 28));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE));
        footer.add(statusBanner, BorderLayout.WEST);
        JLabel note = new JLabel("Giá hiển thị là tạm tính · Thanh toán và nhận vé QR tại quầy   ");
        note.setForeground(MUTED);
        note.setFont(Theme.font(12));
        footer.add(note, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1320, screen.width - 48), Math.min(840, screen.height - 56));
        setMinimumSize(new Dimension(920, 620));
        setLocationRelativeTo(null);
        styleCinemaTree(root);
        loadHome();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(14, 28, 14, 28)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        JComponent mark = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(36, 36); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 26));
                g2.fillOval(1, 1, 33, 33);
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(2, 2, 31, 31);
                Polygon play = new Polygon(new int[]{15, 15, 24}, new int[]{12, 24, 18}, 3);
                g2.fillPolygon(play);
                g2.dispose();
            }
        };
        JPanel word = new JPanel(new GridLayout(2, 1, 0, 1));
        word.setOpaque(false);
        JLabel brand = new JLabel("CINEMA");
        brand.setFont(trackedFont(Theme.bold(18), 0.04f));
        brand.setForeground(TEXT);
        JLabel sub = new JLabel("PHIM TẠI RẠP");
        sub.setFont(trackedFont(Theme.bold(10), 0.08f));
        sub.setForeground(ACCENT);
        word.add(brand);
        word.add(sub);
        left.add(mark);
        left.add(word);

        JButton homeBtn = cinemaButton("Phim tại rạp", false);
        homeBtn.addActionListener(e -> loadHome());
        left.add(Box.createHorizontalStrut(10));
        left.add(homeBtn);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel codeLbl = new JLabel("Mã đặt vé:");
        codeLbl.setForeground(MUTED);
        codeLbl.setFont(Theme.font(13));
        lookupInput.setToolTipText("Nhập mã đặt vé 24 ký tự để tra cứu");
        styleField(lookupInput);
        JButton lookupBtn = cinemaButton("Tra cứu mã", false);
        Runnable doLookup = () -> {
            String raw = lookupInput.getText().strip();
            if (!KhachHangBUS.hopLeMaChonGhe(raw)) {
                showError("Vui lòng nhập mã đặt vé hợp lệ gồm 24 ký tự hex.");
                return;
            }
            openSelection(raw);
        };
        lookupInput.addActionListener(e -> doLookup.run());
        lookupBtn.addActionListener(e -> doLookup.run());

        JButton staffBtn = cinemaButton("Quầy nội bộ", false);
        staffBtn.setIcon(new Icons("people", 16));
        staffBtn.addActionListener(e -> openInternalLoginOrFocus());

        right.add(codeLbl);
        right.add(lookupInput);
        right.add(lookupBtn);
        right.add(staffBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void openInternalLoginOrFocus() {
        for (Window w : Window.getWindows()) {
            if (w != this && w instanceof JFrame frame && frame.isDisplayable()) {
                frame.setVisible(true);
                frame.toFront();
                return;
            }
        }
        new DangNhapWindow(new DangNhapBUS(new TaiKhoanDAO())).setVisible(true);
    }

    private void closeCustomerWindow() {
        boolean otherFrameOpen = Arrays.stream(Window.getWindows())
                .anyMatch(w -> w != this && w instanceof JFrame && w.isDisplayable());
        dispose();
        if (!otherFrameOpen) {
            new DangNhapWindow(new DangNhapBUS(new TaiKhoanDAO())).setVisible(true);
        }
    }

    public void loadHome() {
        run(service::danhSachPhim, films -> {
            List<ChonGheKhach.PhimChieu> nowShowing = films.stream()
                    .filter(f -> "Đang chiếu".equals(f.phim().trangThai())).toList();
            List<ChonGheKhach.PhimChieu> upcoming = films.stream()
                    .filter(f -> "Sắp chiếu".equals(f.phim().trangThai())).toList();
            ChonGheKhach.PhimChieu featured = !nowShowing.isEmpty() ? nowShowing.get(0)
                    : (!upcoming.isEmpty() ? upcoming.get(0) : null);
            showCard("home", buildHomeView(featured, nowShowing, upcoming));
            statusBanner.setForeground(MUTED);
            statusBanner.setText("Chọn phim để xem lịch chiếu, chọn ghế và nhận mã xuất trình tại quầy.");
        });
    }

    private JComponent buildHomeView(
            ChonGheKhach.PhimChieu featured,
            List<ChonGheKhach.PhimChieu> nowShowing,
            List<ChonGheKhach.PhimChieu> upcoming) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(24, 32, 36, 32));

        if (featured != null) {
            content.add(buildMarquee(featured));
            content.add(Box.createVerticalStrut(28));
        }

        content.add(sectionTitle("Phim đang chiếu"));
        content.add(Box.createVerticalStrut(14));
        if (nowShowing.isEmpty()) {
            content.add(emptyNotice("Hiện chưa có phim đang chiếu."));
        } else {
            content.add(buildFilmGrid(nowShowing));
        }

        if (!upcoming.isEmpty()) {
            content.add(Box.createVerticalStrut(28));
            content.add(sectionTitle("Phim sắp chiếu"));
            content.add(Box.createVerticalStrut(14));
            content.add(buildFilmGrid(upcoming));
        }
        return wrapScroll(content);
    }

    private JPanel buildMarquee(ChonGheKhach.PhimChieu item) {
        Phim p = item.phim();
        BufferedImage art = loadPoster(p.poster());
        JPanel marquee = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth(), h = getHeight();
                Shape clip = new RoundRectangle2D.Float(0, 0, w - 1f, h - 2f, 16, 16);
                g2.setColor(new Color(0, 0, 0, 75));
                g2.fillRoundRect(0, 2, w - 1, h - 2, 16, 16);
                g2.setClip(clip);
                g2.setColor(SURFACE);
                g2.fillRect(0, 0, w, h);
                if (art != null) {
                    int drawW = (int) (w * 0.56);
                    int drawX = w - drawW;
                    double scale = Math.max((double) drawW / art.getWidth(), (double) h / art.getHeight());
                    int iw = (int) (art.getWidth() * scale);
                    int ih = (int) (art.getHeight() * scale);
                    g2.drawImage(art, drawX + (drawW - iw) / 2, (h - ih) / 2, iw, ih, null);
                    g2.setPaint(new GradientPaint(drawX, 0, SURFACE,
                            drawX + (int) (drawW * 0.72), 0,
                            new Color(SURFACE.getRed(), SURFACE.getGreen(), SURFACE.getBlue(), 45)));
                    g2.fillRect(drawX, 0, drawW, h);
                }
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRect(0, 1, w, 1);
                g2.setClip(null);
                g2.setColor(LINE);
                g2.draw(clip);
                g2.dispose();
            }
        };
        marquee.setOpaque(false);
        marquee.setAlignmentX(Component.LEFT_ALIGNMENT);
        marquee.setMaximumSize(new Dimension(Integer.MAX_VALUE, 268));
        marquee.setPreferredSize(new Dimension(960, 248));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel eyebrow = pillBadge(
                "Sắp chiếu".equals(p.trangThai()) ? "PHIM SẮP CHIẾU" : "PHIM NỔI BẬT",
                ACCENT, new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 28),
                new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 110));
        JLabel title = new JLabel(p.tenPhim());
        title.setForeground(TEXT);
        title.setFont(trackedFont(Theme.bold(32), -0.02f));
        JLabel meta = new JLabel("<html>" + filmMeta(item) + "</html>");
        meta.setForeground(MUTED);
        meta.setFont(Theme.font(14));
        JButton cta = cinemaButton("Xem giờ chiếu →", true);
        cta.addActionListener(e -> openFilm(p.maPhim(), null));

        for (JComponent c : new JComponent[]{eyebrow, title, meta, cta}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        inner.add(eyebrow);
        inner.add(Box.createVerticalStrut(12));
        inner.add(title);
        inner.add(Box.createVerticalStrut(10));
        inner.add(meta);
        inner.add(Box.createVerticalStrut(22));
        inner.add(cta);

        marquee.add(inner, BorderLayout.WEST);
        return marquee;
    }

    private JPanel buildFilmGrid(List<ChonGheKhach.PhimChieu> films) {
        JPanel grid = new JPanel(new GridLayout(0, 3, 20, 20)) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (ChonGheKhach.PhimChieu item : films) {
            grid.add(buildFilmCard(item));
        }
        int rem = films.size() % 3;
        if (rem != 0) {
            for (int i = rem; i < 3; i++) {
                JPanel empty = new JPanel();
                empty.setOpaque(false);
                grid.add(empty);
            }
        }
        return grid;
    }

    private JPanel buildFilmCard(ChonGheKhach.PhimChieu item) {
        Phim p = item.phim();
        JPanel card = cinemaCard(new BorderLayout(0, 8));
        card.add(posterBox(loadPoster(p.poster()), p.dinhDangChieu() + " · " + ageBadge(p.gioiHanTuoi()), 220, true),
                BorderLayout.NORTH);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(4, 18, 18, 18));

        JLabel title = new JLabel(p.tenPhim());
        title.setForeground(TEXT);
        title.setFont(Theme.bold(18));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel meta = new JLabel("<html>" + filmMeta(item) + "</html>");
        meta.setForeground(MUTED);
        meta.setFont(Theme.font(13));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton action = cinemaButton("Xem giờ chiếu", false);
        action.setAlignmentX(Component.LEFT_ALIGNMENT);
        action.addActionListener(e -> openFilm(p.maPhim(), null));

        info.add(title);
        info.add(Box.createVerticalStrut(6));
        info.add(meta);
        info.add(Box.createVerticalStrut(16));
        info.add(action);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private record FilmScreenData(ChonGheKhach.PhimChieu film, List<ChonGheKhach.SuatKhach> shows) {}

    private void openFilm(String maPhim, LocalDate preferredDate) {
        run(() -> new FilmScreenData(service.chiTietPhim(maPhim), service.suatChieuCuaPhim(maPhim)),
                data -> renderFilmView(data.film(), data.shows(), preferredDate));
    }

    private void renderFilmView(
            ChonGheKhach.PhimChieu item,
            List<ChonGheKhach.SuatKhach> shows,
            LocalDate preferredDate) {
        Phim p = item.phim();
        List<LocalDate> dates = shows.stream()
                .map(s -> s.suat().thoiGianBatDau().toLocalDate()).distinct().toList();
        LocalDate activeDate = (preferredDate != null && dates.contains(preferredDate))
                ? preferredDate : (dates.isEmpty() ? null : dates.get(0));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(24, 32, 36, 32));

        JButton back = cinemaButton("← Phim tại rạp", false);
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(e -> loadHome());
        content.add(back);
        content.add(Box.createVerticalStrut(18));

        JPanel hero = cinemaCard(new BorderLayout(24, 0));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.setBorder(new EmptyBorder(22, 24, 22, 24));

        JComponent poster = posterBox(loadPoster(p.poster()), null, 220, false);
        poster.setPreferredSize(new Dimension(152, 220));
        hero.add(poster, BorderLayout.WEST);

        JPanel metaBox = new JPanel();
        metaBox.setOpaque(false);
        metaBox.setLayout(new BoxLayout(metaBox, BoxLayout.Y_AXIS));

        JLabel status = pillBadge(
                p.trangThai().toUpperCase(Locale.ROOT),
                ACCENT, new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 26),
                new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 100));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(p.tenPhim());
        title.setForeground(TEXT);
        title.setFont(trackedFont(Theme.bold(28), -0.02f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pillRow.setOpaque(false);
        pillRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pillRow.add(pillBadge(p.dinhDangChieu(), TEXT, RAISED, LINE));
        pillRow.add(pillBadge(ageBadge(p.gioiHanTuoi()), ACCENT, RAISED, LINE));
        pillRow.add(pillBadge(p.thoiLuong() + " phút", MUTED, RAISED, LINE));
        if (item.theLoai() != null && !item.theLoai().isBlank()) {
            pillRow.add(pillBadge(item.theLoai(), MUTED, RAISED, LINE));
        }

        String directorText = "Đạo diễn: " + p.daoDien();
        if (p.ngayKhoiChieu() != null) {
            directorText += "   ·   Khởi chiếu: " + p.ngayKhoiChieu().format(RELEASE_FMT);
        }
        JLabel detail = new JLabel(directorText);
        detail.setForeground(MUTED);
        detail.setFont(Theme.font(14));
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        metaBox.add(status);
        metaBox.add(Box.createVerticalStrut(10));
        metaBox.add(title);
        metaBox.add(Box.createVerticalStrut(12));
        metaBox.add(pillRow);
        metaBox.add(Box.createVerticalStrut(12));
        metaBox.add(detail);
        hero.add(metaBox, BorderLayout.CENTER);

        JPanel quickInfo = new JPanel();
        quickInfo.setOpaque(false);
        quickInfo.setLayout(new BoxLayout(quickInfo, BoxLayout.Y_AXIS));
        quickInfo.setPreferredSize(new Dimension(250, 0));
        quickInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, LINE),
                new EmptyBorder(4, 22, 4, 4)));

        JLabel quickEyebrow = new JLabel("THÔNG TIN SUẤT CHIẾU");
        quickEyebrow.setForeground(ACCENT);
        quickEyebrow.setFont(trackedFont(Theme.bold(11), 0.06f));
        quickEyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        String earliest = shows.isEmpty() ? "Đang cập nhật"
                : shows.stream()
                .map(s -> s.suat().thoiGianBatDau())
                .min(Comparator.naturalOrder())
                .map(t -> t.format(SHOW_FMT))
                .orElse("Đang cập nhật");

        JPanel countRow = detailBlock("Suất khả dụng", shows.isEmpty() ? "Chưa mở suất" : shows.size() + " suất");
        countRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel earliestRow = detailBlock("Suất gần nhất", earliest);
        earliestRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel noteLbl = new JLabel("<html>Chọn ngày và giờ chiếu bên dưới để giữ ghế.</html>");
        noteLbl.setForeground(MUTED);
        noteLbl.setFont(Theme.font(12));
        noteLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        quickInfo.add(quickEyebrow);
        quickInfo.add(Box.createVerticalStrut(12));
        quickInfo.add(countRow);
        quickInfo.add(Box.createVerticalStrut(12));
        quickInfo.add(earliestRow);
        quickInfo.add(Box.createVerticalStrut(14));
        quickInfo.add(noteLbl);
        hero.add(quickInfo, BorderLayout.EAST);

        content.add(hero);

        if (!dates.isEmpty()) {
            content.add(Box.createVerticalStrut(24));
            content.add(sectionTitle("Chọn ngày"));
            content.add(Box.createVerticalStrut(10));
            JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            dateRow.setOpaque(false);
            dateRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (LocalDate d : dates) {
                JButton pill = cinemaButton(d.format(DAY_PILL), d.equals(activeDate));
                pill.addActionListener(e -> renderFilmView(item, shows, d));
                dateRow.add(pill);
            }
            content.add(dateRow);
        }

        content.add(Box.createVerticalStrut(24));
        content.add(sectionTitle("Giờ chiếu"));
        content.add(Box.createVerticalStrut(10));
        List<ChonGheKhach.SuatKhach> dayShows = activeDate == null ? List.of()
                : shows.stream().filter(s -> s.suat().thoiGianBatDau().toLocalDate().equals(activeDate)).toList();
        if (dayShows.isEmpty()) {
            content.add(emptyNotice("Chưa có suất chiếu còn nhận đặt ghế."));
        } else {
            JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            timeRow.setOpaque(false);
            timeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (ChonGheKhach.SuatKhach s : dayShows) {
                String label = s.suat().thoiGianBatDau().format(TIME_FMT) + "  ·  " + s.tenPhong();
                JButton slotBtn = cinemaButton(label, false);
                slotBtn.addActionListener(e -> openShowSeats(s.suat().maSuat(), null));
                timeRow.add(slotBtn);
            }
            content.add(timeRow);
        }

        showCard("film", wrapScroll(content));
    }

    private record SeatScreenData(ChonGheKhach.SuatKhach show, List<ChonGheKhach.GheSanh> seats) {}

    private void openShowSeats(int maSuat, String previousError) {
        run(() -> new SeatScreenData(service.chiTietSuat(maSuat), service.soDoGhe(maSuat)),
                data -> renderSeatView(data.show(), data.seats(), previousError));
    }

    private void renderSeatView(
            ChonGheKhach.SuatKhach showInfo,
            List<ChonGheKhach.GheSanh> seats,
            String errorText) {
        Set<Integer> selectedIds = new LinkedHashSet<>();
        Map<Integer, ChonGheKhach.GheSanh> seatById = new LinkedHashMap<>();
        for (ChonGheKhach.GheSanh s : seats) seatById.put(s.maGhe(), s);

        JPanel outer = new JPanel(new BorderLayout(20, 16));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(20, 28, 24, 28));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JButton back = cinemaButton("← Đổi suất chiếu", false);
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(e -> openFilm(showInfo.phim().maPhim(), showInfo.suat().thoiGianBatDau().toLocalDate()));
        JLabel title = new JLabel("Chọn ghế");
        title.setForeground(TEXT);
        title.setFont(trackedFont(Theme.bold(26), -0.02f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel(showInfo.phim().tenPhim() + "   ·   "
                + showInfo.suat().thoiGianBatDau().format(SHOW_FMT) + "   ·   " + showInfo.tenPhong());
        sub.setForeground(MUTED);
        sub.setFont(Theme.font(14));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(back);
        header.add(Box.createVerticalStrut(12));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);
        if (errorText != null && !errorText.isBlank()) {
            header.add(Box.createVerticalStrut(10));
            JPanel errBox = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    g2.setColor(new Color(DANGER.getRed(), DANGER.getGreen(), DANGER.getBlue(), 26));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                    g2.setColor(new Color(DANGER.getRed(), DANGER.getGreen(), DANGER.getBlue(), 130));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                    g2.dispose();
                }
            };
            errBox.setOpaque(false);
            errBox.setBorder(new EmptyBorder(8, 14, 8, 14));
            errBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel err = new JLabel(errorText);
            err.setForeground(DANGER);
            err.setFont(Theme.bold(13));
            errBox.add(err, BorderLayout.CENTER);
            header.add(errBox);
        }
        outer.add(header, BorderLayout.NORTH);

        JLabel chosenSeatsLabel = new JLabel("Chưa chọn ghế");
        chosenSeatsLabel.setForeground(TEXT);
        chosenSeatsLabel.setFont(Theme.bold(15));
        JLabel totalLabel = new JLabel("—");
        totalLabel.setForeground(ACCENT);
        totalLabel.setFont(Theme.number(24));
        JButton submitBtn = cinemaButton("Lấy mã đặt vé →", true);
        submitBtn.setEnabled(false);

        Runnable refreshSummary = () -> {
            if (selectedIds.isEmpty()) {
                chosenSeatsLabel.setText("Chưa chọn ghế");
                totalLabel.setText("—");
                submitBtn.setEnabled(false);
            } else {
                List<String> names = new ArrayList<>();
                BigDecimal sum = BigDecimal.ZERO;
                for (Integer id : selectedIds) {
                    ChonGheKhach.GheSanh s = seatById.get(id);
                    if (s != null) {
                        names.add(s.viTri());
                        if (s.giaTamTinh() != null) sum = sum.add(s.giaTamTinh());
                    }
                }
                chosenSeatsLabel.setText(String.join(", ", names));
                totalLabel.setText(formatMoney(sum));
                submitBtn.setEnabled(true);
            }
        };

        JPanel mapCard = cinemaCard(new BorderLayout(0, 14));
        mapCard.setBorder(new EmptyBorder(18, 22, 18, 22));
        mapCard.add(buildCinemaScreen(), BorderLayout.NORTH);

        JPanel rowsPanel = new JPanel();
        rowsPanel.setOpaque(false);
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));

        if (seats.isEmpty()) {
            rowsPanel.add(emptyNotice("Chưa có sơ đồ ghế cho suất chiếu này."));
        } else {
            Map<String, List<ChonGheKhach.GheSanh>> grouped = new LinkedHashMap<>();
            for (ChonGheKhach.GheSanh s : seats) {
                grouped.computeIfAbsent(s.hangGhe(), k -> new ArrayList<>()).add(s);
            }
            for (var entry : grouped.entrySet()) {
                JPanel rowLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
                rowLine.setOpaque(false);
                JLabel rowTag = new JLabel(entry.getKey(), SwingConstants.CENTER);
                rowTag.setPreferredSize(new Dimension(28, 44));
                rowTag.setForeground(MUTED);
                rowTag.setFont(Theme.bold(13));
                rowLine.add(rowTag);

                for (ChonGheKhach.GheSanh s : entry.getValue()) {
                    JToggleButton seatBtn = buildSeatTile(s, selectedIds, refreshSummary);
                    rowLine.add(seatBtn);
                }
                rowsPanel.add(rowLine);
            }
        }

        JPanel rowsWrap = new JPanel(new BorderLayout());
        rowsWrap.setOpaque(false);
        rowsWrap.add(rowsPanel, BorderLayout.NORTH);
        JScrollPane mapScroll = new JScrollPane(rowsWrap);
        mapScroll.setBorder(BorderFactory.createEmptyBorder());
        mapScroll.getViewport().setBackground(SURFACE);
        mapCard.add(mapScroll, BorderLayout.CENTER);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 6));
        legend.setOpaque(false);
        legend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, LINE),
                new EmptyBorder(10, 0, 0, 0)));
        legend.add(legendItem(SEAT_AVAIL_FILL, SEAT_AVAIL_BORDER, false, "Còn trống"));
        legend.add(legendItem(ACCENT, ACCENT, false, "Đang chọn"));
        legend.add(legendItem(SEAT_UNAVAIL_FILL, SEAT_UNAVAIL_BORDER, true, "Không khả dụng"));
        mapCard.add(legend, BorderLayout.SOUTH);

        JPanel summary = cinemaCard(new BorderLayout(0, 16));
        summary.setPreferredSize(new Dimension(316, 0));
        summary.setBorder(new EmptyBorder(22, 22, 22, 22));

        JPanel summaryBody = new JPanel();
        summaryBody.setOpaque(false);
        summaryBody.setLayout(new BoxLayout(summaryBody, BoxLayout.Y_AXIS));
        JLabel sumHeading = new JLabel("Ghế đã chọn");
        sumHeading.setForeground(TEXT);
        sumHeading.setFont(Theme.bold(18));
        JLabel seatsCaption = new JLabel("Vị trí ghế");
        seatsCaption.setForeground(MUTED);
        seatsCaption.setFont(Theme.font(12));
        JLabel totalCaption = new JLabel("Tạm tính");
        totalCaption.setForeground(MUTED);
        totalCaption.setFont(Theme.font(12));
        JLabel help = new JLabel("<html>Nhân viên sẽ kiểm tra ghế trống khi bạn xuất trình mã tại quầy.</html>");
        help.setForeground(MUTED);
        help.setFont(Theme.font(12));

        for (JComponent c : new JComponent[]{sumHeading, seatsCaption, chosenSeatsLabel, totalCaption, totalLabel, submitBtn, help}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        submitBtn.addActionListener(e -> run(
                () -> service.taoMaChonGhe(showInfo.suat().maSuat(), new ArrayList<>(selectedIds)),
                this::openSelection,
                err -> openShowSeats(showInfo.suat().maSuat(), err.getMessage())));

        summaryBody.add(sumHeading);
        summaryBody.add(Box.createVerticalStrut(14));
        summaryBody.add(seatsCaption);
        summaryBody.add(Box.createVerticalStrut(4));
        summaryBody.add(chosenSeatsLabel);
        summaryBody.add(Box.createVerticalStrut(16));
        summaryBody.add(totalCaption);
        summaryBody.add(Box.createVerticalStrut(4));
        summaryBody.add(totalLabel);
        summaryBody.add(Box.createVerticalStrut(22));
        summaryBody.add(submitBtn);
        summaryBody.add(Box.createVerticalStrut(14));
        summaryBody.add(help);
        summary.add(summaryBody, BorderLayout.NORTH);

        outer.add(mapCard, BorderLayout.CENTER);
        outer.add(summary, BorderLayout.EAST);
        showCard("seats", outer);
    }

    private static JComponent buildCinemaScreen() {
        JComponent screen = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(460, 56); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                float left = Math.max(36f, w * 0.14f);
                float right = w - left;
                float mid = w / 2f;
                float edgeY = 24f;
                float ctrlY = 2f;

                Path2D glowBand = new Path2D.Float();
                glowBand.moveTo(left, edgeY);
                glowBand.quadTo(mid, ctrlY, right, edgeY);
                glowBand.lineTo(right - 14f, edgeY + 22f);
                glowBand.quadTo(mid, ctrlY + 22f, left + 14f, edgeY + 22f);
                glowBand.closePath();
                g2.setPaint(new GradientPaint(0, ctrlY + 6f,
                        new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 52),
                        0, edgeY + 22f,
                        new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 0)));
                g2.fill(glowBand);

                QuadCurve2D arc = new QuadCurve2D.Float(left, edgeY, mid, ctrlY, right, edgeY);
                g2.setPaint(null);
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 56));
                g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(arc);

                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(arc);

                g2.setFont(trackedFont(Theme.bold(11), 0.18f));
                g2.setColor(ACCENT);
                String text = "MÀN HÌNH";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (w - fm.stringWidth(text)) / 2, 46);
                g2.dispose();
            }
        };
        return screen;
    }

    private JToggleButton buildSeatTile(
            ChonGheKhach.GheSanh seat,
            Set<Integer> selectedIds,
            Runnable refreshSummary) {
        boolean vip = seat.tenLoaiGhe() != null && seat.tenLoaiGhe().toLowerCase(Locale.ROOT).contains("vip");
        JToggleButton btn = new JToggleButton(seat.viTri()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                boolean enabled = isEnabled();
                boolean chosen = isSelected();
                boolean hover = enabled && getModel().isRollover();

                Color fill = !enabled ? SEAT_UNAVAIL_FILL
                        : chosen ? ACCENT
                        : hover ? mix(SEAT_AVAIL_FILL, ACCENT, 0.18) : SEAT_AVAIL_FILL;
                Color stroke = !enabled ? SEAT_UNAVAIL_BORDER
                        : chosen ? ACCENT
                        : hover ? ACCENT : (vip ? mix(SEAT_AVAIL_BORDER, ACCENT, 0.45) : SEAT_AVAIL_BORDER);
                Color fg = !enabled ? SEAT_UNAVAIL_TEXT : (chosen ? ACCENT_INK : TEXT);

                int backX = 4, backY = 2, backW = w - 8, backH = h - 10;
                g2.setColor(fill);
                g2.fillRoundRect(backX, backY, backW, backH, 10, 10);

                if (enabled) {
                    Shape prev = g2.getClip();
                    g2.clip(new RoundRectangle2D.Float(backX, backY, backW, backH, 10, 10));
                    g2.setColor(new Color(255, 255, 255, chosen ? 65 : 20));
                    g2.fillRect(backX, backY + 1, backW, 1);
                    g2.setClip(prev);
                }

                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(chosen ? 1.8f : 1f));
                g2.drawRoundRect(backX, backY, backW, backH, 10, 10);

                // Side armrests & bottom seat cushion bar for unmistakable cinema-seat silhouette
                Color trim = !enabled ? SEAT_UNAVAIL_BORDER : chosen ? mix(ACCENT, ACCENT_INK, 0.25) : stroke;
                g2.setColor(trim);
                g2.fillRoundRect(1, 10, 3, h - 18, 2, 2);
                g2.fillRoundRect(w - 4, 10, 3, h - 18, 2, 2);
                g2.fillRoundRect(9, h - 6, w - 18, 4, 4, 4);

                if (!enabled) {
                    g2.setColor(new Color(SEAT_UNAVAIL_TEXT.getRed(), SEAT_UNAVAIL_TEXT.getGreen(), SEAT_UNAVAIL_TEXT.getBlue(), 130));
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(10, h - 14, w - 10, 7);
                }

                g2.setFont(Theme.bold(12));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (w - fm.stringWidth(t)) / 2, backY + (backH + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.putClientProperty("cinema.noIcon", true);
        btn.putClientProperty("seatCode", seat.viTri());
        btn.setPreferredSize(new Dimension(62, 46));
        btn.setEnabled(seat.khaDung());
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setRolloverEnabled(true);
        btn.setCursor(seat.khaDung() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        String priceInfo = seat.giaTamTinh() == null ? "" : " · " + formatMoney(seat.giaTamTinh());
        btn.setToolTipText("Ghế " + seat.viTri() + " · " + seat.tenLoaiGhe()
                + (seat.khaDung() ? priceInfo : " · Không khả dụng"));
        btn.getAccessibleContext().setAccessibleName(btn.getToolTipText());
        btn.addActionListener(e -> {
            if (btn.isSelected()) selectedIds.add(seat.maGhe());
            else selectedIds.remove(seat.maGhe());
            btn.repaint();
            refreshSummary.run();
        });
        return btn;
    }

    public void openSelection(String reference) {
        run(() -> service.traCuuMa(reference),
                opt -> renderSelectionView(reference, opt.orElse(null)));
    }

    private void renderSelectionView(String rawCode, ChonGheKhach selection) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(28, 36, 36, 36));

        JButton back = cinemaButton("← Phim tại rạp", false);
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(e -> loadHome());
        content.add(back);
        content.add(Box.createVerticalStrut(20));

        if (selection == null) {
            JLabel title = sectionTitle("Mã đặt vé");
            content.add(title);
            content.add(Box.createVerticalStrut(12));
            content.add(emptyNotice("Mã đặt vé không tồn tại hoặc đã hết hạn: " + rawCode.strip().toUpperCase(Locale.ROOT)));
            showCard("selection", wrapScroll(content));
            return;
        }

        String headingText = selection.daThanhToan() ? "Đã thanh toán tại quầy"
                : selection.daTiepNhan() ? "Quầy đã nhận mã đặt vé"
                : selection.lichThayDoi() ? "Lịch chiếu đã thay đổi"
                : "Mã đặt vé";
        JLabel heading = new JLabel(headingText);
        heading.setForeground(TEXT);
        heading.setFont(trackedFont(Theme.bold(30), -0.02f));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel(selection.tenPhim() + "   ·   "
                + selection.thoiGianBatDau().format(SHOW_FMT) + "   ·   " + selection.tenPhong());
        sub.setForeground(MUTED);
        sub.setFont(Theme.font(14));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading);
        content.add(Box.createVerticalStrut(6));
        content.add(sub);
        content.add(Box.createVerticalStrut(22));

        final int stubWidth = 280;
        JPanel ticket = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), arc = 16;
                Shape cardShape = new RoundRectangle2D.Float(0, 0, w - 1f, h - 2f, arc, arc);
                g2.setColor(new Color(0, 0, 0, 75));
                g2.fillRoundRect(0, 2, w - 1, h - 2, arc, arc);
                g2.setClip(cardShape);
                g2.setColor(SURFACE);
                g2.fillRect(0, 0, w, h);
                g2.setColor(selection.lichThayDoi() ? DANGER : ACCENT);
                g2.fillRect(0, 0, w, 5);
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillRect(0, 5, w, 1);

                int divX = Math.max(320, w - stubWidth);
                g2.setColor(LINE);
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 5f}, 0f));
                g2.drawLine(divX, 14, divX, h - 14);

                g2.setClip(null);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 110));
                g2.draw(cardShape);

                // Semicircular ticket-stub notches at perforation line
                int notchR = 9;
                g2.setColor(BG);
                g2.fillOval(divX - notchR, -notchR, notchR * 2, notchR * 2);
                g2.fillOval(divX - notchR, h - notchR - 2, notchR * 2, notchR * 2);
                g2.dispose();
            }
        };
        ticket.setOpaque(false);
        ticket.setAlignmentX(Component.LEFT_ALIGNMENT);
        ticket.setMaximumSize(new Dimension(880, 340));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(26, 28, 24, 28));

        String badgeText = selection.daThanhToan() ? "ĐÃ THANH TOÁN"
                : selection.daTiepNhan() ? "ĐÃ TIẾP NHẬN"
                : selection.lichThayDoi() ? "LỊCH THAY ĐỔI" : "CHỜ XUẤT TRÌNH";
        Color badgeColor = selection.daThanhToan() ? SUCCESS
                : selection.lichThayDoi() ? DANGER : ACCENT;
        JLabel statusPill = pillBadge(badgeText, badgeColor,
                new Color(badgeColor.getRed(), badgeColor.getGreen(), badgeColor.getBlue(), 26),
                new Color(badgeColor.getRed(), badgeColor.getGreen(), badgeColor.getBlue(), 110));

        JLabel refTitle = new JLabel(selection.daThanhToan() ? "Giao dịch đã hoàn tất" : "Xuất trình mã tại quầy");
        refTitle.setForeground(ACCENT);
        refTitle.setFont(Theme.bold(15));
        String guide = selection.daThanhToan()
                ? "Giao dịch đã hoàn tất thanh toán. Vui lòng nhận vé tại quầy."
                : selection.daTiepNhan()
                ? "Nhân viên đã tiếp nhận mã. Vui lòng hoàn tất giao dịch tại quầy."
                : selection.lichThayDoi()
                ? "Lịch chiếu của suất này đã thay đổi hoặc tạm ngừng. Vui lòng chọn suất chiếu khác."
                : "Ghế có thể thay đổi trước khi nhân viên xác nhận tại quầy. Thanh toán và nhận vé tại quầy.";
        JLabel guideLbl = new JLabel("<html>" + guide + "</html>");
        guideLbl.setForeground(selection.lichThayDoi() ? DANGER : MUTED);
        guideLbl.setFont(Theme.font(13));

        JPanel codeBox = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(BG);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setColor(LINE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.dispose();
            }
        };
        codeBox.setOpaque(false);
        codeBox.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel codeDisplay = new JLabel(selection.maChonGhe());
        codeDisplay.setForeground(TEXT);
        codeDisplay.setFont(trackedFont(Theme.number(22), 0.08f));
        codeBox.add(codeDisplay, BorderLayout.CENTER);
        codeBox.setMaximumSize(new Dimension(520, 56));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        JButton copyBtn = cinemaButton("Sao chép mã", true);
        copyBtn.setIcon(new Icons("copy", 16));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(selection.maChonGhe()), null);
            statusBanner.setForeground(ACCENT);
            statusBanner.setText("Đã sao chép mã đặt vé: " + selection.maChonGhe());
        });
        JButton refreshBtn = cinemaButton("Làm mới trạng thái", false);
        refreshBtn.addActionListener(e -> openSelection(selection.maChonGhe()));
        actions.add(copyBtn);
        actions.add(refreshBtn);

        for (JComponent c : new JComponent[]{statusPill, refTitle, guideLbl, codeBox, actions}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        left.add(statusPill);
        left.add(Box.createVerticalStrut(10));
        left.add(refTitle);
        left.add(Box.createVerticalStrut(6));
        left.add(guideLbl);
        left.add(Box.createVerticalStrut(16));
        left.add(codeBox);
        left.add(Box.createVerticalStrut(16));
        left.add(actions);

        JPanel details = new JPanel(new GridLayout(0, 1, 0, 12));
        details.setOpaque(false);
        details.setPreferredSize(new Dimension(stubWidth, 0));
        details.setBorder(new EmptyBorder(26, 28, 24, 24));
        String seatText = String.join(", ", selection.ghe().stream().map(ChonGheKhach.GheChon::viTri).toList());
        details.add(detailBlock("Ghế đã chọn", seatText, TEXT));
        details.add(detailBlock("Tạm tính", formatMoney(selection.tongTamTinh()), ACCENT));
        if (!selection.daTiepNhan() && !selection.lichThayDoi()) {
            details.add(detailBlock("Mã có hiệu lực đến", selection.thoiGianHetHan().format(EXPIRY_FMT), TEXT));
        }

        ticket.add(left, BorderLayout.CENTER);
        ticket.add(details, BorderLayout.EAST);
        content.add(ticket);

        showCard("selection", wrapScroll(content));
    }

    private static JPanel detailBlock(String label, String value) {
        return detailBlock(label, value, TEXT);
    }

    private static JPanel detailBlock(String label, String value, Color valueColor) {
        JPanel box = new JPanel(new GridLayout(2, 1, 0, 3));
        box.setOpaque(false);
        JLabel dt = new JLabel(label);
        dt.setForeground(MUTED);
        dt.setFont(Theme.font(12));
        JLabel dd = new JLabel(value);
        dd.setForeground(valueColor);
        dd.setFont(Theme.bold(16));
        box.add(dt);
        box.add(dd);
        return box;
    }

    private JComponent posterBox(BufferedImage image, String badgeText, int height, boolean topOnlyRound) {
        JPanel box = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth(), h = getHeight();
                Shape clip;
                if (topOnlyRound) {
                    Path2D path = new Path2D.Float();
                    float r = 14f;
                    path.moveTo(0, h);
                    path.lineTo(0, r);
                    path.quadTo(0, 0, r, 0);
                    path.lineTo(w - r, 0);
                    path.quadTo(w, 0, w, r);
                    path.lineTo(w, h);
                    path.closePath();
                    clip = path;
                } else {
                    clip = new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, 12, 12);
                }
                g2.setClip(clip);
                g2.setColor(RAISED);
                g2.fillRect(0, 0, w, h);
                if (image != null) {
                    double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
                    int iw = (int) (image.getWidth() * scale);
                    int ih = (int) (image.getHeight() * scale);
                    g2.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
                    if (topOnlyRound) {
                        g2.setPaint(new GradientPaint(0, h - 56,
                                new Color(SURFACE.getRed(), SURFACE.getGreen(), SURFACE.getBlue(), 0),
                                0, h, SURFACE));
                        g2.fillRect(0, h - 56, w, 56);
                    }
                } else {
                    g2.setColor(MUTED);
                    g2.setFont(trackedFont(Theme.bold(12), 0.08f));
                    String fallback = "CINEMA POSTER";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(fallback, (w - fm.stringWidth(fallback)) / 2, h / 2);
                }
                if (badgeText != null && !badgeText.isBlank()) {
                    g2.setFont(Theme.bold(11));
                    FontMetrics fm = g2.getFontMetrics();
                    int bw = fm.stringWidth(badgeText) + 18;
                    int bh = 24;
                    int bx = w - bw - 12, by = 12;
                    g2.setColor(new Color(17, 16, 15, 224));
                    g2.fillRoundRect(bx, by, bw, bh, bh, bh);
                    g2.setColor(LINE);
                    g2.drawRoundRect(bx, by, bw, bh, bh, bh);
                    g2.setColor(TEXT);
                    g2.drawString(badgeText, bx + 9, by + 16);
                }
                g2.setClip(null);
                if (!topOnlyRound) {
                    g2.setColor(LINE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                }
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setPreferredSize(new Dimension(180, height));
        return box;
    }

    private BufferedImage loadPoster(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (posterCache.containsKey(stored)) return posterCache.get(stored);
        BufferedImage img = null;
        try { img = posters.load(stored); } catch (RuntimeException ignored) {}
        posterCache.put(stored, img);
        return img;
    }

    private static JPanel legendItem(Color fill, Color stroke, boolean unavailable, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        item.setOpaque(false);
        JComponent swatch = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(20, 18); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(1, 1, 18, 13, 5, 5);
                g2.setColor(stroke);
                g2.drawRoundRect(1, 1, 18, 13, 5, 5);
                g2.fillRoundRect(4, 15, 12, 2, 2, 2);
                if (unavailable) {
                    g2.setColor(SEAT_UNAVAIL_TEXT);
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(4, 12, 16, 3);
                }
                g2.dispose();
            }
        };
        JLabel lbl = new JLabel(text);
        lbl.setForeground(MUTED);
        lbl.setFont(Theme.font(12));
        item.add(swatch);
        item.add(lbl);
        return item;
    }

    private static JLabel pillBadge(String text, Color fg, Color bg, Color border) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setOpaque(false);
        lbl.setForeground(fg);
        lbl.setFont(trackedFont(Theme.bold(11), 0.04f));
        lbl.setBorder(new EmptyBorder(4, 12, 4, 12));
        return lbl;
    }

    private static JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT);
        lbl.setFont(trackedFont(Theme.bold(22), -0.01f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static JPanel emptyNotice(String text) {
        JPanel box = cinemaCard(new BorderLayout());
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        box.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel lbl = new JLabel(text);
        lbl.setForeground(MUTED);
        lbl.setFont(Theme.font(14));
        box.add(lbl, BorderLayout.CENTER);
        return box;
    }

    private static JPanel cinemaCard(LayoutManager layout) {
        JPanel card = new JPanel(layout) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), arc = 14;
                g2.setColor(new Color(0, 0, 0, 65));
                g2.fillRoundRect(0, 2, w - 1, h - 2, arc, arc);
                Shape shape = new RoundRectangle2D.Float(0, 0, w - 1f, h - 2f, arc, arc);
                g2.setColor(SURFACE);
                g2.fill(shape);
                Shape prev = g2.getClip();
                g2.clip(shape);
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillRect(0, 1, w, 1);
                g2.setClip(prev);
                g2.setColor(LINE);
                g2.draw(shape);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.putClientProperty("cinema.customSurface", SURFACE);
        return card;
    }

    private static JButton cinemaButton(String text, boolean primary) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), arc = 10;
                boolean isPrimary = Boolean.TRUE.equals(getClientProperty("cinema.customPrimary"));
                boolean enabled = isEnabled();
                boolean hover = enabled && getModel().isRollover();
                boolean pressed = enabled && getModel().isPressed();

                Color base = !enabled ? Color.decode("#1E1C1A") : isPrimary ? ACCENT : RAISED;
                if (hover) base = mix(base, isPrimary ? Color.WHITE : ACCENT, 0.14);
                if (pressed) base = mix(base, isPrimary ? ACCENT_INK : ACCENT, 0.24);
                Color border = !enabled ? Color.decode("#2E2A26")
                        : isPrimary ? ACCENT : (hover ? ACCENT : LINE);
                Color fg = !enabled ? Color.decode("#6F675E") : isPrimary ? ACCENT_INK : TEXT;

                Shape shape = new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, arc, arc);
                g2.setColor(base);
                g2.fill(shape);
                if (enabled) {
                    Shape prev = g2.getClip();
                    g2.clip(shape);
                    g2.setColor(new Color(255, 255, 255, isPrimary ? 48 : 16));
                    g2.fillRect(0, 1, w, 1);
                    g2.setClip(prev);
                }
                g2.setColor(border);
                g2.draw(shape);
                g2.dispose();
                setForeground(fg);
                super.paintComponent(g);
            }
        };
        btn.setUI(new BasicButtonUI());
        btn.putClientProperty("cinema.noIcon", true);
        btn.putClientProperty("cinema.customPrimary", primary);
        btn.setFont(Theme.bold(13));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setRolloverEnabled(true);
        btn.setIconTextGap(8);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleCinemaButton(btn);
        return btn;
    }

    private static void styleCinemaButton(AbstractButton btn) {
        Object flag = btn.getClientProperty("cinema.customPrimary");
        if (flag == null) return;
        boolean primary = Boolean.TRUE.equals(flag);
        btn.setForeground(primary ? ACCENT_INK : TEXT);
        btn.setBorder(new EmptyBorder(9, 16, 9, 16));
    }

    private static void styleField(JTextField field) {
        if (!Boolean.TRUE.equals(field.getClientProperty("cinema.customField"))) {
            field.setUI(new BasicTextFieldUI() {
                @Override protected void paintBackground(Graphics g) {}
            });
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { field.repaint(); }
                @Override public void focusLost(FocusEvent e) { field.repaint(); }
            });
        }
        field.setOpaque(false);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 72));
        field.setSelectedTextColor(TEXT);
        field.setFont(Theme.font(13));
        field.setBorder(new EmptyBorder(8, 12, 8, 12));
        field.setPreferredSize(new Dimension(210, 36));
        field.putClientProperty("cinema.customField", true);
    }

    private static JScrollPane wrapScroll(JComponent content) {
        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.setBackground(BG);
        northWrap.putClientProperty("cinema.customSurface", BG);
        northWrap.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(northWrap);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    private void showCard(String name, JComponent view) {
        body.removeAll();
        body.add(view, name);
        cards.show(body, name);
        styleCinemaTree(body);
        body.revalidate();
        body.repaint();
    }

    private static void styleCinemaTree(Component root) {
        if (root instanceof JComponent jc) {
            if (jc.getClientProperty("cinema.customSurface") instanceof Color c && jc.isOpaque()) {
                jc.setBackground(c);
            }
            if (jc instanceof AbstractButton b && jc.getClientProperty("cinema.customPrimary") != null) {
                styleCinemaButton(b);
            }
            if (jc instanceof JTextField f && Boolean.TRUE.equals(jc.getClientProperty("cinema.customField"))) {
                styleField(f);
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) styleCinemaTree(child);
        }
    }

    private static Font trackedFont(Font base, float tracking) {
        return base.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }

    private static Color mix(Color a, Color b, double amount) {
        return new Color(
                (int) Math.round(a.getRed() * (1 - amount) + b.getRed() * amount),
                (int) Math.round(a.getGreen() * (1 - amount) + b.getGreen() * amount),
                (int) Math.round(a.getBlue() * (1 - amount) + b.getBlue() * amount));
    }

    private static String filmMeta(ChonGheKhach.PhimChieu item) {
        Phim p = item.phim();
        List<String> parts = new ArrayList<>();
        if (item.theLoai() != null && !item.theLoai().isBlank()) parts.add(item.theLoai());
        parts.add(p.thoiLuong() + " phút");
        parts.add(p.dinhDangChieu());
        parts.add(ageBadge(p.gioiHanTuoi()));
        return String.join(" · ", parts);
    }

    private static String ageBadge(int age) {
        return age > 0 ? "T" + age : "P";
    }

    private static String formatMoney(BigDecimal value) {
        if (value == null) return "—";
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(value) + " ₫";
    }

    private void showError(String msg) {
        statusBanner.setForeground(DANGER);
        statusBanner.setText(msg);
    }

    private <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        run(work, onSuccess, err -> showError(err.getMessage() == null ? "Không thể tải dữ liệu rạp chiếu." : err.getMessage()));
    }

    private <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        if (busy) return;
        busy = true;
        statusBanner.setForeground(MUTED);
        statusBanner.setText("Đang tải dữ liệu...");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override protected void done() {
                busy = false;
                try {
                    T result = get();
                    statusBanner.setForeground(MUTED);
                    statusBanner.setText("Sẵn sàng.");
                    onSuccess.accept(result);
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    if (cause instanceof IllegalArgumentException) {
                        onError.accept(cause);
                    } else {
                        showError("Không thể kết nối CSDL MySQL. Vui lòng kiểm tra config/database.properties.");
                    }
                }
            }
        }.execute();
    }
}
