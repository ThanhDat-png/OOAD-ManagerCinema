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

public final class DangNhapWindow extends JFrame {
    static { Theme.install(); }
    private static final BufferedImage AUDITORIUM = loadAuditorium();

    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JButton submit = new JButton("Đăng nhập");
    private final JLabel message = new Design.WrappingLabel("Nhập tài khoản đã được cấp để đăng nhập.");

    public DangNhapWindow(DangNhapBUS service) {
        super("Đăng nhập — Quản lý rạp chiếu phim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel stage = new JPanel(new BorderLayout());
        stage.putClientProperty("cinema.background", "canvas");

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.putClientProperty("cinema.background", "canvas");
        topHeader.setBorder(BorderFactory.createEmptyBorder(18, 36, 4, 36));
        JLabel topBrand = Design.text("CINEMA WORKSPACE", "heading");
        topBrand.setIcon(new Icons("film", 22));
        topBrand.setIconTextGap(10);
        topHeader.add(topBrand, BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.putClientProperty("cinema.background", "canvas");
        tools.add(Theme.toggleButton());
        topHeader.add(tools, BorderLayout.EAST);
        stage.add(topHeader, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.putClientProperty("cinema.background", "canvas");

        Design.Card splitCard = new Design.Card(new GridLayout(1, 2, 0, 0));
        splitCard.setBorder(BorderFactory.createLineBorder(Tokens.border()));
        splitCard.setPreferredSize(new Dimension(920, 580));
        splitCard.add(new AuditoriumHero());
        splitCard.add(buildFormPanel());

        GridBagConstraints rootC = new GridBagConstraints();
        rootC.weightx = 1;
        rootC.weighty = 1;
        rootC.insets = new Insets(8, 24, 20, 24);
        center.add(splitCard, rootC);
        stage.add(center, BorderLayout.CENTER);

        setContentPane(stage);
        Theme.apply(stage);
        SwingUtilities.invokeLater(username::requestFocusInWindow);
        getRootPane().setDefaultButton(submit);
        submit.addActionListener(event -> authenticate(service));

        setSize(1024, 700);
        setMinimumSize(new Dimension(760, 600));
        setLocationRelativeTo(null);
    }

    private JPanel buildFormPanel() {
        JPanel formSide = new JPanel(new BorderLayout(0, 10));
        formSide.setBorder(BorderFactory.createEmptyBorder(24, 36, 20, 36));

        JLabel badge = new JLabel("", new Icons("ticket", 24), SwingConstants.LEFT);
        JPanel header = Design.stack(
                badge,
                Design.text("Đăng nhập", "title"),
                Design.text("Sử dụng tài khoản được cấp để vào không gian làm việc.", "subtitle"));
        formSide.add(header, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 7, 0);

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
        peek.setToolTipText("Hiện hoặc ẩn mật khẩu");
        peek.getAccessibleContext().setAccessibleName("Hiện mật khẩu");
        peek.setPreferredSize(new Dimension(42, 40));
        peek.addActionListener(e -> password.setEchoChar(peek.isSelected() ? (char) 0 : defaultEcho));

        JPanel secretInput = new JPanel(new BorderLayout(6, 0));
        secretInput.add(password, BorderLayout.CENTER);
        secretInput.add(peek, BorderLayout.EAST);

        JPanel secret = new JPanel(new BorderLayout(0, 4));
        secret.add(secretInput, BorderLayout.CENTER);
        JLabel required = Design.text("Vui lòng nhập mật khẩu.", "error");
        required.setVisible(false);
        secret.add(required, BorderLayout.SOUTH);
        c.gridy = 3;
        fields.add(secret, c);

        Forms.bind(username, "Tên đăng nhập");
        Forms.password(password, required);
        formSide.add(fields, BorderLayout.CENTER);

        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(42)));
        Design.status(message);
        message.setPreferredSize(new Dimension(320, Theme.s(48)));

        JToggleButton customerLink = new JToggleButton("Xem phim và lịch chiếu →", new Icons("film", 16));
        customerLink.putClientProperty("cinema.noIcon", true);
        customerLink.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(38)));
        customerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        customerLink.addActionListener(e -> {
            customerLink.setSelected(false);
            KhachHangWindow.open();
        });

        formSide.add(Design.stack(message, submit, customerLink), BorderLayout.SOUTH);
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
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth(), h = getHeight();
            Shape clip = new RoundRectangle2D.Float(0, 0, w + 16, h, 16, 16);
            g2.setClip(clip);
            g2.setColor(Color.decode("#1E1624"));
            g2.fillRect(0, 0, w, h);

            if (AUDITORIUM != null) {
                double scale = Math.max((double) w / AUDITORIUM.getWidth(), (double) h / AUDITORIUM.getHeight());
                int iw = (int) (AUDITORIUM.getWidth() * scale);
                int ih = (int) (AUDITORIUM.getHeight() * scale);
                g2.drawImage(AUDITORIUM, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            }
            g2.setPaint(new GradientPaint(0, (int) (h * 0.25), new Color(16, 12, 23, 30),
                    0, h, new Color(16, 12, 23, 235)));
            g2.fillRect(0, 0, w, h);

            int left = 32;
            int bottom = h - 44;
            g2.setFont(Theme.bold(12));
            g2.setColor(Color.decode("#E5DCDE"));
            g2.drawString("HỆ THỐNG QUẢN LÝ", left, bottom - 42);

            g2.setFont(Theme.bold(32));
            g2.setColor(Color.WHITE);
            String title = "Rạp chiếu phim";
            g2.drawString(title, left, bottom);
            int dotX = left + g2.getFontMetrics().stringWidth(title) + 2;
            g2.setColor(Color.decode("#BEA8FA"));
            g2.drawString(".", dotX, bottom);
            g2.dispose();
        }
    }
}
