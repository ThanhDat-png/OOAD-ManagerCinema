package cinema.view.ui;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;

/** Original, stroke-based icon family. No external icon or image dependency. */
public final class Icons implements Icon {
    private final String kind;
    private final int size;

    public Icons(String kind) { this(kind, 18); }
    public Icons(String kind, int size) { this.kind = kind; this.size = size; }

    public int getIconWidth() { return Theme.s(size); }
    public int getIconHeight() { return Theme.s(size); }

    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(x, y);
            g.scale(getIconWidth() / 20.0, getIconHeight() / 20.0);
            g.setColor(c.isEnabled() ? c.getForeground() : Tokens.muted());
            g.setStroke(new BasicStroke(1.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case "search" -> {
                    g.drawOval(3, 3, 10, 10);
                    g.drawLine(12, 12, 17, 17);
                }
                case "film" -> {
                    g.drawRoundRect(2, 3, 16, 14, 2, 2);
                    g.drawLine(6, 3, 6, 17);
                    g.drawLine(14, 3, 14, 17);
                    g.drawLine(2, 8, 6, 8);
                    g.drawLine(14, 8, 18, 8);
                    g.drawLine(2, 12, 6, 12);
                    g.drawLine(14, 12, 18, 12);
                }
                case "people" -> {
                    g.drawOval(7, 3, 6, 6);
                    g.drawArc(3, 10, 14, 10, 0, 180);
                    g.drawOval(2, 6, 3, 3);
                    g.drawOval(15, 6, 3, 3);
                }
                case "calendar" -> {
                    g.drawRoundRect(2, 4, 16, 14, 3, 3);
                    g.drawLine(2, 8, 18, 8);
                    g.drawLine(6, 2, 6, 6);
                    g.drawLine(14, 2, 14, 6);
                    g.drawLine(6, 12, 8, 12);
                    g.drawLine(12, 12, 14, 12);
                }
                case "clock" -> {
                    g.drawOval(2, 2, 16, 16);
                    g.drawLine(10, 5, 10, 10);
                    g.drawLine(10, 10, 14, 12);
                }
                case "seat" -> {
                    g.drawRoundRect(5, 2, 10, 11, 3, 3);
                    g.drawRoundRect(2, 9, 16, 7, 2, 2);
                    g.drawLine(4, 16, 4, 18);
                    g.drawLine(16, 16, 16, 18);
                }
                case "chart" -> {
                    g.drawLine(3, 3, 3, 17);
                    g.drawLine(3, 17, 17, 17);
                    g.drawLine(7, 14, 7, 10);
                    g.drawLine(11, 14, 11, 5);
                    g.drawLine(15, 14, 15, 8);
                }
                case "ticket" -> {
                    g.drawRoundRect(2, 4, 16, 12, 3, 3);
                    g.drawLine(13, 5, 13, 7);
                    g.drawLine(13, 10, 13, 12);
                    g.drawLine(13, 14, 13, 15);
                }
                case "scan" -> {
                    g.drawPolyline(new int[]{3, 3, 7}, new int[]{7, 3, 3}, 3);
                    g.drawPolyline(new int[]{13, 17, 17}, new int[]{3, 3, 7}, 3);
                    g.drawPolyline(new int[]{17, 17, 13}, new int[]{13, 17, 17}, 3);
                    g.drawPolyline(new int[]{7, 3, 3}, new int[]{17, 17, 13}, 3);
                    g.drawLine(5, 10, 15, 10);
                }
                case "login" -> {
                    Path2D door = new Path2D.Float();
                    door.moveTo(10, 3);
                    door.lineTo(15, 3);
                    door.quadTo(17, 3, 17, 5);
                    door.lineTo(17, 15);
                    door.quadTo(17, 17, 15, 17);
                    door.lineTo(10, 17);
                    g.draw(door);
                    g.drawLine(3, 10, 13, 10);
                    g.drawLine(9, 6, 13, 10);
                    g.drawLine(9, 14, 13, 10);
                }
                case "check" -> {
                    g.drawLine(3, 10, 8, 15);
                    g.drawLine(8, 15, 17, 5);
                }
                case "sun" -> {
                    g.drawOval(6, 6, 8, 8);
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4;
                        g.drawLine((int) Math.round(10 + 6.5 * Math.cos(a)), (int) Math.round(10 + 6.5 * Math.sin(a)),
                                (int) Math.round(10 + 8.5 * Math.cos(a)), (int) Math.round(10 + 8.5 * Math.sin(a)));
                    }
                }
                case "moon" -> {
                    Path2D crescent = new Path2D.Double();
                    crescent.moveTo(15.5, 13.0);
                    crescent.curveTo(12.2, 13.8, 8.6, 11.4, 8.4, 7.6);
                    crescent.curveTo(8.3, 5.6, 9.2, 3.8, 10.7, 2.7);
                    crescent.curveTo(6.2, 3.0, 2.8, 6.8, 3.1, 11.3);
                    crescent.curveTo(3.4, 15.8, 7.4, 18.6, 11.9, 17.8);
                    crescent.curveTo(13.8, 17.4, 15.4, 15.6, 15.5, 13.0);
                    crescent.closePath();
                    g.draw(crescent);
                }
                case "exit" -> {
                    g.drawRoundRect(2, 2, 10, 16, 2, 2);
                    g.drawLine(8, 10, 18, 10);
                    g.drawLine(14, 6, 18, 10);
                    g.drawLine(14, 14, 18, 10);
                }
                case "plus" -> {
                    g.drawLine(10, 4, 10, 16);
                    g.drawLine(4, 10, 16, 10);
                }
                case "alert" -> {
                    g.drawRoundRect(2, 2, 16, 16, 5, 5);
                    g.drawLine(10, 6, 10, 11);
                    g.drawLine(10, 14, 10, 14);
                }
                case "eye" -> {
                    g.drawArc(2, 5, 16, 10, 0, 180);
                    g.drawArc(2, 5, 16, 10, 180, 180);
                    g.drawOval(8, 8, 4, 4);
                }
                case "copy" -> {
                    g.drawRoundRect(6, 6, 10, 11, 2, 2);
                    g.drawPolyline(new int[]{4, 4, 12}, new int[]{14, 3, 3}, 3);
                }
                case "arrow" -> {
                    g.drawLine(3, 10, 17, 10);
                    g.drawLine(12, 5, 17, 10);
                    g.drawLine(12, 15, 17, 10);
                }
                case "box" -> {
                    g.drawRoundRect(3, 3, 14, 14, 3, 3);
                    g.drawLine(3, 8, 17, 8);
                    g.drawLine(8, 3, 8, 8);
                }
                default -> {
                    g.drawRoundRect(3, 3, 5, 5, 1, 1);
                    g.drawRoundRect(12, 3, 5, 5, 1, 1);
                    g.drawRoundRect(3, 12, 5, 5, 1, 1);
                    g.drawRoundRect(12, 12, 5, 5, 1, 1);
                }
            }
        } finally {
            g.dispose();
        }
    }

    public static String forTitle(String text) {
        String s = text == null ? "" : text.toLowerCase();
        if (s.contains("đăng nhập")) return "login";
        if (s.contains("đăng xuất")) return "exit";
        if (s.contains("kiểm tra vé") || s.contains("soát vé")) return "scan";
        if (s.contains("phim") || s.contains("poster")) return "film";
        if (s.contains("nhân viên") || s.contains("cá nhân") || s.contains("hồ sơ")) return "people";
        if (s.contains("lịch") || s.contains("suất")) return "calendar";
        if (s.contains("phòng") || s.contains("ghế")) return "seat";
        if (s.contains("báo cáo") || s.contains("thống kê")) return "chart";
        if (s.contains("f&b") || s.contains("sản phẩm") || s.contains("bắp")) return "box";
        if (s.contains("vé") || s.contains("giao dịch")) return "ticket";
        if (s.contains("tìm") || s.contains("tra cứu")) return "search";
        if (s.contains("thêm") || s.contains("tạo")) return "plus";
        return "box";
    }
}
