package cinema.view.ui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableModel;

/** All charts and KPIs are projections of the original report table, never new queries. */
public final class ReportVisuals extends JPanel {
    private final JTable table;
    private final JLabel summary;
    private final String kind;
    private final JLabel[] captions = new JLabel[3];
    private final JLabel[] values = new JLabel[3];
    private final Bars chart = new Bars();

    private record Point(String name, BigDecimal value) {}

    public ReportVisuals(JTable table, JLabel summary, String kind) {
        super(new BorderLayout(12, 12));
        setOpaque(false);
        this.table = table;
        this.summary = summary;
        this.kind = kind;
        table.putClientProperty("cinema.stateLabel", summary);
        summary.addPropertyChangeListener("text", e -> {
            update();
            table.repaint();
        });

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 12));
        cards.setOpaque(false);
        for (int i = 0; i < 3; i++) {
            final int index = i;
            Design.Card card = new Design.Card(new BorderLayout(Theme.s(Theme.MD), 0));
            captions[i] = Design.text(" ", "subtitle");
            values[i] = Design.text("—", "metric");
            values[i].putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                values[index].setFont(Theme.number(26));
                values[index].setForeground(Tokens.text());
            });

            JComponent strip = new JComponent() {
                @Override public Dimension getPreferredSize() { return new Dimension(3, 36); }
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = Skin.graphics(graphics);
                    try {
                        g.setColor(stripColor(index, captions[index].getText()));
                        g.fillRoundRect(0, 2, 3, Math.max(8, getHeight() - 4), 3, 3);
                    } finally {
                        g.dispose();
                    }
                }
            };

            JPanel inner = new JPanel(new BorderLayout(0, 6));
            inner.setOpaque(false);
            inner.add(captions[i], BorderLayout.NORTH);
            inner.add(values[i], BorderLayout.CENTER);
            card.add(strip, BorderLayout.WEST);
            card.add(inner, BorderLayout.CENTER);
            cards.add(card);
        }

        Design.Card graph = new Design.Card(new BorderLayout());
        graph.add(chart, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(12, 12));
        top.setOpaque(false);
        top.add(cards, BorderLayout.NORTH);
        top.add(graph, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        Design.Card tableCard = new Design.Card(new BorderLayout());
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        table.getModel().addTableModelListener(e -> update());
        update();
    }

    private Color stripColor(int index, String caption) {
        String text = caption == null ? "" : caption;
        if (text.contains("Hết hàng")) return Tokens.danger();
        if (text.contains("Doanh thu")) return Tokens.accent();
        if (text.contains("Còn hàng") || text.contains("Đã sử dụng") || text.contains("Vé đã bán")) return Tokens.success();
        return switch (index) {
            case 1 -> Tokens.success();
            case 2 -> Tokens.accent();
            default -> Tokens.info();
        };
    }

    private static BigDecimal number(Object value) {
        return value instanceof BigDecimal amount ? amount
                : value instanceof Number number ? BigDecimal.valueOf(number.longValue()) : BigDecimal.ZERO;
    }

    private static String format(BigDecimal value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }

    private void metrics(String[] names, BigDecimal... data) {
        for (int i = 0; i < 3; i++) {
            captions[i].setText(names[i]);
            values[i].setText(format(data[i]));
        }
    }

    private void update() {
        TableModel data = table.getModel();
        String state = summary.getText() == null ? "" : summary.getText();
        if (data.getColumnCount() == 0 || state.startsWith("Tiêu chí") || state.startsWith("Chưa có")
                || state.startsWith("Đang") || state.startsWith("Không tải")) {
            for (int i = 0; i < 3; i++) {
                captions[i].setText(new String[]{"Kết quả theo bộ lọc", "Số lượng", "Giá trị"}[i]);
                values[i].setText("—");
            }
            chart.data(List.of(), "Chọn tiêu chí rồi bấm Thống kê.", "");
            return;
        }
        BigDecimal zero = BigDecimal.ZERO;
        List<Point> points = new ArrayList<>();
        switch (kind) {
            case "time" -> {
                if (data.getRowCount() == 0) return;
                metrics(new String[]{"Giao dịch thành công", "Vé đã bán", "Doanh thu · VND"},
                        number(data.getValueAt(0, 0)), number(data.getValueAt(0, 1)), number(data.getValueAt(0, 4)));
                points.add(new Point("Đã phát hành", number(data.getValueAt(0, 2))));
                points.add(new Point("Chờ phát hành", number(data.getValueAt(0, 3))));
                chart.data(points, "Phân bổ vé đã thanh toán", "vé");
            }
            case "film", "staff" -> {
                BigDecimal count = zero, total = zero;
                int countCol = 2;
                int moneyCol = kind.equals("film") ? 3 : 6;
                for (int i = 0; i < data.getRowCount(); i++) {
                    count = count.add(number(data.getValueAt(i, countCol)));
                    total = total.add(number(data.getValueAt(i, moneyCol)));
                    if (i < 6) {
                        points.add(new Point(String.valueOf(data.getValueAt(i, 1)), number(data.getValueAt(i, moneyCol))));
                    }
                }
                metrics(kind.equals("film")
                                ? new String[]{"Phim theo bộ lọc", "Vé đã phát hành", "Doanh thu · VND"}
                                : new String[]{"Nhân viên theo bộ lọc", "Giao dịch thành công", "Doanh thu · VND"},
                        BigDecimal.valueOf(data.getRowCount()), count, total);
                chart.data(points,
                        "Doanh thu theo " + (kind.equals("film") ? "phim" : "nhân viên")
                                + (data.getRowCount() > 6 ? " · 6 dòng đầu, bảng bên dưới đầy đủ" : ""),
                        "VND");
            }
            case "tickets" -> {
                if (data.getRowCount() == 0) return;
                metrics(new String[]{"Vé phát hành", "Chưa sử dụng", "Đã sử dụng"},
                        number(data.getValueAt(0, 0)), number(data.getValueAt(0, 1)), number(data.getValueAt(0, 2)));
                points.add(new Point("Chưa sử dụng", number(data.getValueAt(0, 1))));
                points.add(new Point("Đã sử dụng", number(data.getValueAt(0, 2))));
                chart.data(points, "Trạng thái vé hiện tại", "vé");
            }
            case "stock" -> {
                long available = 0, empty = 0;
                for (int i = 0; i < data.getRowCount(); i++) {
                    if (number(data.getValueAt(i, 4)).signum() > 0) available++;
                    else empty++;
                }
                metrics(new String[]{"Sản phẩm theo bộ lọc", "Còn hàng", "Hết hàng"},
                        BigDecimal.valueOf(data.getRowCount()), BigDecimal.valueOf(available), BigDecimal.valueOf(empty));
                points.add(new Point("Còn hàng", BigDecimal.valueOf(available)));
                points.add(new Point("Hết hàng", BigDecimal.valueOf(empty)));
                chart.data(points, "Tình trạng tồn kho · Không cộng lẫn đơn vị sản phẩm", "sản phẩm");
            }
            default -> {}
        }
        revalidate();
        repaint();
    }

    private static final class Bars extends JComponent {
        private List<Point> points = List.of();
        private String title = "", unit = "";

        Bars() {
            setPreferredSize(new Dimension(0, 108));
            setToolTipText("");
            getAccessibleContext();
        }

        void data(List<Point> points, String title, String unit) {
            this.points = List.copyOf(points);
            this.title = title;
            this.unit = unit;
            setPreferredSize(new Dimension(0, points.size() > 3 ? 160 : 108));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Skin.graphics(graphics);
            try {
                g.setFont(Theme.bold(13));
                g.setColor(Tokens.text());
                g.drawString(title, 0, 15);
                if (points.isEmpty()) {
                    g.setColor(Tokens.muted());
                    new Icons("chart", 20).paintIcon(this, g, 0, 44);
                    g.setFont(Theme.font(13));
                    g.drawString("Chưa có số liệu để vẽ biểu đồ.", 28, 59);
                    return;
                }

                int labelWidth = Math.min(200, Math.max(110, getWidth() / 4));
                int right = 136;
                int x = labelWidth;
                int plot = Math.max(80, getWidth() - labelWidth - right);
                int top = 32;
                int bottomSpace = 24;
                int spacing = Math.max(20, (getHeight() - top - bottomSpace) / points.size());
                int barHeight = Math.min(20, Math.max(12, spacing - 8));

                double max = points.stream().mapToDouble(p -> p.value().doubleValue()).max().orElse(0);
                if (max <= 0) max = 1;

                Stroke oldStroke = g.getStroke();
                g.setColor(Tokens.mix(Tokens.surface(), Tokens.border(), .65));
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f));
                for (int i = 0; i <= 4; i++) {
                    int gx = x + plot * i / 4;
                    g.drawLine(gx, top, gx, getHeight() - bottomSpace + 2);
                }
                g.setStroke(oldStroke);

                Color accent = Tokens.accent();
                Color accentEnd = Tokens.mix(accent, Tokens.surface(), .22);
                Color track = Tokens.tone(accent);

                for (int i = 0; i < points.size(); i++) {
                    Point point = points.get(i);
                    int y = top + i * spacing + Math.max(0, (spacing - barHeight) / 2);

                    g.setColor(Tokens.muted());
                    g.setFont(Theme.font(12));
                    FontMetrics nameFm = g.getFontMetrics();
                    String name = point.name();
                    while (name.length() > 3 && nameFm.stringWidth(name) > labelWidth - 14) {
                        name = name.substring(0, name.length() - 2) + "…";
                    }
                    int textY = y + (barHeight + nameFm.getAscent() - nameFm.getDescent()) / 2;
                    g.drawString(name, 0, textY);

                    g.setColor(track);
                    g.fillRoundRect(x, y, plot, barHeight, 6, 6);

                    int width = (int) Math.round(plot * point.value().doubleValue() / max);
                    if (width > 0) {
                        int fillW = Math.max(6, width);
                        g.setPaint(new GradientPaint(x, y, accent, x + fillW, y, accentEnd));
                        g.fillRoundRect(x, y, fillW, barHeight, 6, 6);
                        g.setPaint(null);
                    }

                    g.setFont(Theme.number(12));
                    g.setColor(Tokens.text());
                    String valStr = format(point.value());
                    int valW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x + plot + Math.max(14, right - valW - 4), textY);
                }

                g.setFont(Theme.number(11));
                g.setColor(Tokens.muted());
                g.drawString("0", x, getHeight() - 4);
                String end = format(BigDecimal.valueOf(Math.round(max))) + (unit.isBlank() ? "" : " " + unit);
                g.drawString(end, x + plot - g.getFontMetrics().stringWidth(end), getHeight() - 4);
            } finally {
                g.dispose();
            }
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (points.isEmpty()) return title;
            int spacing = Math.max(20, (getHeight() - 56) / points.size());
            int index = (event.getY() - 32) / spacing;
            if (index < 0 || index >= points.size()) return title;
            Point point = points.get(index);
            return point.name() + ": " + format(point.value()) + (unit.isBlank() ? "" : " " + unit);
        }
    }
}
