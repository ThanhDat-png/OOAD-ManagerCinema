package cinema.view.ui;

import cinema.controller.auth.*;
import cinema.dao.TaiKhoanDAO;
import cinema.model.VaiTro;
import cinema.view.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

public final class Shell extends JPanel {
    private final JPanel sidebar = new JPanel(new BorderLayout(8, 16));
    private final JLabel account;
    private final JLabel pageTitle;

    public Shell(PhienDangNhap session, JComponent content, JList<String> menu, IntConsumer navigate, String title) {
        super(new BorderLayout(0, 0));
        Theme.install();

        sidebar.putClientProperty("cinema.background", "sidebar");
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                new AbstractBorder() {
                    @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 1); }
                    @Override public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, 0, 1); return i; }
                    @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                        g.setColor(Tokens.border());
                        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
                    }
                },
                BorderFactory.createEmptyBorder(Theme.s(Theme.XL), Theme.s(Theme.LG), Theme.s(20), Theme.s(Theme.LG))));
        sidebar.setPreferredSize(new Dimension(Theme.s(244), 0));

        JLabel logo = Design.text("CINEMA", "title");
        JLabel caption = Design.text("WORKSPACE / NỘI BỘ", "eyebrow");
        String name = session.nhanVien() == null ? session.tenDangNhap() : session.nhanVien().hoTen();
        account = Design.text(name, "heading");
        JLabel role = Design.text(session.vaiTro().getTen(), "subtitle");

        JPanel profileCard = new JPanel(new BorderLayout(Theme.s(10), 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    int w = getWidth(), h = getHeight(), r = Theme.s(10);
                    g.setColor(Tokens.bg());
                    g.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                    g.setColor(Tokens.border());
                    g.drawRoundRect(0, 0, w - 2, h - 2, r, r);
                } finally {
                    g.dispose();
                }
            }
        };
        profileCard.setOpaque(false);
        profileCard.putClientProperty("cinema.background", "none");
        profileCard.setBorder(BorderFactory.createEmptyBorder(Theme.s(10), Theme.s(10), Theme.s(10), Theme.s(10)));

        String initials = initialsOf(name);
        JLabel avatar = new JLabel(initials, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    int d = Math.min(getWidth(), getHeight()) - 1;
                    g.setColor(Tokens.tone(Tokens.accent()));
                    g.fillOval(0, 0, d, d);
                    g.setColor(Tokens.mix(Tokens.surface(), Tokens.accent(), 0.35));
                    g.drawOval(0, 0, d, d);
                    Font f = Tokens.bold(13);
                    g.setFont(f);
                    g.setColor(Tokens.accent());
                    var gv = f.createGlyphVector(g.getFontRenderContext(), initials);
                    var bounds = gv.getVisualBounds();
                    double tx = (d - bounds.getWidth()) / 2.0 - bounds.getX();
                    double ty = (d - bounds.getHeight()) / 2.0 - bounds.getY();
                    g.drawGlyphVector(gv, (float) tx, (float) ty);
                } finally {
                    g.dispose();
                }
            }
        };
        avatar.setPreferredSize(new Dimension(Theme.s(34), Theme.s(34)));
        avatar.putClientProperty("cinema.text", "avatar");
        avatar.setFont(Tokens.bold(13));
        profileCard.add(avatar, BorderLayout.WEST);

        JPanel userMeta = new JPanel(new GridLayout(2, 1, 0, Theme.s(2)));
        userMeta.putClientProperty("cinema.background", "none");
        userMeta.setOpaque(false);
        userMeta.add(account);
        userMeta.add(role);
        profileCard.add(userMeta, BorderLayout.CENTER);

        JPanel identity = Design.stack(logo, caption, Box.createVerticalStrut(Theme.s(10)), profileCard);
        identity.putClientProperty("cinema.background", "sidebar");
        sidebar.add(identity, BorderLayout.NORTH);

        menu.putClientProperty("cinema.nav", true);
        menu.setBorder(BorderFactory.createEmptyBorder(Theme.s(4), 0, Theme.s(4), 0));
        menu.setFixedCellHeight(Theme.s(50));
        menu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menu.setCellRenderer(new NavCellRenderer());
        menu.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int index = menu.locationToIndex(e.getPoint());
                if (index >= 0) navigate.accept(index);
            }
        });
        menu.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "cinema.open");
        menu.getActionMap().put("cinema.open", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (menu.getSelectedIndex() >= 0) navigate.accept(menu.getSelectedIndex());
            }
        });
        JScrollPane navigation = new JScrollPane(menu);
        navigation.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(navigation, BorderLayout.CENTER);

        JButton theme = Theme.toggleButton();
        theme.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(40)));
        JButton exit = new JButton("Đăng xuất", new Icons("exit"));
        exit.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(40)));
        exit.addActionListener(e -> logout());
        JPanel footer = new JPanel(new GridLayout(2, 1, 0, Theme.s(8)));
        footer.putClientProperty("cinema.background", "sidebar");
        footer.add(theme);
        footer.add(exit);
        sidebar.add(footer, BorderLayout.SOUTH);
        add(sidebar, BorderLayout.WEST);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.putClientProperty("cinema.background", "canvas");

        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
                new AbstractBorder() {
                    @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, 1, 0); }
                    @Override public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, 1, 0); return i; }
                    @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                        g.setColor(Tokens.border());
                        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
                    }
                },
                BorderFactory.createEmptyBorder(Theme.s(Theme.MD), Theme.s(Theme.XL), Theme.s(Theme.MD), Theme.s(Theme.XL))));
        pageTitle = Design.text(title, "heading");
        bar.add(pageTitle, BorderLayout.WEST);

        JTextField search = new Design.SearchField(18, "Tìm chức năng · Ctrl+K");
        search.setToolTipText("Tìm chức năng được phép · Ctrl+K");
        search.getAccessibleContext().setAccessibleName("Tìm chức năng");
        search.setText("");
        search.setBorder(new Skin.ControlBorder());
        search.setMaximumSize(new Dimension(Theme.s(240), Theme.s(40)));
        search.addActionListener(e -> {
            String text = search.getText().trim().toLowerCase();
            for (int i = 0; i < menu.getModel().getSize(); i++) {
                if (menu.getModel().getElementAt(i).toLowerCase().contains(text)) {
                    menu.setSelectedIndex(i);
                    navigate.accept(i);
                    break;
                }
            }
        });

        JToggleButton customerSite = new JToggleButton("Trang khách", new Icons("film", 16));
        customerSite.putClientProperty("cinema.noIcon", true);
        customerSite.setMaximumSize(new Dimension(Theme.s(150), Theme.s(40)));
        customerSite.addActionListener(e -> {
            customerSite.setSelected(false);
            KhachHangWindow.open();
        });

        JPanel barTools = new JPanel();
        barTools.setLayout(new BoxLayout(barTools, BoxLayout.X_AXIS));
        barTools.add(customerSite);
        barTools.add(Box.createHorizontalStrut(Theme.s(10)));
        barTools.add(search);
        bar.add(barTools, BorderLayout.EAST);

        JScrollPane pageScroll = new JScrollPane(new AdaptivePage(content));
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        workspace.add(pageScroll, BorderLayout.CENTER);
        workspace.add(bar, BorderLayout.NORTH);
        add(workspace, BorderLayout.CENTER);

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "cinema.search");
        getActionMap().put("cinema.search", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { search.requestFocusInWindow(); }
        });

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int width = getWidth() < Theme.s(1120) ? Theme.s(208) : Theme.s(244);
                sidebar.setPreferredSize(new Dimension(width, 0));
                revalidate();
            }
        });
        Theme.apply(this);
    }

    private static String initialsOf(String name) {
        if (name == null || name.isBlank()) return "NV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static final class NavCellRenderer extends JPanel implements ListCellRenderer<String> {
        private final JLabel label = new JLabel();
        private boolean selected;

        NavCellRenderer() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(Theme.s(2), 0, Theme.s(2), 0));
            label.setBorder(BorderFactory.createEmptyBorder(Theme.s(8), Theme.s(14), Theme.s(8), Theme.s(10)));
            label.setIconTextGap(Theme.s(12));
            add(label, BorderLayout.CENTER);
        }

        @Override public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                boolean selected, boolean focus) {
            this.selected = selected;
            String text = value == null ? "" : value;
            label.setText(text);
            label.setToolTipText(text);
            label.setFont(selected ? Tokens.bold(14) : Tokens.font(14));
            label.setForeground(selected ? Tokens.accent() : Tokens.muted());
            label.setIcon(new Icons(Icons.forTitle(text), 18));
            return this;
        }

        @Override protected void paintComponent(Graphics graphics) {
            if (!selected) return;
            Graphics2D g = Skin.graphics(graphics);
            try {
                int w = getWidth(), h = getHeight(), pad = Theme.s(2);
                int r = Theme.s(10);
                g.setColor(Tokens.tone(Tokens.accent()));
                g.fillRoundRect(0, pad, w, h - pad * 2, r, r);
                g.setColor(Tokens.accent());
                g.fillRoundRect(0, pad + Theme.s(8), Theme.s(3), Math.max(8, h - pad * 2 - Theme.s(16)), 4, 4);
            } finally {
                g.dispose();
            }
        }
    }

    public void setPageTitle(String title) { pageTitle.setText(title); }

    public static void attach(JFrame frame, PhienDangNhap session, String[] functions, IntConsumer navigate) {
        Container old = frame.getContentPane();
        JList<String> menu = new JList<>(functions);
        for (int i = 0; i < functions.length; i++) {
            if (frame.getTitle().equals(functions[i])
                    || frame.getTitle().equals("Quản lý bán vé") && functions[i].equals("Bán vé tại quầy")
                    || frame.getTitle().equals("Kiểm tra và xác nhận vé") && functions[i].equals("Soát vé")
                    || frame.getTitle().equals("Thông tin cá nhân") && functions[i].equals("Hồ sơ cá nhân")) {
                menu.setSelectedIndex(i);
            }
        }
        frame.setContentPane(new Shell(session, (JComponent) old, menu, navigate, frame.getTitle()));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(Math.min(1366, screen.width - 40), Math.min(820, screen.height - 56));
        frame.setMinimumSize(new Dimension(960, 620));
        Theme.apply(frame);
    }

    public static JPanel home(PhienDangNhap session, String[] functions, IntConsumer navigate, JButton open) {
        JPanel page = new JPanel(new BorderLayout(16, 24));
        page.putClientProperty("cinema.background", "canvas");
        page.setBorder(BorderFactory.createEmptyBorder(Theme.s(Theme.XL), Theme.s(Theme.XL), Theme.s(Theme.XL), Theme.s(Theme.XL)));
        JPanel heading = Design.stack(
                Design.text(session.vaiTro() == VaiTro.ADMIN ? "Không gian quản trị" : "Không gian làm việc", "title"),
                Design.text("Chọn chức năng để bắt đầu. Dữ liệu và quyền truy cập theo phiên đăng nhập.", "subtitle"));
        heading.putClientProperty("cinema.background", "canvas");
        page.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.putClientProperty("cinema.background", "canvas");
        for (int i = 0; i < functions.length; i++) {
            final int index = i;
            Design.Card card = new Design.Card(new BorderLayout(12, 12));
            JLabel name = Design.text(functions[i], "heading");
            name.setIcon(new Icons(Icons.forTitle(functions[i]), 22));
            name.setIconTextGap(12);
            card.add(name, BorderLayout.NORTH);
            String hint = switch (i) {
                case 0 -> "Hồ sơ, thông tin và quyền thao tác";
                case 1 -> "Danh mục, bộ lọc và thông tin chi tiết";
                case 2 -> "Không gian chiếu và thông tin vận hành";
                default -> "Mở khu vực thao tác theo vai trò";
            };
            card.add(Design.text(hint, "subtitle"));
            JButton go = new JButton("Mở " + functions[i].toLowerCase(), new Icons("arrow", 16));
            go.addActionListener(e -> navigate.accept(index));
            card.add(go, BorderLayout.SOUTH);
            grid.add(card);
        }
        page.add(grid, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.putClientProperty("cinema.background", "canvas");
        footer.add(open);
        page.add(footer, BorderLayout.SOUTH);
        Theme.apply(page);
        page.setBackground(Tokens.bg());
        return page;
    }

    /** Scroll only when the available logical area cannot contain usable controls. */
    private static final class AdaptivePage extends JPanel implements Scrollable {
        AdaptivePage(JComponent content) {
            super(new BorderLayout());
            putClientProperty("cinema.background", "canvas");
            add(content);
        }
        public Dimension getPreferredSize() { return new Dimension(Theme.s(900), Math.max(Theme.s(580), super.getPreferredSize().height)); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return Theme.s(Theme.XL); }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(Theme.s(Theme.XL), (orientation == SwingConstants.VERTICAL ? r.height : r.width) - Theme.s(Theme.XL)); }
        public boolean getScrollableTracksViewportWidth() { return getParent() != null && getParent().getWidth() >= Theme.s(760); }
        public boolean getScrollableTracksViewportHeight() { return getParent() != null && getParent().getHeight() >= getPreferredSize().height; }
    }

    private void logout() {
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w instanceof JFrame frame) {
                for (Component c : Design.all(frame)) {
                    if (c instanceof BanVePanel sale && !sale.requestClose()) return;
                }
            }
        }
        if (JOptionPane.showConfirmDialog(this, "Đăng xuất khỏi phiên làm việc hiện tại?", "Đăng xuất",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        for (Window w : Window.getWindows()) if (w.isDisplayable()) w.dispose();
        new DangNhapWindow(new DangNhapBUS(new TaiKhoanDAO())).setVisible(true);
    }
}
