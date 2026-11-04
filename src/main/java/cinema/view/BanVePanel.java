package cinema.view;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.sales.*;
import cinema.dao.*;
import cinema.model.*;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;

/** UC08: all database work runs off the event dispatch thread. */
public final class BanVePanel extends JPanel {
    private final BanVeBUS service;
    private final JTextField film=new JTextField(18),date=new JTextField(10),time=new JTextField(6);
    private final JComboBox<BanVeDAO.SuatBan> shows=new JComboBox<>();
    private final JPanel seats=new JPanel();
    private final JTextArea cartText=new JTextArea(),historyText=new JTextArea();
    private final JLabel message=new JLabel(" "),identity=new JLabel();
    private final JTextField code=new JTextField(22),from=new JTextField(10),to=new JTextField(10);
    private final DefaultListModel<GiaoDichDAO.Dong> historyModel=new DefaultListModel<>();
    private final JList<GiaoDichDAO.Dong> history=new JList<>(historyModel);
    private final JTabbedPane tabs=new JTabbedPane();
    private final Set<Integer> selected=new HashSet<>();
    private final javax.swing.Timer refresh;
    private BanVeDAO.GioHang cart,inspected;
    private String id=BanVeBUS.newId();
    private Integer showId;
    private boolean busy,paymentOpen,needsCheck;
    private final Map<Component,Boolean> enabled=new IdentityHashMap<>();

    public BanVePanel(PhienDangNhap session) {
        super(new BorderLayout(8,8)); service=new BanVeBUS(session,new BanVeDAO()); service.employeeId();
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); seats.setLayout(new BoxLayout(seats,BoxLayout.Y_AXIS));
        JPanel selling=new JPanel(new BorderLayout(6,6)),top=new JPanel(new GridLayout(0,1));
        JPanel filters=new JPanel(); filters.add(new JLabel("Phim")); filters.add(film); filters.add(new JLabel("Ngày yyyy-MM-dd")); filters.add(date);
        filters.add(new JLabel("Giờ HH:mm")); filters.add(time); button(filters,"Tìm suất",this::search); top.add(filters);
        JPanel schedule=new JPanel(); shows.setPreferredSize(new Dimension(750,28)); schedule.add(shows); button(schedule,"Chọn suất",this::choose); top.add(schedule);
        top.add(identity); top.add(new JLabel("Giữ ghế "+service.holdMinutes()+" phút, tối đa đến giờ bắt đầu. Đang thanh toán sẽ tiếp tục giữ ghế."));
        selling.add(top,BorderLayout.NORTH);
        cartText.setEditable(false); cartText.setLineWrap(true); cartText.setWrapStyleWord(true);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JScrollPane(seats),new JScrollPane(cartText)); split.setResizeWeight(.6); selling.add(split);
        JPanel actions=new JPanel(new GridLayout(2,5,4,4)); button(actions,"Kiểm tra mã hiện tại",this::recover); button(actions,"Cập nhật ghế",this::reload); button(actions,"Giữ ghế / đổi ghế",this::hold);
        button(actions,"Tính tiền",()->{if(savedSelection())run(()->service.quote(id),this::display);});
        button(actions,"Thanh toán / tiếp tục",this::payment);
        button(actions,"Hủy giao dịch",this::cancel);
        button(actions,"Phát hành / in lại",()->run(()->service.issue(id),this::preview));
        button(actions,"Giao dịch mới",this::newTransaction); selling.add(actions,BorderLayout.SOUTH);
        tabs.addTab("Bán vé",selling); tabs.addTab("Kiểm tra giao dịch",historyPanel()); add(tabs); add(message,BorderLayout.SOUTH);
        refresh=new javax.swing.Timer(5000,event->{if(!busy && !paymentOpen && tabs.getSelectedIndex()==0 && showId!=null) reload();});
        identity(); search();
    }
    @Override public void addNotify() { super.addNotify(); if(refresh!=null) refresh.start(); }
    @Override public void removeNotify() { refresh.stop(); super.removeNotify(); }
    private JPanel historyPanel() {
        JPanel result=new JPanel(new BorderLayout()); JPanel filters=new JPanel();
        filters.add(new JLabel("Mã giao dịch / vé")); filters.add(code); filters.add(new JLabel("Từ ngày")); filters.add(from); filters.add(new JLabel("Đến ngày")); filters.add(to);
        button(filters,"Tra cứu",()->{
            try { var a=day(from); var b=day(to); String input=code.getText();
                run(()->service.transactions(input,a,b),rows->{historyModel.clear(); inspected=null; historyText.setText(""); rows.forEach(historyModel::addElement); message.setText(rows.isEmpty()?"Không tìm thấy giao dịch phù hợp":"Tìm thấy "+rows.size()+" giao dịch");});
            } catch(RuntimeException e) { error(e); }
        }); result.add(filters,BorderLayout.NORTH);
        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean sel,boolean focus) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,sel,focus);
                if(value instanceof GiaoDichDAO.Dong row) label.setText(row.giaoDich().maGiaoDich()+" · "+row.giaoDich().trangThai()+" · "+row.hoTenNhanVien()); return label;
            }
        });
        history.addListSelectionListener(event->{if(!event.getValueIsAdjusting() && !busy && history.getSelectedValue()!=null) {
            String key=history.getSelectedValue().giaoDich().maGiaoDich(); inspected=null;
            run(()->new Detail(service.inspect(key),service.savedTickets(key)),detail->{inspected=detail.cart(); historyText.setText(description(inspected)+"\nVé đã lưu:\n"+detail.tickets().stream().map(v->v.ve().maVe()+" · "+v.ve().trangThai()).reduce("",(a,b)->a+b+"\n"));});
        }});
        historyText.setEditable(false); JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JScrollPane(history),new JScrollPane(historyText)); split.setResizeWeight(.45); result.add(split);
        JPanel actions=new JPanel(); button(actions,"Tiếp tục giao dịch đã chọn",()->{
            if(inspected==null) {error(new IllegalArgumentException("Vui lòng chọn giao dịch"));return;}
            if(!inspected.giaoDich().maNV().equalsIgnoreCase(service.employeeId())) {error(new IllegalArgumentException("Chỉ nhân viên tạo được tiếp tục giao dịch"));return;}
            if(!id.equals(inspected.giaoDich().maGiaoDich()) && !canLeave()) return;
            display(inspected); showId=cart.suat()==null?null:cart.suat().maSuat(); selected.clear(); cart.ghe().forEach(g->selected.add(g.ghe().maGhe())); tabs.setSelectedIndex(0); if(showId!=null) reload();
        });
        button(actions,"In lại vé đã lưu",()->{if(inspected!=null) {String key=inspected.giaoDich().maGiaoDich();run(()->service.printableTickets(key),this::preview);}}); result.add(actions,BorderLayout.SOUTH); return result;
    }
    private record Detail(BanVeDAO.GioHang cart,List<VeDAO.ThongTin> tickets) { }
    private record MapResult(List<GheSuatChieu> seats,BanVeDAO.GioHang cart) { }
    private void recover() {
        String key=id;
        run(()->{
            try {return service.inspect(key);}
            catch(IllegalArgumentException e) {if("Không tìm thấy giao dịch".equals(e.getMessage()))return null;throw e;}
        },value->{
            needsCheck=false;
            if(value==null) {cart=null;cartText.setText("Chưa có giao dịch được lưu với mã "+key+". Có thể chọn/giữ ghế lại.");message.setText("Đã kiểm tra: chưa có giao dịch với mã hiện tại");}
            else {display(value);showId=value.suat()==null?null:value.suat().maSuat();selected.clear();value.ghe().forEach(g->selected.add(g.ghe().maGhe()));if(showId!=null)reload();}
        });
    }
    private void search() {
        try { String name=film.getText(); LocalDate d=day(date); LocalTime t=time.getText().isBlank()?null:LocalTime.parse(time.getText().trim());
            run(()->service.search(name,d,t),rows->{shows.removeAllItems();rows.forEach(shows::addItem); message.setText(rows.isEmpty()?"Không tìm thấy suất chiếu phù hợp":"Tìm thấy "+rows.size()+" suất chiếu");});
        } catch(RuntimeException e) {error(e);}
    }
    private void choose() {
        var choice=(BanVeDAO.SuatBan)shows.getSelectedItem(); if(choice==null) return;
        if(showId!=null && showId==choice.suat().maSuat()) {reload();return;}
        if(!canLeave()) return;
        if(cart!=null) {error(new IllegalArgumentException("Hủy giao dịch cũ hoặc tạo giao dịch mới trước khi đổi suất"));return;}
        int next=choice.suat().maSuat();run(()->service.selectShow(next),rows->{showId=next;selected.clear();renderSeats(rows);});
    }
    private void reload() {
        if(showId==null) return; int show=showId; String key=id; boolean existing=cart!=null;
        run(()->new MapResult(service.seats(show),existing?service.inspect(key):null),data->{
            if(data.cart()!=null) display(data.cart()); renderSeats(data.seats());
        });
    }
    private void renderSeats(List<GheSuatChieu> rows) {
        seats.removeAll(); boolean editable=cart==null || "Chờ thanh toán".equals(cart.giaoDich().trangThai());
        Set<Integer> selectable=new HashSet<>();
        JLabel screen=new JLabel("MÀN HÌNH — Xanh lá: trống · Vàng: đang giữ · Đỏ: đã bán · Xám: không khả dụng");screen.setAlignmentX(Component.LEFT_ALIGNMENT);seats.add(screen);
        Map<String,JPanel> rowsByName=new LinkedHashMap<>();
        for(var row:rows) {
            var g=row.ghe(); boolean own=id.equals(row.maGiaoDich());
            boolean free="Sẵn sàng sử dụng".equals(g.trangThai()) && ("Trống".equals(row.trangThai()) || own && "Đang giữ".equals(row.trangThai()));
            if(free) selectable.add(g.maGhe());
            String state=!"Sẵn sàng sử dụng".equals(g.trangThai())?"Không khả dụng":row.trangThai();
            JPanel line=rowsByName.computeIfAbsent(g.hangGhe(),name->{JPanel panel=new JPanel(new FlowLayout(FlowLayout.LEFT,6,6));panel.setAlignmentX(Component.LEFT_ALIGNMENT);panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,60));panel.add(new JLabel("Hàng "+name));seats.add(panel);return panel;});
            JToggleButton chair=new JToggleButton(g.hangGhe()+g.soGhe(),selected.contains(g.maGhe()));chair.setPreferredSize(new Dimension(82,42));
            chair.setBackground(switch(state){case "Trống"->new Color(176,222,181);case "Đang giữ"->new Color(247,221,134);case "Đã bán"->new Color(233,163,163);default->Color.LIGHT_GRAY;});
            chair.putClientProperty("seatId",g.maGhe()); chair.setEnabled(editable && free); chair.setToolTipText(g.loaiGhe().tenLoaiGhe()+" · "+state+(own?" · Giao dịch này":""));
            chair.addActionListener(event->{if(chair.isSelected()) selected.add(g.maGhe());else selected.remove(g.maGhe());message.setText("Bấm Giữ ghế / đổi ghế để xác nhận lựa chọn trước khi tính tiền/thanh toán");}); line.add(chair);
        }
        selected.retainAll(selectable); seats.revalidate();seats.repaint();
        if(selectable.isEmpty()) message.setText("Suất chiếu đã hết chỗ hoặc không còn ghế khả dụng");
    }
    private boolean savedSelection() {
        if(cart==null || "Chờ thanh toán".equals(cart.giaoDich().trangThai())) {
            Set<Integer> held=new HashSet<>();if(cart!=null)cart.ghe().forEach(g->held.add(g.ghe().maGhe()));
            if(cart==null || !held.equals(selected)) {error(new IllegalArgumentException("Vui lòng bấm Giữ ghế / đổi ghế để xác nhận lựa chọn trước"));return false;}
        }
        return true;
    }
    private void hold() {
        if(showId==null) {error(new IllegalArgumentException("Vui lòng chọn suất chiếu"));return;}
        String key=id; int show=showId; Set<Integer> choice=Set.copyOf(selected);
        needsCheck=true;
        run(()->service.hold(key,show,choice),value->{display(value);reload();});
    }
    private void payment() {
        if(!savedSelection())return;
        run(()->service.beginPayment(id),value->{display(value);if("Đã thanh toán".equals(value.giaoDich().trangThai())) {run(()->service.issue(id),this::preview);return;} paymentDialog();});
    }
    private void paymentDialog() {
        paymentOpen=true; JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"Xác nhận thanh toán tại quầy",Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content=new JPanel(new GridLayout(0,1,8,8));content.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        content.add(new JLabel("Giao dịch "+id+" · Tổng tiền "+cart.giaoDich().tongTien().toPlainString()+" VND"));
        JComboBox<String> method=new JComboBox<>();method.addItem("Chọn phương thức");BanVeBUS.PHUONG_THUC.forEach(method::addItem);content.add(method);
        JTextField amount=new JTextField(cart.giaoDich().tongTien().toPlainString());content.add(new JLabel("Số tiền đã nhận / xác nhận"));content.add(amount);
        JCheckBox received=new JCheckBox("Đã nhận đủ tiền hoặc đã kiểm tra thanh toán thành công tại quầy");content.add(received);
        content.add(new JLabel("Đóng cửa sổ sẽ tiếp tục giữ ghế. Kiểm tra giao dịch nếu kết quả chưa rõ."));
        JButton finish=new JButton("Hoàn tất giao dịch"); content.add(finish);
        finish.addActionListener(event->{
            final BigDecimal money;try {money=new BigDecimal(amount.getText().trim());}catch(NumberFormatException e){error(new IllegalArgumentException("Số tiền không hợp lệ"));return;}
            String key=id,chosen=(String)method.getSelectedItem();boolean confirmed=received.isSelected();
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);finish.setEnabled(false);method.setEnabled(false);amount.setEnabled(false);received.setEnabled(false);
            run(()->service.complete(key,chosen,money,confirmed),value->{display(value);dialog.dispose();message.setText("Thanh toán thành công. Đang phát hành vé...");run(()->service.issue(key),this::preview);},()->{
                if(dialog.isDisplayable()) {dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);finish.setEnabled(true);method.setEnabled(true);amount.setEnabled(true);received.setEnabled(true);}
            });
        });
        dialog.setContentPane(content);dialog.pack();dialog.setLocationRelativeTo(this);dialog.setVisible(true);paymentOpen=false;
    }
    private void cancel() {
        if(cart==null) {error(new IllegalArgumentException("Chưa có giao dịch để hủy; nếu lần giữ ghế bị lỗi, hãy tra cứu mã đang hiển thị"));return;}
        boolean processing="Đang thanh toán".equals(cart.giaoDich().trangThai());
        String prompt=processing?"Chỉ hủy khi chắc chắn CHƯA nhận tiền và thanh toán chưa thành công. Xác nhận hủy?":"Hủy giao dịch và giải phóng ghế?";
        if(JOptionPane.showConfirmDialog(this,prompt,"Hủy giao dịch",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        String key=id;run(()->{if(processing)service.cancelPaymentBeforeReceivingMoney(key);else service.cancel(key);return service.inspect(key);},value->{display(value);selected.clear();reload();});
    }
    private boolean canLeave() {
        if(needsCheck) {error(new IllegalArgumentException("Cần tra cứu mã "+id+" để xác định kết quả giữ ghế trước khi chuyển giao dịch"));return false;}
        if(cart!=null && List.of("Chờ thanh toán","Đang thanh toán").contains(cart.giaoDich().trangThai())) {error(new IllegalArgumentException("Hoàn tất hoặc hủy giao dịch hiện tại trước. Nếu chưa rõ kết quả, tra cứu mã "+id));return false;}
        return true;
    }
    private void newTransaction() {if(!canLeave())return;cart=null;id=BanVeBUS.newId();selected.clear();cartText.setText("");identity();if(showId!=null)reload();}
    public boolean requestClose() {
        if(busy || paymentOpen) return false;
        if(needsCheck || cart!=null && List.of("Chờ thanh toán","Đang thanh toán").contains(cart.giaoDich().trangThai()))
            return JOptionPane.showConfirmDialog(this,"Giao dịch "+id+" vẫn đang xử lý. Ghế đang thanh toán không tự giải phóng; dùng Kiểm tra giao dịch để tiếp tục. Đóng cửa sổ?","Giao dịch chưa hoàn tất",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
        return true;
    }
    private void display(BanVeDAO.GioHang value) {cart=value;needsCheck=false;id=value.giaoDich().maGiaoDich();identity();cartText.setText(description(value));
        if(!"Chờ thanh toán".equals(value.giaoDich().trangThai()))for(Component row:seats.getComponents())if(row instanceof Container container)for(Component chair:container.getComponents())if(chair instanceof JToggleButton button){button.setEnabled(false);Object key=button.getClientProperty("seatId");if(value.ghe().stream().anyMatch(g->Objects.equals(key,g.ghe().maGhe()) && "Đã bán".equals(g.trangThai()))){button.setSelected(false);button.setBackground(new Color(233,163,163));button.setToolTipText("Đã bán · Giao dịch này");}}}
    private void identity() {identity.setText("Mã giao dịch hiện tại: "+id+" — Lưu mã này để tra cứu nếu mất kết nối.");}
    private static String description(BanVeDAO.GioHang value) {
        var g=value.giaoDich();StringBuilder text=new StringBuilder("Giao dịch: "+g.maGiaoDich()+"\nTạo lúc: "+g.thoiGianTao()+"\nNhân viên: "+g.maNV()+"\nTrạng thái: "+g.trangThai()+"\nPhim: "+value.tenPhim()+"\n");
        if(value.suat()!=null)text.append("Suất: ").append(value.suat().maSuat()).append(" · ").append(value.suat().thoiGianBatDau()).append("\nPhòng: ").append(value.phong().tenPhong()).append('\n');
        for(var row:value.ghe()) text.append("Ghế ").append(row.ghe().hangGhe()).append(row.ghe().soGhe()).append(" · ").append(row.trangThai()).append(" · ").append(row.giaVeChot()==null?"Chưa tính tiền":row.giaVeChot().toPlainString()+" VND").append(row.thoiGianHetHanGiu()==null?"":" · Giữ đến "+row.thoiGianHetHanGiu()).append('\n');
        text.append("Tổng tiền: ").append(g.tongTien().toPlainString()).append(" VND\n");
        if(value.thanhToan()!=null)text.append("Thanh toán: ").append(value.thanhToan().phuongThuc()).append(" · ").append(value.thanhToan().soTien()).append(" · ").append(value.thanhToan().thoiGianThanhToan());
        return text.toString();
    }
    private void preview(List<VeDAO.ThongTin> tickets) {
        if(tickets.isEmpty()) {message.setText("Chưa có vé được phát hành");return;}
        JTabbedPane pages=new JTabbedPane();for(var ticket:tickets)pages.addTab(ticket.ve().maVe(),new JScrollPane(new JLabel(new ImageIcon(InVe.preview(ticket)))));
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"Vé đã lưu",Dialog.ModalityType.MODELESS);dialog.add(pages);
        JButton print=new JButton("In vé đã lưu");dialog.add(print,BorderLayout.SOUTH);
        print.addActionListener(event->{PrinterJob job=PrinterJob.getPrinterJob();job.setPrintable(new InVe(tickets));if(job.printDialog()) {print.setEnabled(false);run(()->{job.print();return true;},done->message.setText("Đã gửi vé tới máy in"),()->print.setEnabled(true));}});
        dialog.setSize(520,700);dialog.setLocationRelativeTo(this);dialog.setVisible(true);
    }
    private static LocalDate day(JTextField field) {return field.getText().isBlank()?null:LocalDate.parse(field.getText().trim());}
    private void button(JPanel target,String label,Runnable action) {JButton button=new JButton(label);button.addActionListener(event->action.run());target.add(button);}
    private void error(Throwable e) {message.setText(e instanceof IllegalArgumentException || e instanceof java.time.format.DateTimeParseException?e.getMessage():"Không hoàn tất thao tác. Hãy tra cứu mã "+id+" trước khi thực hiện lại.");}
    private void disable(Component component) {enabled.put(component,component.isEnabled());component.setEnabled(false);if(component instanceof Container container)for(Component child:container.getComponents())disable(child);}
    private <T> void run(Callable<T> work,Consumer<T> done) {run(work,done,()->{});}
    private <T> void run(Callable<T> work,Consumer<T> done,Runnable always) {
        if(busy)return;busy=true;enabled.clear();disable(this);message.setText("Đang xử lý...");
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception {return work.call();}
            @Override protected void done() {
                busy=false;enabled.forEach(Component::setEnabled);enabled.clear();
                try {T value=get();message.setText("Đã hoàn tất");done.accept(value);}catch(Exception e){Throwable cause=e.getCause()==null?e:e.getCause();error(cause);}finally{always.run();}
            }
        }.execute();
    }
}
