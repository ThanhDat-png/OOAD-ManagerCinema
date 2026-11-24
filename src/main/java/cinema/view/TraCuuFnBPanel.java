package cinema.view;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.pricing.SanPhamBUS;
import cinema.dao.SanPhamDAO;
import cinema.model.SanPham;
import cinema.view.ui.Design;
import cinema.view.ui.Theme;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Read-only F&B catalog and live stock view for counter staff (4-layer: view -> SanPhamBUS -> SanPhamDAO -> MySQL). */
public final class TraCuuFnBPanel extends JPanel {
    static { Theme.install(); }

    private final SanPhamBUS products;
    private final JTextField keyword = new JTextField(20);
    private final JComboBox<String> stockFilter = new JComboBox<>(new String[]{"Tất cả sản phẩm", "Còn hàng", "Hết hàng"});
    private final JButton searchBtn = new JButton("Tìm kiếm");
    private final JLabel message = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Mã SP", "Tên sản phẩm", "Mô tả", "ĐVT", "Giá bán (VND)", "Tồn kho", "Tình trạng"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private boolean busy;

    public TraCuuFnBPanel(PhienDangNhap session) {
        super(new BorderLayout(8, 8));
        this.products = new SanPhamBUS(session, new SanPhamDAO());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel filters = new JPanel();
        filters.add(new JLabel("Từ khóa"));
        filters.add(keyword);
        filters.add(new JLabel("Tồn kho"));
        filters.add(stockFilter);
        filters.add(searchBtn);
        add(filters, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        int[] widths = {90, 200, 235, 75, 130, 85, 185};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> load());
        keyword.addActionListener(e -> { if (!busy) load(); });
        stockFilter.addActionListener(e -> { if (!busy) load(); });
        load();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (getClientProperty("cinema.page") == null) {
            putClientProperty("cinema.page", true);
            Design.page(this, "Thực đơn F&B", "Tra cứu giá bán và tồn kho bắp nước tại quầy", message);
        }
    }

    private void load() {
        if (busy) return;
        busy = true;
        searchBtn.setEnabled(false);
        keyword.setEnabled(false);
        stockFilter.setEnabled(false);
        String text = keyword.getText();
        Boolean inStock = stockFilter.getSelectedIndex() == 1 ? Boolean.TRUE
                : stockFilter.getSelectedIndex() == 2 ? Boolean.FALSE : null;
        message.setText("Đang tải danh sách sản phẩm F&B...");
        new SwingWorker<List<SanPham>, Void>() {
            @Override protected List<SanPham> doInBackground() throws Exception {
                return products.catalog(text, inStock);
            }
            @Override protected void done() {
                busy = false;
                searchBtn.setEnabled(true);
                keyword.setEnabled(true);
                stockFilter.setEnabled(true);
                try {
                    List<SanPham> rows = get();
                    model.setRowCount(0);
                    for (SanPham p : rows) {
                        model.addRow(new Object[]{
                                p.maSP(), p.tenSP(), p.moTa(), p.donViTinh(),
                                p.giaBan(), p.soLuongTon(), p.tinhTrangKinhDoanh()
                        });
                    }
                    message.setText(rows.isEmpty() ? "Không tìm thấy sản phẩm F&B phù hợp" : "Hiển thị " + rows.size() + " sản phẩm F&B");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    message.setText(cause instanceof IllegalArgumentException
                            ? cause.getMessage()
                            : "Không thể tải danh sách F&B. Vui lòng kiểm tra kết nối CSDL.");
                }
            }
        }.execute();
    }
}
