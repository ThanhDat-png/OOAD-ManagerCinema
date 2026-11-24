package cinema.view.ui;

import cinema.controller.film.PosterStore;
import cinema.model.Phim;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

public final class PosterCells implements TableCellRenderer {
    private static final int THUMB_W = 60;
    private static final int THUMB_H = 84;
    private static final int THUMB_ARC = 8;

    private final JTable table;
    private final PosterStore store;
    private final Map<String, Phim> films = new HashMap<>();
    private final Map<String, BufferedImage> images = new HashMap<>();

    public PosterCells(JTable table, PosterStore store) {
        this.table = table;
        this.store = store;
        table.putClientProperty("cinema.posterTable", true);
        table.getColumnModel().getColumn(1).setCellRenderer(this);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
    }

    public void update(List<Phim> data) {
        films.clear();
        for (Phim film : data) {
            films.put(film.maPhim(), film);
            String key = film.poster();
            if (key != null && !images.containsKey(key)) {
                images.put(key, null);
                new SwingWorker<BufferedImage, Void>() {
                    @Override
                    protected BufferedImage doInBackground() {
                        try {
                            return store.load(key);
                        } catch (RuntimeException e) {
                            return null;
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            BufferedImage image = get();
                            if (image != null) {
                                images.put(key, scaleToCover(image, Theme.s(THUMB_W), Theme.s(THUMB_H)));
                            }
                        } catch (Exception ignored) {
                        }
                        table.repaint();
                    }
                }.execute();
            }
        }
        table.repaint();
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean selected, boolean focused, int row, int column) {
        Phim film = films.get(String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(row), 0)));
        Color rowBg = selected
                ? Tokens.tone(Tokens.accent())
                : (row % 2 == 0 ? Tokens.surface() : Tokens.mix(Tokens.surface(), Tokens.raised(), .6));

        JPanel cell = new JPanel(new BorderLayout(Theme.s(Theme.MD), 0));
        cell.setOpaque(true);
        cell.setBackground(rowBg);
        cell.setBorder(BorderFactory.createEmptyBorder(
                Theme.s(Theme.SM), Theme.s(Theme.MD), Theme.s(Theme.SM), Theme.s(Theme.MD)));

        BufferedImage thumb = film == null ? null : images.get(film.poster());
        JLabel art = new JLabel(thumb == null ? new Placeholder() : new RoundedPosterIcon(thumb));
        art.setPreferredSize(new Dimension(Theme.s(THUMB_W), Theme.s(THUMB_H)));
        cell.add(art, BorderLayout.WEST);

        String title = value == null ? "" : value.toString();
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.bold(14));
        titleLabel.setForeground(selected ? Tokens.accent() : Tokens.text());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String meta = film == null ? "" : film.dinhDangChieu() + " · " + film.thoiLuong() + " phút";
        JLabel metaLabel = new JLabel(meta);
        metaLabel.setFont(Theme.font(12));
        metaLabel.setForeground(Tokens.muted());
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(metaLabel);
        text.add(Box.createVerticalGlue());

        cell.add(text, BorderLayout.CENTER);
        cell.setToolTipText(title.isBlank() ? null : title);
        return cell;
    }

    private static BufferedImage scaleToCover(BufferedImage source, int targetW, int targetH) {
        int w = Math.max(1, targetW);
        int h = Math.max(1, targetH);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double scale = Math.max((double) w / source.getWidth(), (double) h / source.getHeight());
            int drawW = (int) Math.round(source.getWidth() * scale);
            int drawH = (int) Math.round(source.getHeight() * scale);
            g.drawImage(source, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static final class RoundedPosterIcon implements Icon {
        private final BufferedImage image;

        RoundedPosterIcon(BufferedImage image) {
            this.image = image;
        }

        @Override
        public int getIconWidth() {
            return Theme.s(THUMB_W);
        }

        @Override
        public int getIconHeight() {
            return Theme.s(THUMB_H);
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = Skin.graphics(graphics);
            try {
                int w = getIconWidth();
                int h = getIconHeight();
                int arc = Theme.s(THUMB_ARC);
                Shape prevClip = g.getClip();
                g.clip(new RoundRectangle2D.Float(x, y, w - 1f, h - 1f, arc, arc));
                g.drawImage(image, x, y, w, h, null);
                g.setClip(prevClip);
                g.setColor(Tokens.border());
                g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
            } finally {
                g.dispose();
            }
        }
    }

    private static final class Placeholder implements Icon {
        @Override
        public int getIconWidth() {
            return Theme.s(THUMB_W);
        }

        @Override
        public int getIconHeight() {
            return Theme.s(THUMB_H);
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = Skin.graphics(graphics);
            try {
                int w = getIconWidth();
                int h = getIconHeight();
                int arc = Theme.s(THUMB_ARC);
                g.setColor(Tokens.raised());
                g.fillRoundRect(x, y, w - 1, h - 1, arc, arc);
                g.setColor(Tokens.border());
                g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

                int badge = Theme.s(30);
                int bx = x + (w - badge) / 2;
                int by = y + Theme.s(16);
                g.setColor(Tokens.tone(Tokens.accent()));
                g.fillRoundRect(bx, by, badge, badge, arc, arc);
                g.setColor(Tokens.accent());
                new Icons("film", 16).paintIcon(c, g, bx + (badge - Theme.s(16)) / 2, by + (badge - Theme.s(16)) / 2);

                g.setFont(Theme.bold(10));
                g.setColor(Tokens.muted());
                FontMetrics fm = g.getFontMetrics();
                String text = "POSTER";
                g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + h - Theme.s(14));
            } finally {
                g.dispose();
            }
        }
    }
}
