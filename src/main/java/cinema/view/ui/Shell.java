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
    private final JPanel sidebar = new JPanel(new BorderLayout(8, 12));
    private final JLabel account;
    private final JLabel pageTitle;

    public static Icon brandGlyph() {
        return new Icon() {
            @Override public int getIconWidth() { return Theme.s(32); }
            @Override public int getIconHeight() { return Theme.s(32); }
            @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    int w = getIconWidth(), h = getIconHeight();
                    g.translate(x + w / 2.0, y + h / 2.0);
                    g.rotate(Math.toRadians(-6));
                    g.setColor(Tokens.accent());
                    g.fillRoundRect(-w / 2, -h / 2, w, h, Theme.s(9), Theme.s(9));
                    g.setColor(Tokens.accentInk());
                    int barW = Math.max(3, Theme.s(4));
                    int h1 = Theme.s(15);
                    int h2 = Theme.s(21);
                    g.shear(-0.25, 0);
                    g.fillRect(-Theme.s(5), -h1 / 2, barW, h1);
                    g.fillRect(Theme.s(1), -h2 / 2, barW, h2);
                } finally {
                    g.dispose();
                }
            }
        };
    }

    public Shell(PhienDangNhap session, JComponent content, JList<String> menu, IntConsumer navigate, String title) {
        super(new BorderLayout(0, 0));
        Theme.install();

        String name = session.nhanVien() == null ? session.tenDangNhap() : session.nhanVien().hoTen();
        account = Design.text(name, "heading");
        pageTitle = Design.text(title, "heading");

        // Full-width top bar matching layout.html (.app-top)
        JPanel topBar = new JPanel(new BorderLayout(Theme.s(16), 0));
        topBar.putClientProperty("cinema.background", "surface");
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new AbstractBorder() {
                    @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, 1, 0); }
                    @Override public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, 1, 0); return i; }
                    @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                        g.setColor(Tokens.border());
                        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
                    }
                },
                BorderFactory.createEmptyBorder(Theme.s(10), Theme.s(22), Theme.s(10), Theme.s(22))));

        JPanel brandBox = new JPanel();
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.X_AXIS));
        brandBox.setOpaque(false);
        brandBox.putClientProperty("cinema.background", "none");
        JLabel brandGlyphLbl = new JLabel(brandGlyph());
        JLabel brandStrong = new JLabel("Cinema");
        brandStrong.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            brandStrong.setFont(Tokens.bold(19));
            brandStrong.setForeground(Tokens.text());
        });
        JLabel brandLight = new JLabel("Workspace");
        brandLight.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            brandLight.setFont(Tokens.font(18));
            brandLight.setForeground(Tokens.muted());
        });
        pageTitle.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            pageTitle.setFont(Tokens.font(14));
            pageTitle.setForeground(Tokens.muted());
        });
        brandBox.add(brandGlyphLbl);
        brandBox.add(Box.createHorizontalStrut(Theme.s(10)));
        brandBox.add(brandStrong);
        brandBox.add(Box.createHorizontalStrut(Theme.s(4)));
        brandBox.add(brandLight);
        brandBox.add(Box.createHorizontalStrut(Theme.s(16)));
        brandBox.add(pageTitle);
        topBar.add(brandBox, BorderLayout.WEST);

        JToggleButton customerSite = new JToggleButton("Trang khách", new Icons("film", 16));
        customerSite.putClientProperty("cinema.noIcon", true);
        customerSite.setMaximumSize(new Dimension(Theme.s(144), Theme.s(38)));
        customerSite.addActionListener(e -> {
            customerSite.setSelected(false);
            KhachHangWindow.open();
        });

        JButton themeBtn = Theme.toggleButton();
        themeBtn.putClientProperty("cinema.iconOnly", true);
        themeBtn.setText("");
        themeBtn.setToolTipText("Chuyển giao diện sáng hoặc tối");
        themeBtn.setPreferredSize(new Dimension(Theme.s(40), Theme.s(38)));
        themeBtn.setMaximumSize(new Dimension(Theme.s(40), Theme.s(38)));
        themeBtn.setMargin(new Insets(0, 0, 0, 0));

        JPanel userChip = new JPanel();
        userChip.setLayout(new BoxLayout(userChip, BoxLayout.X_AXIS));
        userChip.setOpaque(false);
        userChip.putClientProperty("cinema.background", "none");
        userChip.setAlignmentY(Component.CENTER_ALIGNMENT);
        JLabel userIcon = new JLabel(new Icons("people", 16));
        userIcon.putClientProperty("cinema.statusRefresh", (Runnable) () -> userIcon.setForeground(Tokens.accent()));
        account.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            account.setFont(Tokens.bold(13));
            account.setForeground(Tokens.text());
        });
        userChip.add(userIcon);
        userChip.add(Box.createHorizontalStrut(Theme.s(7)));
        userChip.add(account);

        customerSite.setAlignmentY(Component.CENTER_ALIGNMENT);
        themeBtn.setAlignmentY(Component.CENTER_ALIGNMENT);

        JPanel barTools = new JPanel();
        barTools.setLayout(new BoxLayout(barTools, BoxLayout.X_AXIS));
        barTools.setOpaque(false);
        barTools.putClientProperty("cinema.background", "none");
        barTools.add(customerSite);
        barTools.add(Box.createHorizontalStrut(Theme.s(10)));
        barTools.add(themeBtn);
        barTools.add(Box.createHorizontalStrut(Theme.s(14)));
        barTools.add(userChip);
        topBar.add(barTools, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Sidebar matching layout.html (.app-side + .sidebar-role + nav + .logout)
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
                BorderFactory.createEmptyBorder(Theme.s(16), Theme.s(14), Theme.s(18), Theme.s(14))));
        sidebar.setPreferredSize(new Dimension(Theme.s(236), 0));

        JPanel roleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.s(8), Theme.s(4)));
        roleRow.setOpaque(false);
        roleRow.putClientProperty("cinema.background", "none");
        JComponent statusDot = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(Theme.s(9), Theme.s(9)); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    g.setColor(Tokens.success());
                    int d = Math.min(getWidth(), getHeight());
                    g.fillOval(0, 0, d, d);
                } finally {
                    g.dispose();
                }
            }
        };
        JLabel roleLabel = new JLabel(session.vaiTro() == VaiTro.ADMIN ? "Quản trị viên" : "Nhân viên");
        roleLabel.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            roleLabel.setFont(Tokens.bold(12));
            roleLabel.setForeground(Tokens.muted());
        });
        roleRow.add(statusDot);
        roleRow.add(roleLabel);
        roleRow.setBorder(BorderFactory.createCompoundBorder(
                new AbstractBorder() {
                    @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, Theme.s(8), 0); }
                    @Override public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, Theme.s(8), 0); return i; }
                    @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                        g.setColor(Tokens.border());
                        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
                    }
                },
                BorderFactory.createEmptyBorder(0, Theme.s(6), 0, 0)));
        sidebar.add(roleRow, BorderLayout.NORTH);

        menu.putClientProperty("cinema.nav", true);
        menu.setBorder(BorderFactory.createEmptyBorder(Theme.s(4), 0, Theme.s(4), 0));
        menu.setFixedCellHeight(Theme.s(48));
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

        JButton exit = new JButton("Đăng xuất", new Icons("exit"));
        exit.putClientProperty("cinema.customBorder", true);
        exit.putClientProperty("cinema.background", "none");
        exit.setOpaque(false);
        exit.setContentAreaFilled(false);
        exit.setBorderPainted(false);
        exit.setHorizontalAlignment(SwingConstants.LEFT);
        exit.setBorder(BorderFactory.createEmptyBorder(Theme.s(8), Theme.s(12), Theme.s(8), Theme.s(12)));
        exit.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(40)));
        exit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exit.addActionListener(e -> logout());
        JPanel footer = new JPanel(new GridLayout(1, 1, 0, 0));
        footer.putClientProperty("cinema.background", "sidebar");
        footer.add(exit);
        sidebar.add(footer, BorderLayout.SOUTH);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.putClientProperty("cinema.background", "canvas");
        workspace.add(sidebar, BorderLayout.WEST);

        JScrollPane pageScroll = new JScrollPane(new AdaptivePage(content));
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        workspace.add(pageScroll, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "cinema.search");
        getActionMap().put("cinema.search", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { menu.requestFocusInWindow(); }
        });

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int width = getWidth() < Theme.s(1120) ? Theme.s(204) : Theme.s(236);
                sidebar.setPreferredSize(new Dimension(width, 0));
                revalidate();
            }
        });
        Theme.apply(this);
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
