package cinema.view.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;

public final class Skin {
    private Skin() {}

    static Graphics2D graphics(Graphics g) {
        Graphics2D c = (Graphics2D) g.create();
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return c;
    }

    public static final class ControlBorder extends AbstractBorder {
        public Insets getBorderInsets(Component c) { return new Insets(9, 12, 9, 12); }
        public Insets getBorderInsets(Component c, Insets i) { i.set(9, 12, 9, 12); return i; }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D p = graphics(g);
            try {
                boolean invalid = c instanceof JComponent jc && Boolean.TRUE.equals(jc.getClientProperty("cinema.invalid"));
                p.setColor(invalid ? Tokens.danger() : c.hasFocus() ? Tokens.accent() : Tokens.border());
                p.setStroke(new BasicStroke(c.hasFocus() ? 1.5f : 1f));
                p.drawRoundRect(x + 1, y + 1, width - 3, height - 3, 10, 10);
            } finally {
                p.dispose();
            }
        }
    }

    public static class ButtonUI extends BasicButtonUI {
        public static ComponentUI createUI(JComponent c) { return new ButtonUI(); }

        @Override protected void installDefaults(AbstractButton b) {
            super.installDefaults(b);
            b.setOpaque(false);
            b.setBorder(new ControlBorder());
            b.setRolloverEnabled(true);
            b.setIconTextGap(8);
        }

        @Override public void update(Graphics g, JComponent c) { paint(g, c); }

        @Override public void paint(Graphics graphics, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g = graphics(graphics);
            try {
                Object seat = b.getClientProperty("cinema.seatState");
                String role = String.valueOf(b.getClientProperty("cinema.role"));
                Color accent = seat == null ? ("danger".equals(role) ? Tokens.danger() : Tokens.accent())
                        : Tokens.semantic(seat.toString());
                boolean primary = "primary".equals(role), chosen = b.getModel().isSelected();
                Color bg = primary ? accent : (seat != null || chosen ? Tokens.tone(accent) : Tokens.raised());
                if (b.getModel().isRollover() && b.isEnabled()) bg = Tokens.mix(bg, accent, .12);
                if (b.getModel().isPressed()) bg = Tokens.mix(bg, accent, .24);
                int w = c.getWidth(), h = c.getHeight();
                g.setColor(bg);
                g.fillRoundRect(1, 1, w - 2, h - 2, 10, 10);
                if (primary) {
                    Shape prev = g.getClip();
                    g.setClip(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));
                    g.setColor(new Color(255, 255, 255, Tokens.dark ? 45 : 65));
                    g.drawLine(6, 2, w - 7, 2);
                    g.setClip(prev);
                }
                g.setColor(b.hasFocus() ? Tokens.accent()
                        : (seat != null || chosen || "danger".equals(role) ? accent : Tokens.border()));
                g.setStroke(new BasicStroke(b.hasFocus() || chosen ? 1.5f : 1f));
                g.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
                b.setForeground(!b.isEnabled() ? Tokens.muted()
                        : primary ? Tokens.accentInk()
                        : "danger".equals(role) ? Tokens.danger() : Tokens.text());
                super.paint(graphics, c);
            } finally {
                g.dispose();
            }
        }

        @Override protected void paintButtonPressed(Graphics g, AbstractButton b) {}
        @Override protected void paintFocus(Graphics g, AbstractButton b, Rectangle v, Rectangle t, Rectangle i) {}
        @Override protected void paintText(Graphics g, AbstractButton b, Rectangle rect, String text) {
            g.setColor(b.getForeground());
            g.setFont(b.getFont());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, b.getDisplayedMnemonicIndex(),
                    rect.x, rect.y + g.getFontMetrics().getAscent());
        }
    }

    public static class FieldUI extends BasicTextFieldUI {
        public static ComponentUI createUI(JComponent c) { return new FieldUI(); }
        @Override protected void paintBackground(Graphics graphics) {
            Graphics2D g = graphics(graphics);
            try {
                g.setColor(Tokens.raised());
                g.fillRoundRect(0, 0, getComponent().getWidth(), getComponent().getHeight(), 10, 10);
            } finally {
                g.dispose();
            }
        }
    }

    public static class PasswordUI extends BasicPasswordFieldUI {
        public static ComponentUI createUI(JComponent c) { return new PasswordUI(); }
        @Override protected void paintBackground(Graphics graphics) {
            if (Boolean.TRUE.equals(getComponent().getClientProperty("cinema.customBorder"))) return;
            Graphics2D g = graphics(graphics);
            try {
                g.setColor(Tokens.raised());
                g.fillRoundRect(0, 0, getComponent().getWidth(), getComponent().getHeight(), 10, 10);
            } finally {
                g.dispose();
            }
        }
    }

    public static class ScrollUI extends BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) { return new ScrollUI(); }
        @Override protected JButton createDecreaseButton(int orientation) { return zero(); }
        @Override protected JButton createIncreaseButton(int orientation) { return zero(); }
        private JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(Tokens.surface());
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        @Override protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
            Graphics2D g = graphics(graphics);
            try {
                g.setColor(Tokens.border());
                int arc = Math.min(r.width, r.height);
                g.fillRoundRect(r.x + 2, r.y + 2, Math.max(4, r.width - 4), Math.max(4, r.height - 4), arc, arc);
            } finally {
                g.dispose();
            }
        }
    }

    public static class TabsUI extends BasicTabbedPaneUI {
        public static ComponentUI createUI(JComponent c) { return new TabsUI(); }
        @Override protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(10, 16, 10, 16);
            contentBorderInsets = new Insets(12, 0, 0, 0);
        }
        @Override protected void paintTabBackground(Graphics graphics, int placement, int index, int x, int y, int w, int h, boolean selected) {
            Graphics2D g = graphics(graphics);
            try {
                g.setColor(selected ? Tokens.tone(Tokens.accent()) : Tokens.bg());
                g.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 10, 10);
            } finally {
                g.dispose();
            }
        }
        @Override protected void paintTabBorder(Graphics g, int p, int i, int x, int y, int w, int h, boolean selected) {}
        @Override protected void paintContentBorder(Graphics g, int p, int i) {}
        @Override protected void paintFocusIndicator(Graphics g, int p, Rectangle[] r, int i, Rectangle a, Rectangle b, boolean selected) {
            if (selected && tabPane.hasFocus()) {
                g.setColor(Tokens.accent());
                g.drawRoundRect(r[i].x + 2, r[i].y + 2, r[i].width - 4, r[i].height - 4, 10, 10);
            }
        }
    }

    public static class TableUI extends com.formdev.flatlaf.ui.FlatTableUI {
        public static ComponentUI createUI(JComponent c) { return new TableUI(); }
        @Override public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            if (table.getRowCount() == 0) {
                Object state = table.getClientProperty("cinema.stateLabel");
                String message = state instanceof JLabel label ? label.getText() : "Chưa có dữ liệu";
                empty(g, c, message);
            }
        }
    }

    private static void empty(Graphics graphics, JComponent c, String state) {
        Graphics2D g = graphics(graphics);
        try {
            if (c.getHeight() < 140) {
                g.setColor(Tokens.muted());
                new Icons(state != null && state.startsWith("Đang") ? "calendar" : "box", 20).paintIcon(c, g, 12, 16);
                g.setFont(Tokens.bold(12));
                g.drawString(state != null && state.startsWith("Đang") ? "Đang tải dữ liệu…" : "Chưa có kết quả phù hợp", 44, 24);
                g.setFont(Tokens.font(12));
                g.drawString("Thử lại với bộ lọc khác.", 44, 44);
                return;
            }
            int x = c.getWidth() / 2, y = Math.max(80, Math.min(c.getHeight() / 2, 180));
            g.setColor(Tokens.muted());
            new Icons(state != null && state.startsWith("Đang") ? "calendar" : "box", 32).paintIcon(c, g, x - 16, y - 28);
            g.setFont(Tokens.bold(16));
            String title = state != null && state.startsWith("Đang") ? "Đang tải dữ liệu"
                    : state != null && (state.startsWith("Không thể") || state.startsWith("Không tải"))
                    ? "Chưa tải được dữ liệu" : "Chưa có kết quả";
            g.drawString(title, x - g.getFontMetrics().stringWidth(title) / 2, y + 32);
            g.setFont(Tokens.font(12));
            String hint = state == null || state.isBlank() ? "Chọn bộ lọc hoặc thêm dữ liệu để bắt đầu." : state;
            int max = 80;
            if (hint.length() > max) hint = hint.substring(0, max) + "…";
            g.drawString(hint, x - g.getFontMetrics().stringWidth(hint) / 2, y + 58);
        } finally {
            g.dispose();
        }
    }

    public static class Header extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setFont(Tokens.bold(12));
            setForeground(Tokens.muted());
            setBackground(Tokens.raised());
            boolean numeric = isNumericColumn(table, column, value);
            setHorizontalAlignment(numeric ? RIGHT : LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tokens.border()),
                    BorderFactory.createEmptyBorder(Theme.s(Theme.MD), Theme.s(Theme.MD), Theme.s(Theme.MD), Theme.s(Theme.MD))));
            return this;
        }

        private static boolean isNumericColumn(JTable table, int viewColumn, Object headerValue) {
            if (table != null && table.getModel() != null) {
                int modelCol = table.convertColumnIndexToModel(viewColumn);
                if (modelCol >= 0 && modelCol < table.getModel().getColumnCount()) {
                    Class<?> colClass = table.getModel().getColumnClass(modelCol);
                    if (Number.class.isAssignableFrom(colClass)) return true;
                }
                for (int r = 0; r < Math.min(table.getRowCount(), 5); r++) {
                    Object cell = table.getValueAt(r, viewColumn);
                    if (cell instanceof Number) return true;
                }
            }
            String h = headerValue == null ? "" : headerValue.toString().toLowerCase();
            return h.contains("tiền") || h.contains("giá") || h.equals("sl")
                    || h.contains("số lượng") || h.contains("doanh thu") || h.contains("tồn")
                    || h.contains("phút") || h.contains("tuổi") || h.equals("mã suất") || h.equals("suất");
        }
    }

    public static class Cell extends DefaultTableCellRenderer {
        private Color pillColor;

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            pillColor = null;
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            boolean numeric = value instanceof Number;
            setFont(numeric ? Tokens.number() : Tokens.font(14));
            setHorizontalAlignment(numeric ? RIGHT : LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tokens.mix(Tokens.surface(), Tokens.border(), 0.45)),
                    BorderFactory.createEmptyBorder(Theme.s(Theme.SM), Theme.s(Theme.MD), Theme.s(Theme.SM), Theme.s(Theme.MD))));
            setBackground(selected ? Tokens.tone(Tokens.accent())
                    : row % 2 == 0 ? Tokens.surface() : Tokens.mix(Tokens.surface(), Tokens.raised(), .45));
            setForeground(Tokens.text());
            if (value instanceof java.math.BigDecimal money) {
                setText(java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN")).format(money));
            }
            if (value instanceof java.time.LocalDateTime time) {
                setText(time.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
            if (value instanceof java.time.LocalDate date) {
                setText(date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            if (Boolean.TRUE.equals(table.getClientProperty("cinema.stockTable")) && !selected
                    && table.getModel().getColumnCount() > 5
                    && Integer.valueOf(0).equals(table.getModel().getValueAt(table.convertRowIndexToModel(row), 5))) {
                setBackground(Tokens.tone(Tokens.danger()));
            }
            String s = value == null ? "" : value.toString();
            setToolTipText(s.isBlank() ? null : s);
            if (s.matches("^(Đang hoạt động|Sẵn sàng sử dụng|Đã thanh toán|Chờ thanh toán|Đang thanh toán|Đã bán|Đang giữ|Đã hủy|Hết hạn|Chưa sử dụng|Đã sử dụng|Bảo trì|Ngừng chiếu|Đang chiếu|Sắp chiếu|Đã kết thúc|Còn hàng|Hết hàng|Đang kinh doanh|Ngừng kinh doanh|Không khả dụng)$")) {
                pillColor = Tokens.semantic(s);
                setForeground(pillColor);
                setFont(Tokens.bold(12));
                setText(s);
            }
            return this;
        }

        @Override protected void paintComponent(Graphics graphics) {
            if (pillColor == null) {
                super.paintComponent(graphics);
                return;
            }
            Graphics2D g = graphics(graphics);
            try {
                int w = getWidth(), h = getHeight();
                g.setColor(getBackground());
                g.fillRect(0, 0, w, h);
                FontMetrics fm = g.getFontMetrics(getFont());
                int padL = Theme.s(Theme.MD);
                int pillH = Math.min(h - Theme.s(8), Theme.s(26));
                int pillY = (h - pillH) / 2;
                int dotSize = Theme.s(6);
                int innerPad = Theme.s(10);
                int textMaxW = Math.max(20, w - padL - Theme.s(6) - innerPad * 2 - dotSize - Theme.s(6));
                String txt = getText() == null ? "" : getText();
                if (fm.stringWidth(txt) > textMaxW) {
                    while (txt.length() > 1 && fm.stringWidth(txt + "…") > textMaxW) {
                        txt = txt.substring(0, txt.length() - 1);
                    }
                    txt += "…";
                }
                int pillW = Math.min(w - padL - Theme.s(6), innerPad * 2 + dotSize + Theme.s(6) + fm.stringWidth(txt));
                g.setColor(Tokens.tone(pillColor));
                g.fillRoundRect(padL, pillY, pillW, pillH, pillH, pillH);
                g.setColor(Tokens.mix(Tokens.surface(), pillColor, Tokens.dark ? 0.42 : 0.28));
                g.drawRoundRect(padL, pillY, pillW - 1, pillH - 1, pillH, pillH);
                g.setColor(pillColor);
                g.fillOval(padL + innerPad, pillY + (pillH - dotSize) / 2, dotSize, dotSize);
                g.setFont(getFont());
                int textY = pillY + (pillH + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(txt, padL + innerPad + dotSize + Theme.s(6), textY);
            } finally {
                g.dispose();
            }
        }
    }
}
