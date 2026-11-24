package cinema.view.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.event.*;

public final class ReadOnlyViews {
    private ReadOnlyViews() {}

    public static JPanel info(JTextArea source, String title) {
        return info(source, title, false);
    }

    private static JPanel info(JTextArea source, String title, boolean compact) {
        Design.Card card = new Design.Card(new BorderLayout(Theme.s(Theme.MD), Theme.s(Theme.MD)));
        JPanel fields = new InfoRows();
        fields.setOpaque(false);

        JLabel heading = Design.text(title, "heading");
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tokens.mix(Tokens.surface(), Tokens.border(), .45)),
                BorderFactory.createEmptyBorder(0, 0, Theme.s(Theme.SM), 0)));
        heading.putClientProperty("cinema.statusRefresh", (Runnable) () ->
                heading.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Tokens.mix(Tokens.surface(), Tokens.border(), .45)),
                        BorderFactory.createEmptyBorder(0, 0, Theme.s(Theme.SM), 0))));
        card.add(heading, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(fields);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);

        source.setVisible(false);
        card.add(source, BorderLayout.SOUTH);

        Runnable refresh = () -> {
            fields.removeAll();
            String[] lines = source.getText().split("\\R");
            if (compact) {
                java.util.List<String> priority = java.util.List.of(
                        "Phim:", "Phòng:", "Ghế:", "Suất chiếu:", "Bắt đầu:",
                        "Trạng thái vé:", "Thanh toán:", "Giá vé:", "Mã vé:");
                java.util.Arrays.sort(lines, java.util.Comparator.comparingInt(line -> {
                    for (int i = 0; i < priority.size(); i++) {
                        if (line.startsWith(priority.get(i))) return i;
                    }
                    return priority.size();
                }));
            }

            java.util.List<String[]> entries = new java.util.ArrayList<>();
            for (String line : lines) {
                if (line.isBlank()) continue;
                int colon = line.indexOf(':');
                String name = colon < 0 ? "" : line.substring(0, colon);
                String value = colon < 0 ? line : line.substring(colon + 1).trim();
                entries.add(new String[]{name, value});
            }

            int row = 0;
            for (int i = 0; i < entries.size(); i++) {
                String name = entries.get(i)[0];
                String value = entries.get(i)[1];
                int gridY = compact ? (row / 2) * 2 : row * 2;
                int colOffset = compact ? (row % 2) * 2 : 0;

                JLabel keyLbl = Design.text(name, "subtitle");
                JLabel valLbl = createValueLabel(name, value);

                GridBagConstraints c = new GridBagConstraints();
                c.gridx = colOffset;
                c.gridy = gridY;
                c.weightx = compact ? .14 : .26;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.insets = new Insets(9, 4, 9, 12);
                fields.add(keyLbl, c);

                c.gridx = colOffset + 1;
                c.weightx = compact ? .36 : .74;
                c.insets = new Insets(9, 0, 9, compact && row % 2 == 0 ? 18 : 4);
                fields.add(valLbl, c);

                boolean endOfVisualRow = !compact || row % 2 == 1 || i == entries.size() - 1;
                if (endOfVisualRow && i < entries.size() - 1) {
                    GridBagConstraints sepC = new GridBagConstraints();
                    sepC.gridx = 0;
                    sepC.gridy = gridY + 1;
                    sepC.gridwidth = compact ? 4 : 2;
                    sepC.weightx = 1;
                    sepC.fill = GridBagConstraints.HORIZONTAL;
                    fields.add(hairlineSeparator(), sepC);
                }
                row++;
            }

            if (row == 0) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(10, 4, 10, 4);
                fields.add(Design.text("Tra cứu để xem thông tin chi tiết.", "subtitle"), c);
            }

            GridBagConstraints bottom = new GridBagConstraints();
            bottom.gridx = 0;
            bottom.gridy = compact ? ((row + 1) / 2) * 2 + 1 : row * 2 + 1;
            bottom.weighty = 1;
            fields.add(Box.createVerticalGlue(), bottom);

            Theme.apply(card);
            fields.setOpaque(false);
            fields.revalidate();
            fields.repaint();
        };

        source.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh.run(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh.run(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh.run(); }
        });
        refresh.run();
        return card;
    }

    private static JLabel createValueLabel(String name, String value) {
        boolean isStatus = name.toLowerCase().contains("trạng thái");
        WrappedValueLabel label = new WrappedValueLabel(value, isStatus);
        label.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            label.setFont(Theme.bold(13));
            label.setForeground(isStatus ? Tokens.semantic(value) : Tokens.text());
        });
        return label;
    }

    private static JComponent hairlineSeparator() {
        return new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(1, 1); }
            @Override public Dimension getMinimumSize() { return new Dimension(1, 1); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    g.setColor(Tokens.mix(Tokens.surface(), Tokens.border(), .45));
                    g.fillRect(0, 0, getWidth(), 1);
                } finally {
                    g.dispose();
                }
            }
        };
    }

    private static final class InfoRows extends JPanel implements Scrollable {
        InfoRows() { super(new GridBagLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return Theme.s(24); }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Theme.s(120); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class WrappedValueLabel extends JLabel {
        private final boolean statusPill;

        WrappedValueLabel(String text, boolean statusPill) {
            super(text == null ? "" : text);
            this.statusPill = statusPill;
            setOpaque(false);
            if (statusPill) {
                setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Font font = getFont() == null ? Theme.bold(13) : getFont();
            FontMetrics fm = getFontMetrics(font);
            String text = getText() == null ? "" : getText();
            if (statusPill) {
                Insets in = getInsets();
                return new Dimension(fm.stringWidth(text) + in.left + in.right + 4, fm.getHeight() + in.top + in.bottom);
            }
            Container parent = getParent();
            int avail = getWidth() > 40 ? getWidth()
                    : (parent != null && parent.getWidth() > 80 ? (int) (parent.getWidth() * 0.48) : 260);
            java.util.List<String> lines = wrapLines(text, fm, avail);
            int h = Math.max(1, lines.size()) * fm.getHeight();
            return new Dimension(Math.min(avail, fm.stringWidth(text) + 4), h);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Skin.graphics(graphics);
            try {
                Font font = getFont() == null ? Theme.bold(13) : getFont();
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics();
                String text = getText() == null ? "" : getText();
                if (statusPill) {
                    Color tone = Tokens.semantic(text);
                    Insets in = getInsets();
                    int pw = Math.min(getWidth(), fm.stringWidth(text) + in.left + in.right);
                    int ph = Math.min(getHeight(), fm.getHeight() + in.top + in.bottom);
                    g.setColor(Tokens.tone(tone));
                    g.fillRoundRect(0, 0, pw - 1, ph - 1, ph, ph);
                    g.setColor(Tokens.mix(Tokens.surface(), tone, .45));
                    g.drawRoundRect(0, 0, pw - 1, ph - 1, ph, ph);
                    g.setColor(tone);
                    g.drawString(text, in.left, (ph + fm.getAscent() - fm.getDescent()) / 2);
                    return;
                }
                g.setColor(getForeground() == null ? Tokens.text() : getForeground());
                int y = fm.getAscent();
                for (String line : wrapLines(text, fm, Math.max(80, getWidth()))) {
                    g.drawString(line, 0, y);
                    y += fm.getHeight();
                }
            } finally {
                g.dispose();
            }
        }

        private static java.util.List<String> wrapLines(String text, FontMetrics fm, int maxWidth) {
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

    public static JPanel profile(JTable source) {
        JTextArea data = new JTextArea();
        JPanel card = info(data, "Thông tin cá nhân");
        source.setVisible(false);
        card.add(source, BorderLayout.EAST);
        Runnable refresh = () -> {
            if (source.getModel().getRowCount() == 0) {
                data.setText("");
                return;
            }
            StringBuilder text = new StringBuilder();
            for (int col = 0; col < source.getModel().getColumnCount(); col++) {
                Object val = source.getModel().getValueAt(0, col);
                text.append(source.getModel().getColumnName(col))
                        .append(": ")
                        .append(val == null ? "—" : val)
                        .append('\n');
            }
            data.setText(text.toString());
        };
        source.getModel().addTableModelListener(e -> refresh.run());
        refresh.run();
        return card;
    }

    public static final class Gate extends JPanel {
        private final JLabel heading = Design.text("SẴN SÀNG SOÁT VÉ", "title");
        private final JLabel explanation = Design.text(
                "Chọn đúng phòng/suất, sau đó nhập mã vé hoặc đọc ảnh QR.", "subtitle");
        private String color = "info";
        private String iconKind = "ticket";

        private final JComponent iconBadge = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(Theme.s(54), Theme.s(54)); }
            @Override public Dimension getMinimumSize() { return getPreferredSize(); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    Color tone = toneColor();
                    int w = getWidth(), h = getHeight(), arc = Theme.s(12);
                    g.setColor(Tokens.mix(Tokens.surface(), tone, .20));
                    g.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                    g.setColor(Tokens.mix(Tokens.surface(), tone, .45));
                    g.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                    g.setColor(tone);
                    int iconSize = 32;
                    int ix = (w - Theme.s(iconSize)) / 2;
                    int iy = (h - Theme.s(iconSize)) / 2;
                    new Icons(iconKind, iconSize).paintIcon(this, g, ix, iy);
                } finally {
                    g.dispose();
                }
            }
        };

        private final JPanel banner = new JPanel(new BorderLayout(Theme.s(Theme.LG), 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    Color tone = toneColor();
                    int w = getWidth(), h = getHeight(), arc = Theme.s(14);
                    Shape shape = new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, arc, arc);
                    g.setColor(Tokens.tone(tone));
                    g.fill(shape);
                    Shape prev = g.getClip();
                    g.clip(shape);
                    g.setColor(new Color(255, 255, 255, Tokens.dark ? 16 : 140));
                    g.fillRect(0, 1, w, 1);
                    g.setClip(prev);
                    g.setColor(Tokens.mix(Tokens.surface(), tone, .45));
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(new RoundRectangle2D.Float(0.75f, 0.75f, w - 2.5f, h - 2.5f, arc, arc));
                } finally {
                    g.dispose();
                }
                super.paintComponent(graphics);
            }
        };

        public Gate(JTextArea source, JLabel status) {
            super(new BorderLayout(16, 16));
            setOpaque(false);
            banner.setOpaque(false);
            banner.setBorder(BorderFactory.createEmptyBorder(
                    Theme.s(Theme.LG), Theme.s(Theme.XL), Theme.s(Theme.LG), Theme.s(Theme.XL)));

            JPanel textStack = Design.stack(heading, explanation);
            textStack.setOpaque(false);
            heading.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                heading.setForeground(toneColor());
                heading.setFont(Theme.bold(24));
            });

            banner.add(iconBadge, BorderLayout.WEST);
            banner.add(textStack, BorderLayout.CENTER);
            banner.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                textStack.setOpaque(false);
                heading.setForeground(toneColor());
                heading.setFont(Theme.bold(24));
            });

            add(banner, BorderLayout.NORTH);
            add(info(source, "Thông tin vé", true), BorderLayout.CENTER);
            status.addPropertyChangeListener("text", e -> update(status.getText()));
            update(status.getText());
        }

        private Color toneColor() {
            return switch (color) {
                case "success" -> Theme.success();
                case "warning" -> Theme.warning();
                case "danger" -> Theme.danger();
                default -> Theme.info();
            };
        }

        private void update(String text) {
            String value = text == null ? "" : text;
            color = "info";
            String title = "SẴN SÀNG SOÁT VÉ";
            if (value.equals("Vé hợp lệ")) {
                title = "HỢP LỆ";
                color = "success";
            } else if (value.equals("Vé đã được sử dụng") || value.equals("Xác nhận vé thành công")) {
                title = "ĐÃ SỬ DỤNG";
                color = "info";
            } else if (value.equals("Vé chưa có thanh toán hợp lệ")) {
                title = "CHƯA THANH TOÁN";
                color = "warning";
            } else if (value.equals("Vé không đúng suất chiếu đang kiểm soát")) {
                title = "SAI SUẤT CHIẾU";
                color = "danger";
            } else if (value.equals("Vé không đúng phòng đang kiểm soát")) {
                title = "SAI PHÒNG CHIẾU";
                color = "danger";
            } else if (value.equals("Vé không tồn tại")) {
                title = "VÉ KHÔNG TỒN TẠI";
                color = "danger";
            } else if (value.contains("không hợp lệ") || value.contains("Ngoài thời gian") || value.contains("đã bị hủy")) {
                title = "CẦN KIỂM TRA LẠI";
                color = "danger";
            } else if (value.startsWith("Đang")) {
                title = "ĐANG KIỂM TRA";
            } else if (value.startsWith("Đã tra cứu")) {
                title = "ĐÃ TÌM THẤY VÉ";
            }
            iconKind = color.equals("success") ? "check" : color.equals("info") ? "ticket" : "alert";
            heading.setText(title);
            heading.setForeground(toneColor());
            heading.setFont(Theme.bold(24));
            explanation.setText(value);
            banner.repaint();
            iconBadge.repaint();
        }
    }
}
