package cinema.view.ui;

import cinema.dao.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

/** Presentation adapters retain original selection models and text sources. */
public final class SalesViews {
    private SalesViews() {}

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JPanel roundedFrame(JComponent inner) {
        JPanel frame = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    int w = getWidth(), h = getHeight(), arc = Theme.s(10);
                    Shape shape = new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, arc, arc);
                    g.setColor(Tokens.surface());
                    g.fill(shape);
                    g.setColor(Tokens.border());
                    g.draw(shape);
                } finally {
                    g.dispose();
                }
            }
        };
        frame.setOpaque(false);
        frame.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        frame.add(inner, BorderLayout.CENTER);
        return frame;
    }

    public static final class Shows extends JPanel {
        private boolean syncing;
        private final JTable table;

        public Shows(JComboBox<BanVeDAO.SuatBan> source, Runnable confirm) {
            super(new BorderLayout());
            setOpaque(false);
            DefaultTableModel rows = model("Suất", "Phim", "Phòng", "Bắt đầu");
            table = new JTable(rows);
            table.putClientProperty("cinema.compactTable", true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowHeight(26);
            table.setToolTipText("Nhấp đúp vào suất chiếu hoặc bấm Chọn suất để mở sơ đồ ghế");
            table.getColumnModel().getColumn(0).setPreferredWidth(54);
            table.getColumnModel().getColumn(1).setPreferredWidth(185);
            table.getColumnModel().getColumn(2).setPreferredWidth(105);
            table.getColumnModel().getColumn(3).setPreferredWidth(165);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(0, Theme.s(72)));
            add(roundedFrame(scroll), BorderLayout.CENTER);
            source.setVisible(false);
            add(source, BorderLayout.SOUTH);

            Runnable sync = () -> {
                if (syncing) return;
                syncing = true;
                try {
                    rows.setRowCount(0);
                    for (int i = 0; i < source.getItemCount(); i++) {
                        var show = source.getItemAt(i);
                        rows.addRow(new Object[]{show.suat().maSuat(), show.tenPhim(), show.tenPhong(), show.suat().thoiGianBatDau()});
                    }
                    int index = source.getSelectedIndex();
                    if (index >= 0 && index < rows.getRowCount()) {
                        table.setRowSelectionInterval(index, index);
                    }
                } finally {
                    syncing = false;
                }
            };

            source.getModel().addListDataListener(new ListDataListener() {
                @Override public void intervalAdded(ListDataEvent e) { sync.run(); }
                @Override public void intervalRemoved(ListDataEvent e) { sync.run(); }
                @Override public void contentsChanged(ListDataEvent e) { sync.run(); }
            });
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!syncing && !e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                    source.setSelectedIndex(table.getSelectedRow());
                }
            });
            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                        source.setSelectedIndex(table.getSelectedRow());
                        confirm.run();
                    }
                }
            });
            table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "cinema.confirmShow");
            table.getActionMap().put("cinema.confirmShow", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { confirm.run(); }
            });
            sync.run();
            Theme.apply(this);
            setOpaque(false);
        }
    }

    public static final class Transactions extends JPanel {
        private boolean syncing;

        public Transactions(JList<GiaoDichDAO.Dong> source) {
            super(new BorderLayout());
            setOpaque(false);
            DefaultTableModel rows = model("Mã giao dịch", "Thời gian", "Nhân viên", "Trạng thái", "Tổng tiền (VND)");
            JTable table = new JTable(rows);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getColumnModel().getColumn(0).setPreferredWidth(160);
            table.getColumnModel().getColumn(1).setPreferredWidth(135);
            table.getColumnModel().getColumn(2).setPreferredWidth(130);
            table.getColumnModel().getColumn(3).setPreferredWidth(135);
            table.getColumnModel().getColumn(4).setPreferredWidth(155);

            add(roundedFrame(new JScrollPane(table)), BorderLayout.CENTER);
            source.setVisible(false);
            add(source, BorderLayout.SOUTH);

            Runnable sync = () -> {
                if (syncing) return;
                syncing = true;
                try {
                    rows.setRowCount(0);
                    for (int i = 0; i < source.getModel().getSize(); i++) {
                        var row = source.getModel().getElementAt(i);
                        var sale = row.giaoDich();
                        rows.addRow(new Object[]{sale.maGiaoDich(), sale.thoiGianTao(), row.hoTenNhanVien(), sale.trangThai(), sale.tongTien()});
                    }
                    if (source.getSelectedIndex() >= 0 && source.getSelectedIndex() < rows.getRowCount()) {
                        table.setRowSelectionInterval(source.getSelectedIndex(), source.getSelectedIndex());
                    }
                } finally {
                    syncing = false;
                }
            };

            source.getModel().addListDataListener(new ListDataListener() {
                @Override public void intervalAdded(ListDataEvent e) { sync.run(); }
                @Override public void intervalRemoved(ListDataEvent e) { sync.run(); }
                @Override public void contentsChanged(ListDataEvent e) { sync.run(); }
            });
            source.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) sync.run();
            });
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!syncing && !e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                    source.setSelectedIndex(table.getSelectedRow());
                }
            });
            sync.run();
            Theme.apply(this);
            setOpaque(false);
        }
    }

    public static final class Details extends JPanel {
        private final JPanel rows = new Rows();
        private final JLabel total = Design.text("Chưa tính tiền", "title");
        private final JLabel state = new StatusPillLabel("Chưa có giao dịch");
        private final JTextArea source;
        private final String emptyStateText;
        private String draftStatus;
        private String draftTotal;
        private java.util.List<String[]> draftItems;

        public Details(JTextArea source, String title) {
            super(new BorderLayout(8, 12));
            this.source = source;
            this.emptyStateText = title != null && title.contains("Chi tiết")
                    ? "Chọn giao dịch để xem chi tiết"
                    : "Chọn suất và ghế để bắt đầu";
            setOpaque(false);
            setPreferredSize(new Dimension(Theme.s(360), Theme.s(170)));
            setMinimumSize(new Dimension(Theme.s(320), Theme.s(150)));
            setBorder(BorderFactory.createEmptyBorder(
                    Theme.s(10), Theme.s(Theme.MD), Theme.s(10), Theme.s(Theme.MD)));

            rows.setOpaque(false);
            rows.addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) { update(); }
            });

            JPanel header = new JPanel(new BorderLayout(8, 8));
            header.setOpaque(false);
            header.add(Design.text(title, "heading"), BorderLayout.NORTH);
            JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            badgeRow.setOpaque(false);
            badgeRow.add(state);
            header.add(badgeRow, BorderLayout.CENTER);
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tokens.mix(Tokens.surface(), Tokens.border(), .5)),
                    BorderFactory.createEmptyBorder(0, 0, Theme.s(Theme.SM), 0)));
            header.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                header.setOpaque(false);
                badgeRow.setOpaque(false);
                header.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Tokens.mix(Tokens.surface(), Tokens.border(), .5)),
                        BorderFactory.createEmptyBorder(0, 0, Theme.s(Theme.SM), 0)));
            });

            add(header, BorderLayout.NORTH);

            JScrollPane scroll = new JScrollPane(rows);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            add(scroll, BorderLayout.CENTER);

            total.putClientProperty("cinema.text", "total");
            total.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                total.setFont(Theme.number(24));
                total.setForeground(Tokens.accent());
            });

            JPanel totalBox = new JPanel(new BorderLayout(8, 4)) {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = Skin.graphics(graphics);
                    try {
                        int w = getWidth(), h = getHeight(), arc = Theme.s(10);
                        g.setColor(Tokens.tone(Tokens.accent()));
                        g.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                        g.setColor(Tokens.mix(Tokens.surface(), Tokens.accent(), .42));
                        g.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                    } finally {
                        g.dispose();
                    }
                }
            };
            totalBox.setOpaque(false);
            totalBox.setBorder(BorderFactory.createEmptyBorder(
                    Theme.s(8), Theme.s(Theme.MD), Theme.s(8), Theme.s(Theme.MD)));
            totalBox.add(Design.text("Tổng cộng", "subtitle"), BorderLayout.NORTH);
            totalBox.add(total, BorderLayout.CENTER);
            source.setVisible(false);
            totalBox.add(source, BorderLayout.SOUTH);
            totalBox.putClientProperty("cinema.statusRefresh", (Runnable) () -> totalBox.setOpaque(false));
            add(totalBox, BorderLayout.SOUTH);

            source.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { draftItems = null; update(); }
                @Override public void removeUpdate(DocumentEvent e) { draftItems = null; update(); }
                @Override public void changedUpdate(DocumentEvent e) { draftItems = null; update(); }
            });
            update();
        }

        public void setDraftPreview(String status, java.util.List<String[]> items, String totalText) {
            this.draftStatus = status;
            this.draftItems = items == null ? null : new java.util.ArrayList<>(items);
            this.draftTotal = totalText;
            update();
        }

        public void clearDraftPreview() {
            if (draftItems != null || draftStatus != null || draftTotal != null) {
                draftItems = null;
                draftStatus = null;
                draftTotal = null;
                update();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Skin.graphics(graphics);
            try {
                int w = getWidth(), h = getHeight(), arc = Theme.s(14);
                Shape shape = new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, arc, arc);
                g.setColor(Tokens.surface());
                g.fill(shape);
                Shape prev = g.getClip();
                g.clip(shape);
                g.setColor(new Color(255, 255, 255, Tokens.dark ? 15 : 150));
                g.fillRect(0, 1, w, 1);
                g.setClip(prev);
                g.setColor(Tokens.border());
                g.draw(shape);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }

        private void update() {
            rows.removeAll();
            total.setText("Chưa tính tiền");
            state.setText(emptyStateText);

            java.util.List<String[]> items = new java.util.ArrayList<>();
            if (draftItems != null && !draftItems.isEmpty()) {
                if (draftStatus != null && !draftStatus.isBlank()) state.setText(draftStatus);
                if (draftTotal != null && !draftTotal.isBlank()) total.setText(draftTotal);
                items.addAll(draftItems);
            } else {
                for (String line : source.getText().split("\\R")) {
                    if (line.isBlank()) continue;
                    if (line.startsWith("Tổng tiền:")) {
                        total.setText(line.substring("Tổng tiền:".length()).trim());
                    }
                    if (line.startsWith("Trạng thái:")) {
                        state.setText(line.substring("Trạng thái:".length()).trim());
                    }
                    String key = "";
                    String value = line;
                    int colon = line.indexOf(':');
                    if (colon >= 0 && !line.startsWith("Ghế ")) {
                        key = line.substring(0, colon);
                        value = line.substring(colon + 1).trim();
                    } else if (line.startsWith("Ghế ")) {
                        key = "Ghế";
                    }
                    items.add(new String[]{key, value});
                }
            }

            int row = 0;
            for (int i = 0; i < items.size(); i++) {
                String key = items.get(i)[0];
                String value = items.get(i)[1];
                int gridY = row * 2;

                JLabel name = Design.text(key, "subtitle");
                JLabel content = new PlainWrapLabel(value);
                content.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                    content.setFont(Theme.font(13));
                    content.setForeground(Tokens.text());
                });

                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = gridY;
                c.weightx = .28;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(7, 0, 7, 10);
                rows.add(name, c);

                c.gridx = 1;
                c.weightx = .72;
                c.insets = new Insets(7, 0, 7, 0);
                rows.add(content, c);

                if (i < items.size() - 1) {
                    GridBagConstraints sep = new GridBagConstraints();
                    sep.gridx = 0;
                    sep.gridy = gridY + 1;
                    sep.gridwidth = 2;
                    sep.weightx = 1;
                    sep.fill = GridBagConstraints.HORIZONTAL;
                    rows.add(divider(), sep);
                }
                row++;
            }

            if (row == 0) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(8, 0, 8, 0);
                rows.add(Design.text("Thông tin giao dịch sẽ xuất hiện tại đây.", "subtitle"), c);
            }

            GridBagConstraints end = new GridBagConstraints();
            end.gridx = 0;
            end.gridy = row * 2 + 1;
            end.weighty = 1;
            rows.add(Box.createVerticalGlue(), end);

            Theme.apply(this);
            setOpaque(false);
            rows.setOpaque(false);
            state.setForeground(Theme.semantic(state.getText()));
            state.setFont(Theme.bold(12));
            total.setFont(Theme.number(24));
            total.setForeground(Tokens.accent());
            rows.revalidate();
            rows.repaint();
        }

        private static JComponent divider() {
            return new JComponent() {
                @Override public Dimension getPreferredSize() { return new Dimension(1, 1); }
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = Skin.graphics(graphics);
                    try {
                        g.setColor(Tokens.mix(Tokens.surface(), Tokens.border(), .4));
                        g.fillRect(0, 0, getWidth(), 1);
                    } finally {
                        g.dispose();
                    }
                }
            };
        }
    }

    private static final class StatusPillLabel extends JLabel {
        StatusPillLabel(String text) {
            super(text);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                setFont(Theme.bold(12));
                setForeground(Theme.semantic(getText()));
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Skin.graphics(graphics);
            try {
                String text = getText() == null ? "" : getText();
                Color tone = Theme.semantic(text);
                int w = getWidth(), h = getHeight();
                g.setColor(Tokens.tone(tone));
                g.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                g.setColor(Tokens.mix(Tokens.surface(), tone, .42));
                g.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static final class PlainWrapLabel extends JLabel {
        PlainWrapLabel(String text) {
            super(text == null ? "" : text);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            Font font = getFont() == null ? Theme.font(13) : getFont();
            FontMetrics fm = getFontMetrics(font);
            String text = getText() == null ? "" : getText();
            Container parent = getParent();
            int avail = getWidth() > 40 ? getWidth()
                    : (parent != null && parent.getWidth() > 60 ? (int) (parent.getWidth() * 0.68) : 210);
            java.util.List<String> lines = wrap(text, fm, avail);
            return new Dimension(Math.min(avail, fm.stringWidth(text) + 4), Math.max(1, lines.size()) * fm.getHeight());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Skin.graphics(graphics);
            try {
                Font font = getFont() == null ? Theme.font(13) : getFont();
                g.setFont(font);
                g.setColor(getForeground() == null ? Tokens.text() : getForeground());
                FontMetrics fm = g.getFontMetrics();
                String text = getText() == null ? "" : getText();
                int y = fm.getAscent();
                for (String line : wrap(text, fm, Math.max(80, getWidth()))) {
                    g.drawString(line, 0, y);
                    y += fm.getHeight();
                }
            } finally {
                g.dispose();
            }
        }

        private static java.util.List<String> wrap(String text, FontMetrics fm, int maxWidth) {
            java.util.List<String> lines = new java.util.ArrayList<>();
            if (text.isEmpty()) {
                lines.add("");
                return lines;
            }
            StringBuilder current = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (!current.isEmpty() && fm.stringWidth(candidate) > maxWidth) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    if (!current.isEmpty()) current.append(' ');
                    current.append(word);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
            return lines;
        }
    }

    private static final class Rows extends JPanel implements Scrollable {
        Rows() { super(new GridBagLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return Theme.s(24); }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Theme.s(120); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static JPanel field(String label, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel name = Design.text(label, "subtitle");
        name.setLabelFor(input);
        panel.add(name, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }
}
