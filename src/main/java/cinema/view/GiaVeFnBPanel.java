package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.pricing.*;
import cinema.dao.*;
import cinema.model.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class GiaVeFnBPanel extends JPanel {
    static { Theme.install(); }
    private final GiaVeBUS prices;
    private final SanPhamBUS products;
    private final DefaultTableModel priceModel=tableModel("Mã giá","Loại ghế","Loại phòng","Thời điểm","Giá vé (VND)");
    private final DefaultTableModel productModel=tableModel("Mã sản phẩm","Tên sản phẩm","Mô tả","Đơn vị tính","Giá bán (VND)","Số lượng tồn","Tồn kho","Kinh doanh");
    private final JTable priceTable=new JTable(priceModel),productTable=new JTable(productModel);
    private final JTextField keyword=new JTextField(22);
    private final JComboBox<String> stock=new JComboBox<>(new String[]{"Tất cả sản phẩm","Còn hàng","Hết hàng"});
    private final JLabel message=new JLabel(" ");
    private final List<Component> controls=new ArrayList<>();
    private List<LoaiGhe> seatTypes=List.of();
    private List<LoaiPhong> roomTypes=List.of();
    private List<LoaiThoiDiem> periods=List.of();
    private boolean busy;
    private record Data(List<BangGiaVe> prices,List<SanPham> products,List<LoaiGhe> seats,List<LoaiPhong> rooms,List<LoaiThoiDiem> periods) { }

    @Override public void addNotify() { super.addNotify(); if(getClientProperty("cinema.page")==null){putClientProperty("cinema.page",true);Design.page(this,"Giá vé & F&B","Bảng giá, sản phẩm và tồn kho hiện tại",message);} }

    public GiaVeFnBPanel(PhienDangNhap session) {
        super(new BorderLayout(8,8));
        prices=new GiaVeBUS(session,new BangGiaVeDAO()); products=new SanPhamBUS(session,new SanPhamDAO());
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JTabbedPane tabs=new JTabbedPane();
        JPanel pricing=new JPanel(new BorderLayout(8,8)),priceActions=new JPanel();
        button(priceActions,"Thiết lập giá vé",() -> priceEditor(null));
        button(priceActions,"Sửa giá vé",() -> selectPrice(id -> load(() -> prices.detail(id),this::priceEditor)));
        button(priceActions,"Tải lại bảng giá",this::refresh);
        pricing.add(priceActions,BorderLayout.NORTH); pricing.add(new JScrollPane(priceTable),BorderLayout.CENTER); tabs.addTab("Giá vé",pricing);
        JPanel catalog=new JPanel(new BorderLayout(8,8)); JPanel top=new JPanel(new GridLayout(0,1,4,4));
        JPanel filters=new JPanel(); filters.add(new JLabel("Mã hoặc tên sản phẩm")); filters.add(keyword); filters.add(stock);
        controls.add(keyword); controls.add(stock); button(filters,"Kiểm tra tồn kho",this::refresh); top.add(filters);
        JPanel actions=new JPanel(); button(actions,"Thêm sản phẩm",() -> productEditor(null));
        button(actions,"Cập nhật sản phẩm",() -> selectProduct(id -> load(() -> products.detail(id),this::productEditor)));
        button(actions,"Quản lý giá bán",() -> selectProduct(id -> load(() -> products.detail(id),this::productPriceEditor))); top.add(actions);
        catalog.add(top,BorderLayout.NORTH); catalog.add(new JScrollPane(productTable),BorderLayout.CENTER); tabs.addTab("Quản lý F&B",catalog);
        for(JTable table:List.of(priceTable,productTable)) { table.setAutoCreateRowSorter(true); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); }
        keyword.addActionListener(event -> { if(!busy) refresh(); }); add(tabs,BorderLayout.CENTER); add(message,BorderLayout.SOUTH); refresh();
    }
    private void button(JPanel parent,String title,Runnable action) {
        JButton button=new JButton(title); controls.add(button); parent.add(button);
        button.addActionListener(event -> { if(!busy) action.run(); });
    }
    private void refresh() {
        String text=keyword.getText(); Boolean available=stock.getSelectedIndex()==0?null:stock.getSelectedIndex()==1;
        load(() -> new Data(prices.all(),products.search(text,available),prices.seatTypes(),prices.roomTypes(),prices.periods()),data -> {
            seatTypes=data.seats(); roomTypes=data.rooms(); periods=data.periods(); priceModel.setRowCount(0); productModel.setRowCount(0);
            for(BangGiaVe price:data.prices()) priceModel.addRow(new Object[]{price.maGia(),price.loaiGhe().tenLoaiGhe(),price.loaiPhong().tenLoaiPhong(),price.loaiThoiDiem().tenLoaiThoiDiem(),price.giaVe()});
            for(SanPham product:data.products()) productModel.addRow(new Object[]{product.maSP(),product.tenSP(),product.moTa(),product.donViTinh(),product.giaBan(),product.soLuongTon(),
                    product.soLuongTon()==0?"Hết hàng":"Còn hàng",product.tinhTrangKinhDoanh()});
            message.setText("Mức giá vé: "+data.prices().size()+" · "+(data.products().isEmpty()?"Không tìm thấy sản phẩm phù hợp":"Sản phẩm phù hợp: "+data.products().size()));
        });
    }
    private void selectPrice(Consumer<Integer> action) {
        int row=priceTable.getSelectedRow(); if(row<0) { message.setText("Vui lòng chọn mức giá vé"); return; }
        action.accept((Integer)priceModel.getValueAt(priceTable.convertRowIndexToModel(row),0));
    }
    private void selectProduct(Consumer<String> action) {
        int row=productTable.getSelectedRow(); if(row<0) { message.setText("Vui lòng chọn sản phẩm"); return; }
        action.accept((String)productModel.getValueAt(productTable.convertRowIndexToModel(row),0));
    }

    private void priceEditor(BangGiaVe existing) {
        String title=existing==null?"Thiết lập giá vé":"Sửa giá vé";
        JDialog dialog=dialog(title); JPanel form=form();
        JComboBox<LoaiGhe> seat=new JComboBox<>(seatTypes.toArray(LoaiGhe[]::new));
        JComboBox<LoaiPhong> room=new JComboBox<>(roomTypes.toArray(LoaiPhong[]::new));
        JComboBox<LoaiThoiDiem> period=new JComboBox<>(periods.toArray(LoaiThoiDiem[]::new));
        renderer(seat); renderer(room); renderer(period);
        if(existing==null) { row(form,"Loại ghế",seat); row(form,"Loại phòng",room); row(form,"Thời điểm",period); }
        else {
            row(form,"Loại ghế",new JLabel(existing.loaiGhe().tenLoaiGhe())); row(form,"Loại phòng",new JLabel(existing.loaiPhong().tenLoaiPhong()));
            row(form,"Thời điểm",new JLabel(existing.loaiThoiDiem().tenLoaiThoiDiem()));
        }
        JTextField amount=new JTextField(existing==null?"":existing.giaVe().toPlainString(),22);
        row(form,"Giá vé (VND, ví dụ 100000)",amount); JButton save=new JButton("Lưu"),cancel=new JButton("Hủy"); form.add(save); form.add(cancel);
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            BigDecimal price;
            try { price=money(amount.getText(),"Vui lòng nhập giá vé hợp lệ"); }
            catch(IllegalArgumentException e) { JOptionPane.showMessageDialog(dialog,e.getMessage()); return; }
            LoaiGhe chosenSeat=(LoaiGhe)seat.getSelectedItem(); LoaiPhong chosenRoom=(LoaiPhong)room.getSelectedItem(); LoaiThoiDiem chosenPeriod=(LoaiThoiDiem)period.getSelectedItem();
            perform(dialog,save,cancel,() -> {
                if(existing==null) prices.create(chosenSeat==null?0:chosenSeat.maLoaiGhe(),chosenRoom==null?0:chosenRoom.maLoaiPhong(),chosenPeriod==null?0:chosenPeriod.maLoaiThoiDiem(),price);
                else prices.update(existing.maGia(),price);
                return existing==null?"Thiết lập giá vé thành công":"Cập nhật giá vé thành công";
            },text -> { JOptionPane.showMessageDialog(this,text); refresh(); });
        });
        show(dialog,form);
    }
    private void productEditor(SanPham existing) {
        JDialog dialog=dialog(existing==null?"Thêm sản phẩm":"Cập nhật sản phẩm"); JPanel form=form();
        JTextField code=new JTextField(existing==null?"":existing.maSP(),22),name=new JTextField(existing==null?"":existing.tenSP());
        JTextArea description=new JTextArea(existing==null?"":existing.moTa(),3,22); description.setLineWrap(true); description.setWrapStyleWord(true);
        JTextField unit=new JTextField(existing==null?"":existing.donViTinh()),amount=new JTextField(existing==null?"":existing.giaBan().toPlainString());
        JTextField quantity=new JTextField(existing==null?"0":Integer.toString(existing.soLuongTon()));
        JComboBox<String> state=new JComboBox<>(SanPhamBUS.TINH_TRANG.toArray(String[]::new)); if(existing!=null) state.setSelectedItem(existing.tinhTrangKinhDoanh());
        row(form,"Mã sản phẩm",existing==null?code:new JLabel(existing.maSP())); row(form,"Tên sản phẩm",name);
        row(form,"Mô tả",new JScrollPane(description)); row(form,"Đơn vị tính",unit);
        if(existing==null) row(form,"Giá bán (VND, ví dụ 50000)",amount);
        row(form,"Số lượng tồn kho",quantity); row(form,"Tình trạng kinh doanh",state);
        JButton save=new JButton("Lưu"),cancel=new JButton("Hủy"); form.add(save); form.add(cancel); cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            SanPham product;
            try {
                int stockCount=Integer.parseInt(quantity.getText().trim());
                product=new SanPham(code.getText(),name.getText(),description.getText(),unit.getText(),
                        existing==null?money(amount.getText(),"Vui lòng nhập giá bán hợp lệ"):existing.giaBan(),stockCount,(String)state.getSelectedItem());
            } catch(NumberFormatException e) { JOptionPane.showMessageDialog(dialog,"Số lượng tồn kho không hợp lệ"); return; }
            catch(IllegalArgumentException e) { JOptionPane.showMessageDialog(dialog,e.getMessage()); return; }
            perform(dialog,save,cancel,() -> {
                if(existing==null) products.create(product); else products.updateInfo(product);
                return existing==null?"Thêm sản phẩm thành công":"Cập nhật sản phẩm thành công";
            },text -> {
                keyword.setText(product.maSP()); stock.setSelectedIndex(0); JOptionPane.showMessageDialog(this,text); refresh();
            });
        });
        show(dialog,form);
    }
    private void productPriceEditor(SanPham product) {
        JDialog dialog=dialog("Quản lý giá bán"); JPanel form=form();
        row(form,"Sản phẩm",new JLabel(product.maSP()+" · "+product.tenSP()));
        JTextField amount=new JTextField(product.giaBan().toPlainString(),22); row(form,"Giá bán (VND)",amount);
        JButton save=new JButton("Lưu"),cancel=new JButton("Hủy"); form.add(save); form.add(cancel); cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            BigDecimal price;
            try { price=money(amount.getText(),"Vui lòng nhập giá bán hợp lệ"); }
            catch(IllegalArgumentException e) { JOptionPane.showMessageDialog(dialog,e.getMessage()); return; }
            perform(dialog,save,cancel,() -> { products.updatePrice(product.maSP(),price); return price; },saved -> {
                JOptionPane.showMessageDialog(this,"Cập nhật giá bán thành công"); refresh();
            });
        }); show(dialog,form);
    }

    private <T> void load(Callable<T> task,Consumer<T> success) {
        if(busy) return; String previousMessage=message.getText(); busy=true; controls.forEach(c -> c.setEnabled(false)); message.setText("Đang tải dữ liệu...");
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy=false; controls.forEach(c -> c.setEnabled(true));
                try { T result=get(); message.setText(previousMessage); success.accept(result); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
                catch(ExecutionException e) { message.setText(error(e.getCause())); }
            }
        }.execute();
    }
    private <T> void perform(JDialog dialog,JButton save,JButton cancel,Callable<T> task,Consumer<T> success) {
        save.setEnabled(false); cancel.setEnabled(false); dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                try { T result=get(); dialog.dispose(); success.accept(result); }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); }
                catch(ExecutionException e) { JOptionPane.showMessageDialog(dialog,error(e.getCause())); }
                finally { save.setEnabled(true); cancel.setEnabled(true); dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); }
            }
        }.execute();
    }
    private JDialog dialog(String title) { return new JDialog(SwingUtilities.getWindowAncestor(this),title,Dialog.ModalityType.APPLICATION_MODAL); }
    private void show(JDialog dialog,JPanel form) { dialog.add(form); dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); dialog.pack(); dialog.setLocationRelativeTo(this); dialog.setVisible(true); }
    private static JPanel form() { JPanel form=new JPanel(new GridLayout(0,2,8,8)); form.setBorder(BorderFactory.createEmptyBorder(16,16,16,16)); return form; }
    private static void row(JPanel panel,String label,Component value) { panel.add(new JLabel(label)); panel.add(value); }
    private static BigDecimal money(String value,String message) {
        String text=value.trim(); if(!text.matches("[0-9]{1,12}")) throw new IllegalArgumentException(message+". Nhập VND bằng chữ số, tối đa 12 chữ số"); return new BigDecimal(text);
    }
    private static void renderer(JComboBox<?> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                if(value instanceof LoaiGhe type) label.setText(type.tenLoaiGhe());
                if(value instanceof LoaiPhong type) label.setText(type.tenLoaiPhong()+" · "+type.dinhDangChieu());
                if(value instanceof LoaiThoiDiem type) label.setText(type.tenLoaiThoiDiem()); return label;
            }
        });
    }
    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns,0) {
            @Override public boolean isCellEditable(int row,int column) { return false; }
            @Override public Class<?> getColumnClass(int column) { return getRowCount()>0 && getValueAt(0,column)!=null?getValueAt(0,column).getClass():Object.class; }
        };
    }
    private static String error(Throwable e) { return e instanceof IllegalArgumentException?e.getMessage():"Không thể đọc hoặc lưu giá vé/sản phẩm. Vui lòng kiểm tra kết nối và thử lại."; }
}
