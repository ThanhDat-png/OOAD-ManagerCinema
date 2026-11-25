package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.film.PhimBUS;
import cinema.controller.film.PosterStore;
import cinema.dao.PhimDAO;
import cinema.dao.TheLoaiDAO;
import cinema.model.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public final class PhimPanel extends JPanel {
    static { Theme.install(); }
    private final PhimBUS service;
    private final PosterStore posters = new PosterStore();
    private final JTextField name = new JTextField(18);
    private final JComboBox<TheLoai> genre = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final List<Component> controls = new ArrayList<>();
    private List<TheLoai> genres = List.of();
    private final JLabel message = new JLabel(" ");
    private final JLabel countBadge = new JLabel("0");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Mã phim", "Tên phim", "Phút", "Thể loại", "Khởi chiếu", "Tuổi", "Trạng thái", "Định dạng"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final PosterCells posterCells=new PosterCells(table,posters);
    private final CardLayout viewCards = new CardLayout();
    private final JPanel viewStage = new JPanel(viewCards);
    private final JPanel filmGrid = new JPanel(new Design.Wrap());
    private final JToggleButton viewToggle = new JToggleButton("Xem dạng bảng");
    private final JButton addBtn;
    private final JPanel filterBar = new JPanel(new Design.Wrap());
    private final JPanel actionBar = new JPanel(new Design.Wrap());
    private final List<JPanel> filmCards = new ArrayList<>();
    private final Map<String, ImageIcon> cardPosterCache = new ConcurrentHashMap<>();
    private final boolean isAdmin;
    private String selectedFilmId;
    private boolean busy;

    @Override public void addNotify() {
        super.addNotify();
        if(getClientProperty("cinema.page")==null){
            putClientProperty("cinema.page",true);
            Design.webPage(this, isAdmin ? "Quản lý phim" : "Tra cứu phim", addBtn, filterBar, message);
        }
    }

    public PhimPanel(PhienDangNhap session) {
        super(new BorderLayout(8, Theme.s(14)));
        service = new PhimBUS(session, new PhimDAO(), new TheLoaiDAO(), posters);
        isAdmin = session.vaiTro() == VaiTro.ADMIN;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        if (isAdmin) {
            addBtn = new JButton("Thêm phim");
            addBtn.putClientProperty("cinema.role", "primary");
            addBtn.addActionListener(event -> { if (!busy) new Editor(Mode.ADD, null).setVisible(true); });
            controls.add(addBtn);
        } else {
            addBtn = null;
        }

        filterBar.setOpaque(false);
        filterBar.add(SalesViews.field("Tên phim", name));
        filterBar.add(SalesViews.field("Thể loại", genre));
        statusFilter.addItem("Tất cả trạng thái");
        PhimBUS.TRANG_THAI.forEach(statusFilter::addItem);
        filterBar.add(SalesViews.field("Trạng thái", statusFilter));
        controls.add(name); controls.add(genre); controls.add(statusFilter);
        button(filterBar, "Tìm kiếm", this::refresh);

        JButton clearBtn = new JButton("Xóa bộ lọc");
        clearBtn.putClientProperty("cinema.customBorder", true);
        clearBtn.putClientProperty("cinema.background", "none");
        clearBtn.putClientProperty("cinema.noIcon", true);
        clearBtn.putClientProperty("cinema.role", "link");
        clearBtn.setOpaque(false);
        clearBtn.setContentAreaFilled(false);
        clearBtn.setBorderPainted(false);
        clearBtn.setForeground(Tokens.accent());
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            if (busy) return;
            name.setText("");
            genre.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            refresh();
        });
        controls.add(clearBtn);
        filterBar.add(clearBtn);

        viewToggle.addActionListener(e -> {
            boolean showTable = viewToggle.isSelected();
            viewToggle.setText(showTable ? "Xem dạng lưới" : "Xem dạng bảng");
            viewCards.show(viewStage, showTable ? "table" : "grid");
        });
        filterBar.add(viewToggle);
        name.addActionListener(event -> { if (!busy) refresh(); });
        add(filterBar, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        int[] widths = {135, 260, 65, 165, 110, 65, 145, 90};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int r = table.getSelectedRow();
                selectedFilmId = r >= 0 ? (String) model.getValueAt(table.convertRowIndexToModel(r), 0) : null;
                actionBar.setVisible(selectedFilmId != null);
                filmCards.forEach(Component::repaint);
            }
        });

        filmGrid.putClientProperty("cinema.background", "canvas");
        filmGrid.setLayout(new Design.Wrap());
        JScrollPane gridScroll = new JScrollPane(filmGrid);
        gridScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        gridScroll.getVerticalScrollBar().setUnitIncrement(28);
        JScrollPane tableScroll = new JScrollPane(table);
        viewStage.putClientProperty("cinema.background", "canvas");
        viewStage.add(gridScroll, "grid");
        viewStage.add(tableScroll, "table");

        JPanel bodyWrap = new JPanel(new BorderLayout(0, Theme.s(12)));
        bodyWrap.putClientProperty("cinema.background", "canvas");

        JPanel sectionHead = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.s(10), 0));
        sectionHead.setOpaque(false);
        sectionHead.add(Design.text("Thư viện phim", "heading"));
        countBadge.setOpaque(true);
        countBadge.setBorder(BorderFactory.createEmptyBorder(2, 9, 2, 9));
        countBadge.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            countBadge.setFont(Theme.bold(12));
            countBadge.setBackground(Tokens.tone(Tokens.accent()));
            countBadge.setForeground(Tokens.accent());
        });
        sectionHead.add(countBadge);
        bodyWrap.add(sectionHead, BorderLayout.NORTH);
        bodyWrap.add(viewStage, BorderLayout.CENTER);

        actionBar.setOpaque(false);
        button(actionBar, "Xem chi tiết", () -> selected(id -> run(() -> service.detail(id), this::detail)));
        if (isAdmin) {
            button(actionBar, "Sửa tên phim", () -> edit(Mode.NAME));
            button(actionBar, "Cập nhật poster", () -> edit(Mode.POSTER));
            button(actionBar, "Cập nhật thể loại", () -> edit(Mode.GENRES));
            button(actionBar, "Ngừng chiếu", () -> selected(this::stop));
        }
        actionBar.setVisible(false);
        bodyWrap.add(actionBar, BorderLayout.SOUTH);

        add(bodyWrap, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
        run(service::genres, items -> {
            genres = items;
            genre.addItem(new TheLoai(0, "Tất cả thể loại"));
            items.forEach(genre::addItem);
            refresh();
        });
    }

    private void button(JPanel parent, String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> { if (!busy) action.run(); });
        controls.add(button); parent.add(button);
    }

    private void refresh() {
        TheLoai selected = (TheLoai)genre.getSelectedItem();
        Integer genreId = selected == null || selected.maTheLoai() == 0 ? null : selected.maTheLoai();
        String status = statusFilter.getSelectedIndex() == 0 ? null : (String)statusFilter.getSelectedItem();
        String text = name.getText();
        run(() -> service.search(text, genreId, status), films -> {
            model.setRowCount(0);
            for (Phim film : films) model.addRow(new Object[]{film.maPhim(), film.tenPhim(), film.thoiLuong(),
                    film.theLoai().stream().map(TheLoai::tenTheLoai).collect(Collectors.joining(", ")),
                    film.ngayKhoiChieu(), film.gioiHanTuoi(), film.trangThai(), film.dinhDangChieu()});
            posterCells.update(films);
            countBadge.setText(String.valueOf(films.size()));
            rebuildFilmGrid(films);
            actionBar.setVisible(table.getSelectedRow() >= 0);
            message.setText(films.isEmpty() ? "Không tìm thấy phim phù hợp" : "Số phim: " + films.size());
        });
    }

    private void rebuildFilmGrid(List<Phim> films) {
        filmGrid.removeAll();
        filmCards.clear();
        if (films.isEmpty()) {
            filmGrid.add(Design.text("Không tìm thấy phim phù hợp.", "subtitle"));
        } else {
            for (int i = 0; i < films.size(); i++) {
                JPanel card = createFilmCard(films.get(i), i);
                filmCards.add(card);
                filmGrid.add(card);
            }
        }
        filmGrid.revalidate();
        filmGrid.repaint();
    }

    private JPanel createFilmCard(Phim film, int modelRowIndex) {
        int cardW = Theme.s(228), cardH = Theme.s(416), imgH = Theme.s(306);
        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isSel = film.maPhim().equals(selectedFilmId);
                    int w = getWidth(), h = getHeight(), r = Theme.s(14);
                    g.setColor(Tokens.surface());
                    g.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                    g.setColor(isSel ? Tokens.accent() : Tokens.border());
                    g.setStroke(new BasicStroke(isSel ? 2f : 1f));
                    g.drawRoundRect(0, 0, w - 2, h - 2, r, r);
                } finally {
                    g.dispose();
                }
            }
        };
        card.setOpaque(false);
        card.putClientProperty("cinema.background", "none");
        card.setPreferredSize(new Dimension(cardW, cardH));
        card.setMinimumSize(new Dimension(cardW, cardH));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        JLabel posterLabel = new JLabel(film.dinhDangChieu(), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight(), r = Theme.s(13);
                    Shape clip = new RoundRectangle2D.Float(0, 0, w, h + r, r, r);
                    g.setClip(clip);
                    g.setColor(Tokens.raised());
                    g.fillRect(0, 0, w, h);
                    if (getIcon() != null) {
                        getIcon().paintIcon(this, g, 0, 0);
                    }
                } finally {
                    g.dispose();
                }
                if (getIcon() == null) {
                    super.paintComponent(graphics);
                }
                Graphics2D badgeG = (Graphics2D) graphics.create();
                try {
                    badgeG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    String fmt = film.dinhDangChieu() == null ? "2D" : film.dinhDangChieu();
                    badgeG.setFont(Theme.bold(11));
                    int bw = badgeG.getFontMetrics().stringWidth(fmt) + Theme.s(14);
                    int bh = Theme.s(22);
                    int bx = Theme.s(10), by = getHeight() - bh - Theme.s(10);
                    badgeG.setColor(new Color(16, 17, 27, 215));
                    badgeG.fillRoundRect(bx, by, bw, bh, Theme.s(7), Theme.s(7));
                    badgeG.setColor(Color.WHITE);
                    badgeG.drawString(fmt, bx + Theme.s(7), by + Theme.s(15));
                } finally {
                    badgeG.dispose();
                }
            }
        };
        posterLabel.setPreferredSize(new Dimension(cardW - 2, imgH));
        posterLabel.setForeground(Tokens.muted());
        loadCardPoster(film.poster(), posterLabel, cardW - 2, imgH);
        card.add(posterLabel, BorderLayout.CENTER);

        String genresText = film.theLoai().stream().map(TheLoai::tenTheLoai).collect(Collectors.joining(", "));
        if (genresText.isBlank()) genresText = "Chưa phân loại";
        JLabel stateLbl = Design.text(film.trangThai(), "eyebrow");
        stateLbl.setForeground(Tokens.accent());
        JLabel titleLbl = new JLabel("<html><body style='width:" + Theme.s(160) + "px'>" + escapeHtml(film.tenPhim()) + "</body></html>");
        titleLbl.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            titleLbl.setFont(Theme.bold(14));
            titleLbl.setForeground(Tokens.text());
        });
        JLabel metaLbl = Design.text(film.thoiLuong() + " phút · " + genresText, "subtitle");
        metaLbl.setPreferredSize(new Dimension(cardW - Theme.s(26), Theme.s(18)));

        JPanel caption = new JPanel(new BorderLayout(0, Theme.s(4)));
        caption.setOpaque(false);
        caption.putClientProperty("cinema.background", "none");
        caption.setBorder(BorderFactory.createEmptyBorder(Theme.s(10), Theme.s(12), Theme.s(12), Theme.s(12)));
        caption.add(stateLbl, BorderLayout.NORTH);
        caption.add(titleLbl, BorderLayout.CENTER);
        caption.add(metaLbl, BorderLayout.SOUTH);
        card.add(caption, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectedFilmId = film.maPhim();
                int viewRow = table.convertRowIndexToView(modelRowIndex);
                if (viewRow >= 0 && viewRow < table.getRowCount()) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }
                actionBar.setVisible(true);
                filmCards.forEach(Component::repaint);
                if (e.getClickCount() == 2 && !busy) {
                    run(() -> service.detail(film.maPhim()), PhimPanel.this::detail);
                }
            }
        });
        Theme.apply(card);
        stateLbl.setForeground(Tokens.accent());
        return card;
    }

    private void loadCardPoster(String key, JLabel target, int w, int h) {
        if (key == null || key.isBlank()) return;
        ImageIcon cached = cardPosterCache.get(key);
        if (cached != null) {
            target.setText(null);
            target.setIcon(cached);
            return;
        }
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() {
                BufferedImage img = posters.load(key);
                if (img == null) return null;
                Image scaled = img.getScaledInstance(Math.max(1, w), Math.max(1, h), Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        cardPosterCache.put(key, icon);
                        target.setText(null);
                        target.setIcon(icon);
                    }
                } catch (Exception ignored) {
                    // Keep placeholder badge when poster fails to load.
                }
            }
        }.execute();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void selected(Consumer<String> action) {
        int row = table.getSelectedRow();
        if (row < 0) { message.setText("Vui lòng chọn phim"); return; }
        action.accept((String)model.getValueAt(table.convertRowIndexToModel(row), 0));
    }

    private void edit(Mode mode) {
        selected(id -> run(() -> service.detail(id), film -> new Editor(mode, film).setVisible(true)));
    }

    private void detail(Phim film) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Thông tin phim", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel body = new JPanel(new BorderLayout(16, 16));
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JTextArea text = new JTextArea("Mã phim: " + film.maPhim() + "\nTên phim: " + film.tenPhim()
                + "\nThời lượng: " + film.thoiLuong() + " phút\nĐạo diễn: " + film.daoDien()
                + "\nKhởi chiếu: " + film.ngayKhoiChieu() + "\nGiới hạn tuổi: " + film.gioiHanTuoi()
                + "\nĐịnh dạng: " + film.dinhDangChieu() + "\nTrạng thái: " + film.trangThai()
                + "\nThể loại: " + film.theLoai().stream().map(TheLoai::tenTheLoai).collect(Collectors.joining(", ")), 12, 35);
        text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        JLabel image = new JLabel("Đang tải poster...", SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(230, 320));
        body.add(image, BorderLayout.WEST); body.add(new JScrollPane(text), BorderLayout.CENTER);
        JButton back = new JButton("Trở về"); back.addActionListener(event -> dialog.dispose());
        body.add(back, BorderLayout.SOUTH); dialog.add(body); dialog.pack(); dialog.setLocationRelativeTo(this);
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() { return posters.load(film.poster()); }
            @Override protected void done() {
                try { showImage(image, get()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { image.setText("Không đọc được poster đã lưu"); }
            }
        }.execute();
        dialog.setVisible(true);
    }

    private void stop(String id) {
        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn ngừng chiếu phim này?",
                "Xác nhận ngừng chiếu", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        run(() -> { service.stop(id); return id; }, value -> {
            JOptionPane.showMessageDialog(this, "Ngừng chiếu phim thành công"); refresh();
        });
    }

    private enum Mode { ADD, NAME, POSTER, GENRES }

    private final class Editor extends JDialog {
        private final Mode mode;
        private final Phim existing;
        private final JTextField filmName = new JTextField(28);
        private final JTextField duration = new JTextField();
        private final JTextField director = new JTextField();
        private final JTextField release = new JTextField();
        private final JTextField age = new JTextField("0");
        private final JTextField format = new JTextField("2D");
        private final JComboBox<String> status = new JComboBox<>(PhimBUS.TRANG_THAI.toArray(String[]::new));
        private final List<JCheckBox> genreChecks = new ArrayList<>();
        private final JLabel preview = new JLabel("Chọn ảnh PNG/JPEG", SwingConstants.CENTER);
        private final JButton choose = new JButton("Chọn poster");
        private final JButton save = new JButton("Lưu");
        private final JButton cancel = new JButton("Hủy");
        private Path selectedPoster;

        Editor(Mode mode, Phim existing) {
            super(SwingUtilities.getWindowAncestor(PhimPanel.this), "Thông tin phim", Dialog.ModalityType.APPLICATION_MODAL);
            this.mode = mode; this.existing = existing;
            JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
            if (existing != null) filmName.setText(existing.tenPhim());
            if (mode == Mode.ADD || mode == Mode.NAME) row(fields, "Tên phim", filmName);
            if (mode == Mode.ADD) {
                row(fields, "Thời lượng (phút)", duration); row(fields, "Đạo diễn", director);
                row(fields, "Khởi chiếu (yyyy-MM-dd)", release); row(fields, "Giới hạn tuổi", age);
                row(fields, "Định dạng chiếu", format); row(fields, "Trạng thái", status);
            }
            if (mode == Mode.ADD || mode == Mode.GENRES) {
                row(fields, "Thể loại", buildGenrePicker(existing));
            }
            JPanel body = new JPanel(new BorderLayout(12, 12));
            body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            body.add(fields, BorderLayout.CENTER);
            if (mode == Mode.ADD || mode == Mode.POSTER) {
                JPanel images = new JPanel(new BorderLayout());
                preview.setPreferredSize(new Dimension(230, 320));
                images.add(preview, BorderLayout.CENTER); images.add(choose, BorderLayout.SOUTH);
                body.add(images, BorderLayout.EAST); save.setEnabled(false);
                choose.addActionListener(event -> choosePoster());
                if (existing != null) loadCurrentPoster();
            }
            JPanel buttons = new JPanel(); buttons.add(save); buttons.add(cancel);
            body.add(buttons, BorderLayout.SOUTH); add(body);
            save.addActionListener(event -> save()); cancel.addActionListener(event -> dispose());
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); pack(); setLocationRelativeTo(PhimPanel.this);
        }

        private JScrollPane buildGenrePicker(Phim existing) {
            JPanel grid = new JPanel(new GridLayout(0, 2, Theme.s(12), Theme.s(6)));
            grid.putClientProperty("cinema.background", "raised");
            grid.setBorder(BorderFactory.createEmptyBorder(Theme.s(4), Theme.s(6), Theme.s(4), Theme.s(6)));
            for (TheLoai g : genres) {
                JCheckBox box = new JCheckBox(g.tenTheLoai());
                box.setOpaque(false);
                box.setContentAreaFilled(false);
                box.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                    box.setOpaque(false);
                    box.setContentAreaFilled(false);
                    box.setFont(Tokens.font(13));
                    box.setForeground(Tokens.text());
                });
                box.setFont(Tokens.font(13));
                box.setForeground(Tokens.text());
                if (existing != null && existing.theLoai().stream().anyMatch(t -> t.maTheLoai() == g.maTheLoai())) {
                    box.setSelected(true);
                }
                genreChecks.add(box);
                grid.add(box);
            }
            int rows = Math.max(2, (genres.size() + 1) / 2);
            int innerH = Math.max(grid.getPreferredSize().height, rows * Theme.s(28) + Theme.s(12));
            int prefH = Math.min(Theme.s(230), innerH + Theme.s(26));
            JScrollPane scroll = new JScrollPane(grid);
            scroll.putClientProperty("cinema.customBorder", true);
            scroll.setBorder(new Skin.ControlBorder());
            scroll.getViewport().putClientProperty("cinema.background", "raised");
            scroll.getViewport().setBackground(Tokens.raised());
            scroll.putClientProperty("cinema.statusRefresh", (Runnable) () -> scroll.setBorder(new Skin.ControlBorder()));
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setPreferredSize(new Dimension(Theme.s(370), prefH));
            scroll.setMinimumSize(new Dimension(Theme.s(310), Math.min(prefH, Theme.s(144))));
            scroll.setToolTipText("Chọn một hoặc nhiều thể loại cho phim");
            return scroll;
        }

        private List<TheLoai> selectedGenreValues() {
            List<TheLoai> result = new ArrayList<>();
            for (int i = 0; i < genreChecks.size() && i < genres.size(); i++) {
                if (genreChecks.get(i).isSelected()) result.add(genres.get(i));
            }
            return result;
        }

        private void row(JPanel panel, String label, Component field) { panel.add(new JLabel(label)); panel.add(field); }

        private void loadCurrentPoster() {
            new SwingWorker<BufferedImage, Void>() {
                @Override protected BufferedImage doInBackground() { return posters.load(existing.poster()); }
                @Override protected void done() {
                    try { if (selectedPoster == null && choose.isEnabled()) showImage(preview, get()); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    catch (ExecutionException e) { preview.setText("Không đọc được poster hiện tại"); }
                }
            }.execute();
        }

        private void choosePoster() {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Ảnh PNG/JPEG", "png", "jpg", "jpeg"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path source = chooser.getSelectedFile().toPath();
            choose.setEnabled(false); save.setEnabled(false); selectedPoster = null;
            new SwingWorker<BufferedImage, Void>() {
                @Override protected BufferedImage doInBackground() { return posters.preview(source); }
                @Override protected void done() {
                    try { showImage(preview, get()); selectedPoster = source; save.setEnabled(true); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    catch (ExecutionException e) { JOptionPane.showMessageDialog(Editor.this, error(e.getCause())); }
                    finally { choose.setEnabled(true); }
                }
            }.execute();
        }

        private void save() {
            final Phim draft;
            List<TheLoai> newGenres = selectedGenreValues();
            try {
                draft = mode == Mode.ADD ? new Phim(null, filmName.getText(), Integer.parseInt(duration.getText().trim()),
                        director.getText(), LocalDate.parse(release.getText().trim()), Integer.parseInt(age.getText().trim()),
                        null, (String)status.getSelectedItem(), format.getText(), newGenres) : null;
            } catch (NumberFormatException | DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Kiểm tra thời lượng, giới hạn tuổi và ngày khởi chiếu (yyyy-MM-dd)."); return;
            }
            String newName = filmName.getText();
            Path image = selectedPoster;
            save.setEnabled(false); cancel.setEnabled(false); choose.setEnabled(false);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    switch (mode) {
                        case ADD -> service.create(draft, image);
                        case NAME -> service.rename(existing.maPhim(), newName);
                        case POSTER -> service.updatePoster(existing.maPhim(), image);
                        case GENRES -> service.updateGenres(existing.maPhim(), newGenres);
                    }
                    return null;
                }
                @Override protected void done() {
                    try {
                        get(); dispose();
                        String message = switch (mode) {
                            case ADD -> "Thêm phim thành công";
                            case NAME -> "Cập nhật tên phim thành công";
                            case POSTER -> "Cập nhật poster thành công";
                            case GENRES -> "Cập nhật thể loại thành công";
                        };
                        JOptionPane.showMessageDialog(PhimPanel.this, message); refresh();
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    catch (ExecutionException e) { JOptionPane.showMessageDialog(Editor.this, error(e.getCause())); }
                    finally {
                        save.setEnabled(true); cancel.setEnabled(true); choose.setEnabled(true);
                        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    }
                }
            }.execute();
        }
    }

    private static void showImage(JLabel label, BufferedImage image) {
        double scale = Math.min(230.0 / image.getWidth(), 320.0 / image.getHeight());
        label.setText(null); label.setIcon(new ImageIcon(image.getScaledInstance(
                Math.max(1, (int)(image.getWidth() * scale)), Math.max(1, (int)(image.getHeight() * scale)), Image.SCALE_SMOOTH)));
    }

    private <T> void run(Callable<T> task, Consumer<T> success) {
        if (busy) return;
        String previousMessage=message.getText();
        busy = true; controls.forEach(c -> c.setEnabled(false)); message.setText("Đang tải dữ liệu...");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy = false; controls.forEach(c -> c.setEnabled(true));
                try { T result=get(); message.setText(previousMessage); success.accept(result); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { message.setText(error(e.getCause())); }
            }
        }.execute();
    }

    private static String error(Throwable error) {
        return error instanceof IllegalArgumentException ? error.getMessage()
                : "Không thể đọc hoặc lưu thông tin phim. Vui lòng kiểm tra CSDL và thư mục poster.";
    }
}
