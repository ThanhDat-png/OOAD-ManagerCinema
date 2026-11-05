package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.staff.NhanVienBUS;
import cinema.dao.NhanVienDAO;
import cinema.model.NhanVien;
import cinema.model.VaiTro;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class NhanVienPanel extends JPanel {
    static { Theme.install(); }
    private final NhanVienBUS service;
    private final JTextField keyword = new JTextField(20);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Mã nhân viên", "Họ tên", "Ngày sinh", "Số điện thoại", "Email", "Địa chỉ", "Chức vụ"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");
    private final JPanel actions = new JPanel();
    private boolean busy;

    @Override public void addNotify() { super.addNotify(); if(getClientProperty("cinema.page")==null){putClientProperty("cinema.page",true);Design.page(this,Boolean.TRUE.equals(getClientProperty("cinema.personal"))?"Hồ sơ cá nhân":"Nhân viên","Hồ sơ và thông tin nhân viên",status);} }

    public NhanVienPanel(PhienDangNhap session) {
        super(new BorderLayout(8, 8));
        service = new NhanVienBUS(session, new NhanVienDAO());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        add(actions, BorderLayout.NORTH);
        if (session.vaiTro() == VaiTro.ADMIN) {
            actions.add(new JLabel("Mã, họ tên hoặc số điện thoại"));
            actions.add(keyword);
            button("Tìm kiếm", this::refresh);
            button("Thêm nhân viên", () -> edit(null));
            button("Xem chi tiết", () -> selected(this::detail));
            button("Sửa nhân viên", () -> selected(id -> run(() -> service.detail(id), this::edit)));
            button("Xóa nhân viên", () -> selected(this::delete));
            keyword.addActionListener(event -> refresh());
            refresh();
        } else {
            putClientProperty("cinema.personal",true);Component old=((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER);remove(old);add(ReadOnlyViews.profile(table),BorderLayout.CENTER);
            String id = session.nhanVien() == null ? null : session.nhanVien().maNV();
            run(() -> service.detail(id), employee -> showRows(List.of(employee)));
        }
    }

    private void button(String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> { if (!busy) action.run(); });
        actions.add(button);
    }

    private void refresh() {
        String search = keyword.getText();
        run(() -> service.search(search), this::showRows);
    }

    private void showRows(List<NhanVien> employees) {
        model.setRowCount(0);
        for (NhanVien e : employees) model.addRow(new Object[]{e.maNV(), e.hoTen(), e.ngaySinh(),
                e.soDienThoai(), e.email(), e.diaChi(), e.chucVu()});
        status.setText(employees.isEmpty() ? "Không tìm thấy nhân viên phù hợp" : "Số hồ sơ: " + employees.size());
    }

    private void selected(Consumer<String> action) {
        int row = table.getSelectedRow();
        if (row < 0) { status.setText("Vui lòng chọn nhân viên"); return; }
        action.accept((String) model.getValueAt(table.convertRowIndexToModel(row), 0));
    }

    private void detail(String id) {
        run(() -> service.detail(id), e -> {
            JTextArea text = new JTextArea("Mã nhân viên: " + e.maNV() + "\nHọ tên: " + e.hoTen()
                    + "\nNgày sinh: " + e.ngaySinh() + "\nSố điện thoại: " + e.soDienThoai()
                    + "\nEmail: " + e.email() + "\nĐịa chỉ: " + e.diaChi() + "\nChức vụ: " + e.chucVu(), 9, 36);
            text.setEditable(false);
            JOptionPane.showMessageDialog(this, ReadOnlyViews.info(text,"Thông tin nhân viên"), "Thông tin nhân viên", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void delete(String id) {
        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa nhân viên này?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        run(() -> { service.delete(id); return id; }, value -> {
            JOptionPane.showMessageDialog(this, "Xóa nhân viên thành công");
            refresh();
        });
    }

    private void edit(NhanVien existing) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Thêm nhân viên" : "Sửa nhân viên", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        String[] names = {"Họ tên", "Ngày sinh (yyyy-MM-dd)", "Số điện thoại", "Email", "Địa chỉ", "Chức vụ"};
        String[] values = existing == null ? new String[]{"", "", "", "", "", ""}
                : new String[]{existing.hoTen(), existing.ngaySinh() == null ? "" : existing.ngaySinh().toString(),
                    existing.soDienThoai(), existing.email(), existing.diaChi(), existing.chucVu()};
        JTextField[] fields = new JTextField[names.length];
        for (int i = 0; i < names.length; i++) {
            fields[i] = new JTextField(values[i], 24);
            form.add(new JLabel(names[i])); form.add(fields[i]);
        }
        JButton save = new JButton("Lưu");
        JButton cancel = new JButton("Hủy");
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            NhanVien input;
            try {
                input = new NhanVien(existing == null ? null : existing.maNV(), fields[0].getText(),
                        LocalDate.parse(fields[1].getText().trim()), fields[2].getText(), fields[3].getText(),
                        fields[4].getText(), fields[5].getText());
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(dialog, "Ngày sinh không hợp lệ. Nhập theo yyyy-MM-dd."); return;
            }
            save.setEnabled(false); cancel.setEnabled(false);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    if (existing == null) service.create(input); else service.update(input);
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        dialog.dispose();
                        JOptionPane.showMessageDialog(NhanVienPanel.this, existing == null
                                ? "Thêm nhân viên thành công" : "Cập nhật nhân viên thành công");
                        refresh();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        JOptionPane.showMessageDialog(dialog, error(e.getCause()));
                    } finally {
                        save.setEnabled(true); cancel.setEnabled(true);
                        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    }
                }
            }.execute();
        });
        form.add(save); form.add(cancel);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.add(form); dialog.pack(); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    private <T> void run(Callable<T> task, Consumer<T> success) {
        if (busy) return;
        String previousMessage=status.getText();
        busy = true;
        status.setText("Đang tải dữ liệu...");
        for (Component c : actions.getComponents()) c.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy = false;
                for (Component c : actions.getComponents()) c.setEnabled(true);
                try { T result=get(); status.setText(previousMessage); success.accept(result); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { status.setText(error(e.getCause())); }
            }
        }.execute();
    }

    private static String error(Throwable error) {
        return error instanceof IllegalArgumentException ? error.getMessage()
                : "Không thể lưu hoặc đọc dữ liệu. Vui lòng kiểm tra kết nối và thử lại.";
    }
}
