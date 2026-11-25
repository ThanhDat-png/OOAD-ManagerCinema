package cinema.view;

import cinema.controller.auth.DangNhapBUS;
import cinema.controller.auth.PhienDangNhap;
import cinema.view.ui.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

public final class DangNhapWindow extends JFrame {
    static { Theme.install(); }
    private static final BufferedImage AUDITORIUM = loadAuditorium();

    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JButton submit = new JButton("Đăng nhập", new Icons("login", 18));
    private final JLabel message = new Design.WrappingLabel("Nhập tài khoản đã được cấp để đăng nhập.");

    public DangNhapWindow(DangNhapBUS service) {
        super("Đăng nhập — Quản lý rạp chiếu phim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel stage = new JPanel(new BorderLayout());
        stage.putClientProperty("cinema.background", "canvas");

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.putClientProperty("cinema.background", "canvas");
        topHeader.setBorder(BorderFactory.createCompoundBorder(
                new AbstractBorder() {
                    @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, 1, 0); }
                    @Override public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, 1, 0); return i; }
                    @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                        g.setColor(Tokens.border());
                        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
                    }
                },
                BorderFactory.createEmptyBorder(14, 36, 14, 36)));

        JLabel topBrand = Design.text("Cinema Workspace", "heading");
        topBrand.setIcon(Shell.brandGlyph());
        topBrand.setIconTextGap(10);
        topHeader.add(topBrand, BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.putClientProperty("cinema.background", "canvas");
        JToggleButton themeToggle = new JToggleButton(Tokens.dark ? "Giao diện sáng" : "Giao diện tối",
                new Icons(Tokens.dark ? "sun" : "moon"));
        themeToggle.putClientProperty("cinema.themeToggle", true);
        themeToggle.putClientProperty("cinema.noIcon", true);
        themeToggle.addActionListener(e -> {
            themeToggle.setSelected(false);
            Theme.toggle();
        });
        tools.add(themeToggle);
        topHeader.add(tools, BorderLayout.EAST);
        stage.add(topHeader, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.putClientProperty("cinema.background", "canvas");

        Design.Card splitCard = new Design.Card(new GridLayout(1, 2, 0, 0));
        splitCard.setBorder(BorderFactory.createEmptyBorder(1, 1, 3, 2));
        splitCard.setPreferredSize(new Dimension(920, 560));
        splitCard.add(new AuditoriumHero());
        splitCard.add(buildFormPanel());

        GridBagConstraints rootC = new GridBagConstraints();
        rootC.weightx = 1;
        rootC.weighty = 1;
        rootC.insets = new Insets(10, 24, 16, 24);
        center.add(splitCard, rootC);
        stage.add(center, BorderLayout.CENTER);

        setContentPane(stage);
        Theme.apply(stage);
        SwingUtilities.invokeLater(username::requestFocusInWindow);
        getRootPane().setDefaultButton(submit);
        submit.addActionListener(event -> authenticate(service));

        setSize(1060, 760);
        setMinimumSize(new Dimension(760, 660));
        setLocationRelativeTo(null);
    }

    private JPanel buildFormPanel() {
        JPanel formSide = new JPanel(new BorderLayout(0, 6));
        formSide.putClientProperty("cinema.background", "none");
        formSide.setOpaque(false);
        formSide.setBorder(BorderFactory.createEmptyBorder(16, 36, 16, 36));

        JLabel badge = new JLabel("", new Icons("ticket", 20), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight(), r = Theme.s(10);
                    g.setColor(Tokens.tone(Tokens.accent()));
                    g.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                    g.setColor(Tokens.mix(Tokens.surface(), Tokens.accent(), Tokens.dark ? 0.42 : 0.28));
                    g.drawRoundRect(0, 0, w - 2, h - 2, r, r);
                } finally {
                    g.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        badge.putClientProperty("cinema.text", "accent");
        badge.setPreferredSize(new Dimension(Theme.s(40), Theme.s(40)));
        badge.setMaximumSize(new Dimension(Theme.s(40), Theme.s(40)));

        JPanel header = Design.stack(
                badge,
                Design.text("Đăng nhập", "title"),
                Design.text("Sử dụng tài khoản được cấp để vào không gian làm việc.", "subtitle"));
        header.putClientProperty("cinema.background", "none");
        header.setOpaque(false);
        formSide.add(header, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.putClientProperty("cinema.background", "none");
        fields.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 5, 0);

        JLabel userLabel = Design.text("Tên đăng nhập", "subtitle");
        userLabel.setLabelFor(username);
        c.gridy = 0;
        fields.add(userLabel, c);
        c.gridy = 1;
        fields.add(username, c);

        JLabel passwordLabel = Design.text("Mật khẩu", "subtitle");
        passwordLabel.setLabelFor(password);
        c.gridy = 2;
        fields.add(passwordLabel, c);

        char defaultEcho = password.getEchoChar();
        JToggleButton peek = new JToggleButton(new Icons("eye", 16));
        peek.putClientProperty("cinema.noIcon", true);
        peek.putClientProperty("cinema.customBorder", true);
        peek.putClientProperty("cinema.background", "none");
        peek.setOpaque(false);
        peek.setContentAreaFilled(false);
        peek.setBorderPainted(false);
        peek.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));
        peek.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        peek.setToolTipText("Hiện hoặc ẩn mật khẩu");
        peek.getAccessibleContext().setAccessibleName("Hiện mật khẩu");
        peek.setPreferredSize(new Dimension(Theme.s(38), Theme.s(40)));
        peek.addActionListener(e -> password.setEchoChar(peek.isSelected() ? (char) 0 : defaultEcho));

        password.putClientProperty("cinema.customBorder", true);
        password.putClientProperty("cinema.background", "none");
        password.setOpaque(false);
        password.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 4));
        password.setMinimumSize(new Dimension(80, Theme.s(40)));

        JPanel secretInput = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight(), r = Theme.s(10);
                    g.setColor(Tokens.raised());
                    g.fillRoundRect(1, 1, w - 2, h - 2, r, r);
                    boolean invalid = Boolean.TRUE.equals(password.getClientProperty("cinema.invalid"));
                    boolean focused = password.hasFocus() || peek.hasFocus();
                    Color idleBorder = Tokens.dark ? Tokens.border() : Tokens.mix(Tokens.border(), Tokens.text(), 0.16);
                    g.setColor(invalid ? Tokens.danger() : focused ? Tokens.accent() : idleBorder);
                    g.setStroke(new BasicStroke(focused ? 1.5f : 1f));
                    g.drawRoundRect(1, 1, w - 3, h - 3, r, r);
                } finally {
                    g.dispose();
                }
            }
        };
        secretInput.setOpaque(false);
        secretInput.putClientProperty("cinema.background", "none");
        secretInput.add(password, BorderLayout.CENTER);
        secretInput.add(peek, BorderLayout.EAST);
        java.awt.event.FocusAdapter borderRepaint = new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { secretInput.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { secretInput.repaint(); }
        };
        password.addFocusListener(borderRepaint);
        peek.addFocusListener(borderRepaint);
        password.addPropertyChangeListener("cinema.invalid", e -> secretInput.repaint());

        JPanel secret = new JPanel(new BorderLayout(0, 4));
        secret.setOpaque(false);
        secret.putClientProperty("cinema.background", "none");
        secret.add(secretInput, BorderLayout.CENTER);
        JLabel required = Design.text("Vui lòng nhập mật khẩu.", "error");
        required.setVisible(false);
        secret.add(required, BorderLayout.SOUTH);
        c.gridy = 3;
        fields.add(secret, c);

        Forms.bind(username, "Tên đăng nhập");
        Forms.password(password, required);
        formSide.add(fields, BorderLayout.CENTER);

        submit.putClientProperty("cinema.role", "primary");
        submit.setPreferredSize(new Dimension(360, Theme.s(44)));
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(44)));

        message.putClientProperty("cinema.alert", true);
        Design.status(message);

        JToggleButton customerLink = new JToggleButton("Xem phim và lịch chiếu →", new Icons("film", 16));
        customerLink.putClientProperty("cinema.noIcon", true);
        customerLink.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(40)));
        customerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        customerLink.addActionListener(e -> {
            customerLink.setSelected(false);
            KhachHangWindow.open();
        });

        JPanel footer = Design.stack(message, submit, customerLink);
        footer.putClientProperty("cinema.background", "none");
        footer.setOpaque(false);
        formSide.add(footer, BorderLayout.SOUTH);
        return formSide;
    }

    private void authenticate(DangNhapBUS service) {
        String name = username.getText();
        char[] secret = password.getPassword();
        submit.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);
        message.setText("Đang kiểm tra thông tin đăng nhập...");
        new SwingWorker<PhienDangNhap, Void>() {
            @Override
            protected PhienDangNhap doInBackground() throws Exception {
                try { return service.login(name, secret); }
                finally { Arrays.fill(secret, '\0'); }
            }

            @Override
            protected void done() {
                try {
                    PhienDangNhap session = get();
                    CinemaWindow.open(session);
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    message.setText("Đăng nhập bị gián đoạn. Vui lòng thử lại.");
                } catch (ExecutionException e) {
                    message.setText(e.getCause() instanceof IllegalArgumentException
                            ? e.getCause().getMessage()
                            : "Không thể đăng nhập. Vui lòng kiểm tra cấu hình và kết nối CSDL.");
                } finally {
                    password.setText("");
                    submit.setEnabled(true);
                    username.setEnabled(true);
                    password.setEnabled(true);
                }
            }
        }.execute();
    }

    private static BufferedImage loadAuditorium() {
        try (InputStream in = DangNhapWindow.class.getResourceAsStream("/ui/auditorium.jpg")) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class AuditoriumHero extends JPanel {
        AuditoriumHero() {
            setOpaque(false);
            putClientProperty("cinema.background", "none");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth(), h = getHeight();
                int r = Theme.s(Theme.RADIUS);
                Shape clip = new RoundRectangle2D.Float(0, 0, w + r, h, r, r);
                g2.setClip(clip);
                g2.setColor(Color.decode("#140F10"));
                g2.fillRect(0, 0, w, h);

                if (AUDITORIUM != null) {
                    int srcY = (int) (AUDITORIUM.getHeight() * 0.24);
                    int srcH = AUDITORIUM.getHeight() - srcY;
                    int srcW = AUDITORIUM.getWidth();
                    double scale = Math.max((double) w / srcW, (double) h / srcH);
                    int drawW = (int) Math.ceil(srcW * scale);
                    int drawH = (int) Math.ceil(srcH * scale);
                    int dx = (w - drawW) / 2;
                    int dy = (h - drawH) / 2;
                    g2.drawImage(AUDITORIUM, dx, dy, dx + drawW, dy + drawH, 0, srcY, srcW, AUDITORIUM.getHeight(), null);
                }

                // Warm top atmospheric vignette + deep editorial bottom scrim
                g2.setPaint(new GradientPaint(0, 0, new Color(24, 14, 16, 110),
                        0, (int) (h * 0.42), new Color(20, 12, 14, 24)));
                g2.fillRect(0, 0, w, (int) (h * 0.42));
                g2.setPaint(new GradientPaint(0, (int) (h * 0.35), new Color(18, 12, 13, 25),
                        0, h, new Color(16, 11, 12, 238)));
                g2.fillRect(0, 0, w, h);

                int left = Theme.s(32);
                int bottom = h - Theme.s(42);
                g2.setFont(Theme.bold(11));
                g2.setColor(Color.decode("#E5DCDE"));
                g2.drawString("HỆ THỐNG QUẢN LÝ", left, bottom - Theme.s(42));

                g2.setFont(Theme.bold(32));
                g2.setColor(Color.WHITE);
                String title = "Rạp chiếu phim";
                g2.drawString(title, left, bottom);
                int dotX = left + g2.getFontMetrics().stringWidth(title) + 2;
                g2.setColor(Color.decode("#BEA8FA"));
                g2.drawString(".", dotX, bottom);
            } finally {
                g2.dispose();
            }
        }
    }
}
