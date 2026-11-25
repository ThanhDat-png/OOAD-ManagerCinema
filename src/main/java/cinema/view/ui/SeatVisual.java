package cinema.view.ui;

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

/** Shared renderer for actual seats; no availability or reservation decisions live here. */
public final class SeatVisual {
    private SeatVisual() {}

    public static void install(AbstractButton button, String state, String type, boolean own) {
        button.putClientProperty("cinema.seatState", state);
        button.putClientProperty("cinema.seatType", type);
        button.putClientProperty("cinema.seatOwn", own);
        button.putClientProperty("cinema.noIcon", true);
        button.setUI(new SeatUI());
        button.setPreferredSize(new Dimension(Theme.s(54), Theme.s(46)));
        button.setMinimumSize(new Dimension(Theme.s(54), Theme.s(46)));
        button.setMaximumSize(new Dimension(Theme.s(54), Theme.s(46)));
        button.setBorder(BorderFactory.createEmptyBorder(Theme.s(2), Theme.s(2), Theme.s(2), Theme.s(2)));
        button.getAccessibleContext().setAccessibleName(button.getText() + " · " + type + " · " + state + (own ? " · Giao dịch này" : ""));
    }

    public static void state(AbstractButton button, String state) {
        button.putClientProperty("cinema.seatState", state);
        button.setUI(new SeatUI());
        button.repaint();
    }

    public static JPanel legend() {
        return legend("Trống", "Đang giữ", "Đã bán", "Không khả dụng", "Giao dịch này");
    }

    public static JPanel legend(String... states) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.s(6), Theme.s(4))) {
            @Override public Dimension getMaximumSize() {
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
        panel.setOpaque(false);
        for (String state : states) {
            JLabel chip = new JLabel(state, new Mark(state), JLabel.LEADING) {
                @Override public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    return new Dimension(d.width + Theme.s(6), d.height);
                }
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = Skin.graphics(graphics);
                    try {
                        int w = getWidth(), h = getHeight();
                        g.setColor(Tokens.raised());
                        g.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                        g.setColor(Tokens.border());
                        g.drawRoundRect(0, 0, w - 2, h - 2, h, h);
                    } finally {
                        g.dispose();
                    }
                    super.paintComponent(graphics);
                }
            };
            chip.setOpaque(false);
            chip.setFont(Theme.font(12));
            chip.setForeground(Tokens.text());
            chip.setIconTextGap(Theme.s(6));
            chip.setBorder(BorderFactory.createEmptyBorder(Theme.s(4), Theme.s(10), Theme.s(4), Theme.s(12)));
            chip.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                chip.setFont(Theme.font(12));
                chip.setForeground(Tokens.text());
            });
            panel.add(chip);
        }
        return panel;
    }

    public static JComponent screen() {
        return new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(Theme.s(260), Theme.s(42)); }
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, Theme.s(42)); }

            protected void paintComponent(Graphics graphics) {
                Graphics2D g = Skin.graphics(graphics);
                try {
                    int w = getWidth(), h = getHeight();
                    int left = Theme.s(20), right = Math.max(left + 40, w - Theme.s(20));
                    int midX = w / 2;
                    int yEdge = Theme.s(18), yCtrl = Theme.s(4);

                    Color glow = Tokens.accent();
                    for (int i = 3; i >= 1; i--) {
                        g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), Tokens.dark ? 22 / i : 16 / i));
                        g.setStroke(new BasicStroke(Theme.s(i * 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.draw(new QuadCurve2D.Double(left, yEdge + 2, midX, yCtrl + 2, right, yEdge + 2));
                    }

                    g.setColor(Tokens.mix(Tokens.border(), Tokens.accent(), 0.7));
                    g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new QuadCurve2D.Double(left, yEdge, midX, yCtrl, right, yEdge));

                    g.setColor(Tokens.muted());
                    g.setFont(Theme.bold(11));
                    String text = "MÀN HÌNH";
                    g.drawString(text, (w - g.getFontMetrics().stringWidth(text)) / 2, h - Theme.s(6));
                } finally {
                    g.dispose();
                }
            }
        };
    }

    private static void symbol(Graphics2D g, String state, int x, int y) {
        if (state.equals("Đã bán")) {
            g.drawLine(x, y, x + 7, y + 7);
            g.drawLine(x + 7, y, x, y + 7);
        } else if (state.equals("Không khả dụng")) {
            g.drawLine(x, y + 4, x + 7, y + 4);
        } else if (state.equals("Đang giữ")) {
            g.drawOval(x - 1, y - 1, 9, 9);
            g.drawLine(x + 3, y + 1, x + 3, y + 4);
            g.drawLine(x + 3, y + 4, x + 6, y + 5);
        } else if (state.equals("Giao dịch này")) {
            g.drawLine(x, y + 4, x + 3, y + 7);
            g.drawLine(x + 3, y + 7, x + 8, y);
        } else {
            g.drawOval(x, y, 7, 7);
        }
    }

    private static final class Mark implements Icon {
        private final String state;
        Mark(String state) { this.state = state; }
        public int getIconWidth() { return Theme.s(16); }
        public int getIconHeight() { return Theme.s(16); }

        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = Skin.graphics(graphics);
            try {
                g.translate(x + 3, y + 3);
                g.setColor(state.equals("Giao dịch này") ? Theme.accent() : Theme.semantic(state));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                symbol(g, state, 1, 1);
            } finally {
                g.dispose();
            }
        }
    }

    public static final class SeatUI extends BasicButtonUI {
        @Override public void update(Graphics g, JComponent c) { paint(g, c); }

        @Override public void paint(Graphics graphics, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            Graphics2D g = Skin.graphics(graphics);
            try {
                String state = String.valueOf(button.getClientProperty("cinema.seatState"));
                String type = String.valueOf(button.getClientProperty("cinema.seatType"));
                boolean own = Boolean.TRUE.equals(button.getClientProperty("cinema.seatOwn"));
                boolean selected = button.isSelected();
                boolean current = (own && state.equals("Đang giữ")) || selected;
                boolean unavailable = state.equals("Không khả dụng");
                boolean available = state.equals("Trống") && !current;

                int w = component.getWidth(), h = component.getHeight();
                Color semantic = current ? Theme.accent() : Theme.semantic(state);

                Color fill;
                Color stroke;
                Color titleColor;
                Color captionColor;
                if (current) {
                    fill = Theme.accent();
                    stroke = Theme.accent();
                    titleColor = Theme.accentInk();
                    captionColor = Theme.accentInk();
                } else if (unavailable) {
                    fill = Theme.bg();
                    stroke = Theme.mix(Theme.border(), Theme.bg(), 0.55);
                    titleColor = Theme.muted();
                    captionColor = Theme.muted();
                } else if (available) {
                    fill = Theme.raised();
                    stroke = button.getModel().isRollover() ? Theme.accent() : Theme.border();
                    titleColor = Theme.text();
                    captionColor = Theme.success();
                } else {
                    fill = Theme.tone(semantic);
                    stroke = Theme.mix(Theme.surface(), semantic, Theme.dark ? 0.55 : 0.45);
                    titleColor = Theme.text();
                    captionColor = semantic;
                }

                // Compact rounded web seat block
                int seatX = 2, seatY = 2, seatW = w - 4, seatH = h - 4;
                int r = Theme.s(8);
                g.setColor(fill);
                g.fillRoundRect(seatX, seatY, seatW, seatH, r, r);

                g.setColor(stroke);
                g.setStroke(new BasicStroke(current || button.hasFocus() ? 1.8f : 1.1f));
                g.drawRoundRect(seatX, seatY, seatW - 1, seatH - 1, r, r);

                // VIP / Couple trim at bottom
                String lowerType = type.toLowerCase();
                if (lowerType.contains("vip")) {
                    g.setColor(current ? Theme.accentInk() : Theme.warning());
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(seatX + 10, seatY + seatH - 5, seatX + seatW - 10, seatY + seatH - 5);
                } else if (lowerType.contains("đôi")) {
                    g.setColor(current ? Theme.accentInk() : Theme.info());
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(seatX + 10, seatY + seatH - 5, seatX + seatW - 10, seatY + seatH - 5);
                }

                if (unavailable) {
                    g.setColor(new Color(Theme.muted().getRed(), Theme.muted().getGreen(), Theme.muted().getBlue(), 75));
                    g.setStroke(new BasicStroke(1.2f));
                    g.drawLine(seatX + 6, seatY + seatH - 6, seatX + seatW - 6, seatY + 6);
                }

                // State symbol corner mark (kept clean in top-right)
                if (!available) {
                    g.setColor(current ? Theme.accentInk() : captionColor);
                    g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    symbol(g, current ? "Giao dịch này" : state, w - 13, seatY + 4);
                }

                // Centered seat code
                String raw = button.getText() == null ? "" : button.getText();
                String title = raw.split(" · ")[0];
                g.setFont(Theme.bold(13));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(titleColor);
                g.drawString(title, (w - fm.stringWidth(title)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2 - 1);

                if (button.hasFocus()) {
                    g.setColor(Theme.accent());
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawRoundRect(0, 0, w - 1, h - 1, r + 2, r + 2);
                }
            } finally {
                g.dispose();
            }
        }
    }
}
