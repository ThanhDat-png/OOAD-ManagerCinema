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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

public final class KhachHangWindow extends JFrame {
    static { Theme.install(); }

    private static final Color BG = Color.decode("#111317");
    private static final Color SURFACE = Color.decode("#191D22");
    private static final Color RAISED = Color.decode("#22272E");
    private static final Color LINE = Color.decode("#3C4249");
    private static final Color TEXT = Color.decode("#FAF2E7");
    private static final Color MUTED = Color.decode("#BCB9B5");
    private static final Color ACCENT = Color.decode("#FFB07B");
    private static final Color ACCENT_INK = Color.decode("#201309");
    private static final Color DANGER = Color.decode("#FFB3AA");

    private static final DateTimeFormatter SHOW_FMT = DateTimeFormatter.ofPattern("dd/MM · HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_PILL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter EXPIRY_FMT = DateTimeFormatter.ofPattern("HH:mm · dd/MM/uuuu");

    private final KhachHangBUS service;
    private final PosterStore posters = new PosterStore();
    private final Map<String, BufferedImage> posterCache = new HashMap<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JLabel statusBanner = new JLabel(" ");
    private final JTextField lookupInput = new JTextField(20);
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
        statusBanner.setBorder(new EmptyBorder(8, 28, 10, 28));
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
            @Override public Dimension getPreferredSize() { return new Dimension(34, 34); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(2, 2, 29, 29);
                Polygon play = new Polygon(new int[]{14, 14, 23}, new int[]{11, 23, 17}, 3);
                g2.fillPolygon(play);
                g2.dispose();
            }
        };
        JPanel word = new JPanel(new GridLayout(2, 1, 0, 1));
        word.setOpaque(false);
        JLabel brand = new JLabel("CINEMA");
        brand.setFont(Theme.bold(18));
        brand.setForeground(TEXT);
        JLabel sub = new JLabel("PHIM TẠI RẠP");
        sub.setFont(Theme.bold(10));
        sub.setForeground(ACCENT);
        word.add(brand);
        word.add(sub);
        left.add(mark);
        left.add(word);

        JButton homeBtn = cinemaButton("Phim tại rạp", false);
        homeBtn.addActionListener(e -> loadHome());
        left.add(Box.createHorizontalStrut(12));
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
                int w = getWidth(), h = getHeight();
                Shape clip = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 18, 18);
                g2.setClip(clip);
                g2.setColor(Color.decode("#171A20"));
                g2.fillRect(0, 0, w, h);
                if (art != null) {
                    int drawW = (int) (w * 0.58);
                    int drawX = w - drawW;
                    double scale = Math.max((double) drawW / art.getWidth(), (double) h / art.getHeight());
                    int iw = (int) (art.getWidth() * scale);
                    int ih = (int) (art.getHeight() * scale);
                    g2.drawImage(art, drawX + (drawW - iw) / 2, (h - ih) / 2, iw, ih, null);
                    g2.setPaint(new GradientPaint(drawX, 0, Color.decode("#171A20"),
                            drawX + (int) (drawW * 0.65), 0, new Color(23, 26, 32, 35)));
                    g2.fillRect(drawX, 0, drawW, h);
                }
                g2.setClip(null);
                g2.setColor(LINE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
                g2.dispose();
            }
        };
        marquee.setOpaque(false);
        marquee.setAlignmentX(Component.LEFT_ALIGNMENT);
        marquee.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        marquee.setPreferredSize(new Dimension(960, 240));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel eyebrow = new JLabel("Sắp chiếu".equals(p.trangThai()) ? "PHIM SẮP CHIẾU" : "PHIM NỔI BẬT");
        eyebrow.setForeground(ACCENT);
        eyebrow.setFont(Theme.bold(12));
        JLabel title = new JLabel(p.tenPhim());
        title.setForeground(TEXT);
        title.setFont(Theme.bold(32));
        JLabel meta = new JLabel("<html>" + filmMeta(item) + "</html>");
        meta.setForeground(MUTED);
        meta.setFont(Theme.font(14));
        JButton cta = cinemaButton("Xem giờ chiếu →", true);
        cta.addActionListener(e -> openFilm(p.maPhim(), null));

        for (JComponent c : new JComponent[]{eyebrow, title, meta, cta}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        inner.add(eyebrow);
        inner.add(Box.createVerticalStrut(8));
        inner.add(title);
        inner.add(Box.createVerticalStrut(10));
        inner.add(meta);
        inner.add(Box.createVerticalStrut(20));
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
        JPanel card = cinemaCard(new BorderLayout(0, 12));
        card.add(posterBox(loadPoster(p.poster()), p.dinhDangChieu() + " · " + ageBadge(p.gioiHanTuoi()), 220),
                BorderLayout.NORTH);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(4, 16, 16, 16));

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
        info.add(Box.createVerticalStrut(14));
        info.add(action);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private record FilmScreenData(ChonGheKhach.PhimChieu film, List<ChonGheKhach.SuatKhach> shows) { }

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
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(20, 20, 20, 20)));
        JComponent poster = posterBox(loadPoster(p.poster()), null, 210);
        poster.setPreferredSize(new Dimension(145, 210));
        hero.add(poster, BorderLayout.WEST);

        JPanel metaBox = new JPanel();
        metaBox.setOpaque(false);
        metaBox.setLayout(new BoxLayout(metaBox, BoxLayout.Y_AXIS));
        JLabel status = new JLabel(p.trangThai().toUpperCase(Locale.ROOT));
        status.setForeground(ACCENT);
        status.setFont(Theme.bold(12));
        JLabel title = new JLabel(p.tenPhim());
        title.setForeground(TEXT);
        title.setFont(Theme.bold(28));
        JLabel detail = new JLabel(filmMeta(item) + " · Đạo diễn: " + p.daoDien());
        detail.setForeground(MUTED);
        detail.setFont(Theme.font(14));
        metaBox.add(status);
        metaBox.add(Box.createVerticalStrut(8));
        metaBox.add(title);
        metaBox.add(Box.createVerticalStrut(10));
        metaBox.add(detail);
        hero.add(metaBox, BorderLayout.CENTER);
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

    private record SeatScreenData(ChonGheKhach.SuatKhach show, List<ChonGheKhach.GheSanh> seats) { }

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
        title.setFont(Theme.bold(26));
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
            JLabel err = new JLabel(errorText);
            err.setForeground(DANGER);
            err.setFont(Theme.bold(13));
            err.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(err);
        }
        outer.add(header, BorderLayout.NORTH);

        JLabel chosenSeatsLabel = new JLabel("Chưa chọn ghế");
        chosenSeatsLabel.setForeground(TEXT);
        chosenSeatsLabel.setFont(Theme.bold(15));
        JLabel totalLabel = new JLabel("—");
        totalLabel.setForeground(ACCENT);
        totalLabel.setFont(Theme.bold(22));
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
        mapCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(18, 20, 18, 20)));

        JLabel screenLbl = new JLabel("MÀN HÌNH", SwingConstants.CENTER);
        screenLbl.setForeground(ACCENT);
        screenLbl.setFont(Theme.bold(12));
        screenLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, ACCENT),
                new EmptyBorder(8, 0, 12, 0)));
        mapCard.add(screenLbl, BorderLayout.NORTH);

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
                rowTag.setPreferredSize(new Dimension(28, 38));
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

        JScrollPane mapScroll = new JScrollPane(rowsPanel);
        mapScroll.setBorder(BorderFactory.createEmptyBorder());
        mapScroll.getViewport().setBackground(SURFACE);
        mapCard.add(mapScroll, BorderLayout.CENTER);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        legend.setOpaque(false);
        legend.add(legendItem(RAISED, LINE, "Còn trống"));
        legend.add(legendItem(ACCENT, ACCENT, "Đang chọn"));
        legend.add(legendItem(Color.decode("#15181C"), Color.decode("#2A2F36"), "Không khả dụng"));
        mapCard.add(legend, BorderLayout.SOUTH);

        JPanel summary = cinemaCard(new BorderLayout(0, 16));
        summary.setPreferredSize(new Dimension(310, 0));
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(20, 20, 20, 20)));

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
        summaryBody.add(Box.createVerticalStrut(20));
        summaryBody.add(submitBtn);
        summaryBody.add(Box.createVerticalStrut(12));
        summaryBody.add(help);
        summary.add(summaryBody, BorderLayout.NORTH);

        outer.add(mapCard, BorderLayout.CENTER);
        outer.add(summary, BorderLayout.EAST);
        showCard("seats", outer);
    }

    private JToggleButton buildSeatTile(
            ChonGheKhach.GheSanh seat,
            Set<Integer> selectedIds,
            Runnable refreshSummary) {
        JToggleButton btn = new JToggleButton(seat.viTri()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Color fill = !isEnabled() ? Color.decode("#15181C") : (isSelected() ? ACCENT : RAISED);
                Color stroke = !isEnabled() ? Color.decode("#2A2F36") : (isSelected() ? ACCENT : LINE);
                Color fg = !isEnabled() ? Color.decode("#595E66") : (isSelected() ? ACCENT_INK : TEXT);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setColor(stroke);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setFont(Theme.bold(13));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (w - fm.stringWidth(t)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.putClientProperty("cinema.noIcon", true);
        btn.putClientProperty("seatCode", seat.viTri());
        btn.setPreferredSize(new Dimension(58, 40));
        btn.setEnabled(seat.khaDung());
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
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
        heading.setFont(Theme.bold(30));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel(selection.tenPhim() + "   ·   "
                + selection.thoiGianBatDau().format(SHOW_FMT) + "   ·   " + selection.tenPhong());
        sub.setForeground(MUTED);
        sub.setFont(Theme.font(14));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading);
        content.add(Box.createVerticalStrut(6));
        content.add(sub);
        content.add(Box.createVerticalStrut(20));

        JPanel ticket = cinemaCard(new BorderLayout(24, 16));
        ticket.setAlignmentX(Component.LEFT_ALIGNMENT);
        ticket.setMaximumSize(new Dimension(860, 320));
        ticket.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1), new EmptyBorder(24, 28, 24, 28)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
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

        JLabel codeDisplay = new JLabel(selection.maChonGhe());
        codeDisplay.setForeground(TEXT);
        codeDisplay.setFont(Theme.number(24));
        codeDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(10, 14, 10, 14)));

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

        for (JComponent c : new JComponent[]{refTitle, guideLbl, codeDisplay, actions}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        left.add(refTitle);
        left.add(Box.createVerticalStrut(6));
        left.add(guideLbl);
        left.add(Box.createVerticalStrut(16));
        left.add(codeDisplay);
        left.add(Box.createVerticalStrut(14));
        left.add(actions);

        JPanel details = new JPanel(new GridLayout(0, 1, 0, 10));
        details.setOpaque(false);
        details.setPreferredSize(new Dimension(250, 0));
        String seatText = String.join(", ", selection.ghe().stream().map(ChonGheKhach.GheChon::viTri).toList());
        details.add(detailBlock("Ghế đã chọn", seatText));
        details.add(detailBlock("Tạm tính", formatMoney(selection.tongTamTinh())));
        if (!selection.daTiepNhan() && !selection.lichThayDoi()) {
            details.add(detailBlock("Mã có hiệu lực đến", selection.thoiGianHetHan().format(EXPIRY_FMT)));
        }

        ticket.add(left, BorderLayout.CENTER);
        ticket.add(details, BorderLayout.EAST);
        content.add(ticket);

        showCard("selection", wrapScroll(content));
    }

    private static JPanel detailBlock(String label, String value) {
        JPanel box = new JPanel(new GridLayout(2, 1, 0, 2));
        box.setOpaque(false);
        JLabel dt = new JLabel(label);
        dt.setForeground(MUTED);
        dt.setFont(Theme.font(12));
        JLabel dd = new JLabel(value);
        dd.setForeground(TEXT);
        dd.setFont(Theme.bold(16));
        box.add(dt);
        box.add(dd);
        return box;
    }

    private JComponent posterBox(BufferedImage image, String badgeText, int height) {
        JPanel box = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth(), h = getHeight();
                g2.setColor(RAISED);
                g2.fillRect(0, 0, w, h);
                if (image != null) {
                    double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
                    int iw = (int) (image.getWidth() * scale);
                    int ih = (int) (image.getHeight() * scale);
                    g2.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
                } else {
                    g2.setColor(MUTED);
                    g2.setFont(Theme.bold(13));
                    String fallback = "CINEMA POSTER";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(fallback, (w - fm.stringWidth(fallback)) / 2, h / 2);
                }
                if (badgeText != null && !badgeText.isBlank()) {
                    g2.setFont(Theme.bold(11));
                    FontMetrics fm = g2.getFontMetrics();
                    int bw = fm.stringWidth(badgeText) + 14;
                    int bh = 24;
                    int bx = w - bw - 10, by = 10;
                    g2.setColor(new Color(17, 19, 23, 225));
                    g2.fillRoundRect(bx, by, bw, bh, 6, 6);
                    g2.setColor(LINE);
                    g2.drawRoundRect(bx, by, bw, bh, 6, 6);
                    g2.setColor(TEXT);
                    g2.drawString(badgeText, bx + 7, by + 16);
                }
                g2.dispose();
            }
        };
        box.setPreferredSize(new Dimension(180, height));
        return box;
    }

    private BufferedImage loadPoster(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (posterCache.containsKey(stored)) return posterCache.get(stored);
        BufferedImage img = null;
        try { img = posters.load(stored); } catch (RuntimeException ignored) { }
        posterCache.put(stored, img);
        return img;
    }

    private static JPanel legendItem(Color fill, Color stroke, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);
        JComponent swatch = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(16, 16); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, 15, 15, 4, 4);
                g2.setColor(stroke);
                g2.drawRoundRect(0, 0, 15, 15, 4, 4);
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

    private static JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT);
        lbl.setFont(Theme.bold(22));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static JPanel emptyNotice(String text) {
        JPanel box = cinemaCard(new BorderLayout());
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(16, 18, 16, 18)));
        JLabel lbl = new JLabel(text);
        lbl.setForeground(MUTED);
        lbl.setFont(Theme.font(14));
        box.add(lbl, BorderLayout.CENTER);
        return box;
    }

    private static JPanel cinemaCard(LayoutManager layout) {
        JPanel card = new JPanel(layout);
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createLineBorder(LINE));
        card.putClientProperty("cinema.customSurface", SURFACE);
        return card;
    }

    private static JButton cinemaButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.putClientProperty("cinema.noIcon", true);
        btn.putClientProperty("cinema.customPrimary", primary);
        btn.setFont(Theme.bold(13));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleCinemaButton(btn);
        return btn;
    }

    private static void styleCinemaButton(AbstractButton btn) {
        Object flag = btn.getClientProperty("cinema.customPrimary");
        if (flag == null) return;
        boolean primary = Boolean.TRUE.equals(flag);
        btn.setBackground(primary ? ACCENT : RAISED);
        btn.setForeground(primary ? ACCENT_INK : TEXT);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT : LINE),
                new EmptyBorder(9, 16, 9, 16)));
    }

    private static void styleField(JTextField field) {
        field.setBackground(BG);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setFont(Theme.font(13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(7, 10, 7, 10)));
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
            if (jc.getClientProperty("cinema.customSurface") instanceof Color c) {
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
