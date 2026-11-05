package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.film.PhimBUS;
import cinema.controller.film.PosterStore;
import cinema.dao.PhimDAO;
import cinema.dao.TheLoaiDAO;
import cinema.model.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Mã phim", "Tên phim", "Phút", "Thể loại", "Khởi chiếu", "Tuổi", "Trạng thái", "Định dạng"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private boolean busy;

    @Override public void addNotify() { super.addNotify(); if(getClientProperty("cinema.page")==null){putClientProperty("cinema.page",true);Design.page(this,"Thư viện phim","Tìm phim, quản lý thông tin và hình ảnh",message);} }

    public PhimPanel(PhienDangNhap session) {
        super(new BorderLayout(8, 8));
        service = new PhimBUS(session, new PhimDAO(), new TheLoaiDAO(), posters);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel top = new JPanel(new GridLayout(0, 1, 4, 4));
        JPanel filters = new JPanel();
        filters.add(new JLabel("Tên phim")); filters.add(name);
        filters.add(new JLabel("Thể loại")); filters.add(genre);
        statusFilter.addItem("Tất cả trạng thái");
        PhimBUS.TRANG_THAI.forEach(statusFilter::addItem);
        filters.add(statusFilter);
        controls.add(name); controls.add(genre); controls.add(statusFilter);
        button(filters, "Tìm kiếm", this::refresh);
        name.addActionListener(event -> { if (!busy) refresh(); });
        top.add(filters);
        JPanel actions = new JPanel();
        button(actions, "Xem chi tiết", () -> selected(id -> run(() -> service.detail(id), this::detail)));
        if (session.vaiTro() == VaiTro.ADMIN) {
            button(actions, "Thêm phim", () -> new Editor(Mode.ADD, null).setVisible(true));
            button(actions, "Sửa tên phim", () -> edit(Mode.NAME));
            button(actions, "Cập nhật poster", () -> edit(Mode.POSTER));
            button(actions, "Cập nhật thể loại", () -> edit(Mode.GENRES));
            button(actions, "Ngừng chiếu", () -> selected(this::stop));
        }
        top.add(actions); add(top, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER); add(message, BorderLayout.SOUTH);
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
            message.setText(films.isEmpty() ? "Không tìm thấy phim phù hợp" : "Số phim: " + films.size());
        });
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
        private final JList<TheLoai> selectedGenres = new JList<>(genres.toArray(TheLoai[]::new));
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
                selectedGenres.setVisibleRowCount(5);
                if (existing != null) selectedGenres.setSelectedIndices(java.util.stream.IntStream.range(0, genres.size())
                        .filter(i -> existing.theLoai().stream().anyMatch(g -> g.maTheLoai() == genres.get(i).maTheLoai())).toArray());
                row(fields, "Thể loại (Ctrl để chọn nhiều)", new JScrollPane(selectedGenres));
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
            try {
                draft = mode == Mode.ADD ? new Phim(null, filmName.getText(), Integer.parseInt(duration.getText().trim()),
                        director.getText(), LocalDate.parse(release.getText().trim()), Integer.parseInt(age.getText().trim()),
                        null, (String)status.getSelectedItem(), format.getText(), selectedGenres.getSelectedValuesList()) : null;
            } catch (NumberFormatException | DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Kiểm tra thời lượng, giới hạn tuổi và ngày khởi chiếu (yyyy-MM-dd)."); return;
            }
            String newName = filmName.getText();
            List<TheLoai> newGenres = selectedGenres.getSelectedValuesList();
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
