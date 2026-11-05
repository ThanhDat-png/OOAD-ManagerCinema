package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.revenue.*;
import cinema.dao.BaoCaoDAO;
import cinema.model.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

public final class BaoCaoPanel extends JPanel {
    static { Theme.install(); }
    private final BaoCaoBUS service;
    private final JLabel status=new JLabel(" ");
    private final Map<Component,Boolean> enabled=new IdentityHashMap<>();
    private final JComboBox<Muc> film=choices("film.filter"),employee=choices("staff.filter"),ticketFilm=choices("tickets.film"),show=choices("tickets.show"),product=choices("stock.product");
    private boolean busy;
    private record Muc(String ma,String ten){@Override public String toString(){return ma==null?"Tất cả":ma+" · "+ten;}}
    private record Data(String[] columns,List<Object[]> rows,String summary){ }

    @Override public void addNotify() { super.addNotify(); if(getClientProperty("cinema.page")==null){putClientProperty("cinema.page",true);Design.page(this,"Báo cáo & thống kê","Dữ liệu đã thanh toán · Chỉ đọc · Giá trị lịch sử",status);} }

    public BaoCaoPanel(PhienDangNhap session) {
        super(new BorderLayout(8,8));service=new BaoCaoBUS(session,new BaoCaoDAO());
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JTabbedPane tabs=new JTabbedPane();tabs.addTab("Doanh thu theo thời gian",timeTab());tabs.addTab("Doanh thu theo phim",filmTab());
        tabs.addTab("Doanh thu theo nhân viên",staffTab());tabs.addTab("Vé và tồn kho F&B",ticketsAndStockTab());add(tabs);add(status,BorderLayout.SOUTH);
        run(service::danhMuc,data->{
            for(Phim item:data.phim()){film.addItem(new Muc(item.maPhim(),item.tenPhim()));ticketFilm.addItem(new Muc(item.maPhim(),item.tenPhim()));}
            for(NhanVien item:data.nhanVien())employee.addItem(new Muc(item.maNV(),item.hoTen()));
            Map<String,String> names=new HashMap<>();data.phim().forEach(p->names.put(p.maPhim(),p.tenPhim()));
            for(SuatChieu item:data.suat())show.addItem(new Muc(Integer.toString(item.maSuat()),names.getOrDefault(item.maPhim(),item.maPhim())+" · "+item.maPhong()+" · "+item.thoiGianBatDau()));
            for(SanPham item:data.sanPham())product.addItem(new Muc(item.maSP(),item.tenSP()));status.setText("Đã tải danh mục. Chọn tiêu chí rồi bấm Thống kê.");
        });
    }
    private JPanel timeTab(){
        Report report=new Report("time");Range dates=new Range("time",true);report.filters.add(dates);
        report.note.setText("Theo ngày thanh toán. Vé bán tính theo ghế đã thanh toán; hiển thị riêng phần chờ phát hành.");
        report.action(()->{var range=dates.read();report.submit(()->{
            var value=service.theoThoiGian(range);
            return new Data(new String[]{"Số giao dịch","Vé bán","Vé đã phát hành","Vé chờ phát hành","Doanh thu (VND)"},List.<Object[]>of(new Object[]{value.soGiaoDich(),value.soVeBan(),value.soVeDaPhatHanh(),value.soVeChoPhatHanh(),value.doanhThu()}),range+" · "+value.giaoDichChoVe()+" giao dịch còn chờ phát hành vé");
        });});report.watch();return report;
    }
    private JPanel filmTab(){
        Report report=new Report("film");Range dates=new Range("film",false);report.filters.add(dates);report.filters.add(new JLabel("Phim"));report.filters.add(film);
        report.note.setText("Theo ngày thanh toán, cộng giá từng vé đã phát hành; giá lịch sử không đổi theo bảng giá hiện tại.");
        report.action(()->{var range=dates.read();String id=selected(film);report.submit(()->{
            var result=service.theoPhim(range,id);List<Object[]> rows=new ArrayList<>();long count=0;BigDecimal total=BigDecimal.ZERO;
            for(var row:result){rows.add(new Object[]{row.maPhim(),row.tenPhim(),row.soVe(),row.doanhThu()});count+=row.soVe();total=total.add(row.doanhThu());}
            return new Data(new String[]{"Mã phim","Tên phim","Vé đã phát hành","Doanh thu (VND)"},rows,range+" · "+count+" vé · "+money(total)+" VND. Giao dịch chưa có vé được tính ở báo cáo thời gian/nhân viên.");
        });});report.watch();return report;
    }
    private JPanel staffTab(){
        Report report=new Report("staff");Range dates=new Range("staff",false);report.filters.add(dates);report.filters.add(new JLabel("Nhân viên"));report.filters.add(employee);
        report.note.setText("Theo ngày thanh toán và nhân viên được lưu trên giao dịch. Mỗi khoản thanh toán chỉ cộng một lần.");
        report.action(()->{var range=dates.read();String id=selected(employee);report.submit(()->{
            var result=service.theoNhanVien(range,id);List<Object[]> rows=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;
            for(var row:result){var value=row.tongHop();rows.add(new Object[]{row.maNV(),row.hoTen(),value.soGiaoDich(),value.soVeBan(),value.soVeDaPhatHanh(),value.soVeChoPhatHanh(),value.doanhThu()});total=total.add(value.doanhThu());}
            return new Data(new String[]{"Mã nhân viên","Họ tên","Số giao dịch","Vé bán","Vé đã phát hành","Vé chờ phát hành","Doanh thu (VND)"},rows,range+" · Tổng doanh thu: "+money(total)+" VND");
        });});report.watch();return report;
    }
    private JPanel ticketsAndStockTab(){
        JPanel result=new JPanel(new BorderLayout(8,8));JComboBox<String> type=new JComboBox<>(new String[]{"Chọn loại thống kê","Vé","F&B"});type.setName("summary.type");JPanel choose=new JPanel();choose.add(new JLabel("Loại thống kê"));choose.add(type);result.add(choose,BorderLayout.NORTH);
        CardLayout layout=new CardLayout();JPanel cards=new JPanel(layout);JPanel empty=new JPanel();empty.add(new JLabel("Vui lòng chọn Vé hoặc F&B để xem tiêu chí thống kê."));cards.add(empty,"0");
        Report tickets=new Report("tickets");Range dates=new Range("tickets",false);tickets.filters.add(dates);tickets.filters.add(new JLabel("Phim"));tickets.filters.add(ticketFilm);tickets.filters.add(new JLabel("Suất"));show.setPreferredSize(new Dimension(330,26));tickets.filters.add(show);
        tickets.note.setText("Theo thời gian phát hành vé, khác thời gian thanh toán. Trạng thái là trạng thái hiện tại của vé.");
        tickets.action(()->{var range=dates.read();String movie=selected(ticketFilm),slot=selected(show);Integer showId=slot==null?null:Integer.valueOf(slot);tickets.submit(()->{
            var data=service.ve(range,movie,showId);return new Data(new String[]{"Tổng vé phát hành","Chưa sử dụng","Đã sử dụng"},List.<Object[]>of(new Object[]{data.tongVe(),data.chuaSuDung(),data.daSuDung()}),range+" · Thời gian phát hành vé");
        });});tickets.watch();cards.add(tickets,"1");
        Report stock=new Report("stock");JComboBox<String> state=new JComboBox<>(new String[]{"Tất cả","Đang kinh doanh","Ngừng kinh doanh"}),available=new JComboBox<>(new String[]{"Tất cả","Còn hàng","Hết hàng"});state.setName("stock.state");available.setName("stock.available");stock.filters.add(new JLabel("Sản phẩm"));stock.filters.add(product);stock.filters.add(new JLabel("Kinh doanh"));stock.filters.add(state);stock.filters.add(new JLabel("Tồn kho"));stock.filters.add(available);
        stock.note.setText("Thông tin tồn kho hiện tại; không có doanh thu hoặc số lượng F&B đã bán trong phạm vi báo cáo.");
        stock.action(()->{String id=selected(product),condition=state.getSelectedIndex()==0?null:(String)state.getSelectedItem();Boolean inStock=available.getSelectedIndex()==0?null:available.getSelectedIndex()==1;stock.submit(()->{
            var data=service.sanPham(id,condition,inStock);List<Object[]> rows=new ArrayList<>();for(var row:data)rows.add(new Object[]{row.maSP(),row.tenSP(),row.donViTinh(),row.tinhTrangKinhDoanh(),row.soLuongTon()});
            return new Data(new String[]{"Mã sản phẩm","Tên sản phẩm","Đơn vị tính","Tình trạng kinh doanh","Tồn kho"},rows,data.isEmpty()?"Không có sản phẩm phù hợp":data.size()+" sản phẩm · Tồn kho hiện tại");
        });});stock.watch();cards.add(stock,"2");
        type.addActionListener(event->{tickets.clear();stock.clear();layout.show(cards,Integer.toString(type.getSelectedIndex()));});result.add(cards);return result;
    }
    private final class Report extends JPanel {
        private final JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));
        private final JLabel note=new JLabel(" "),summary=new JLabel("Chưa có kết quả thống kê");
        private final DefaultTableModel model=new DefaultTableModel(){@Override public boolean isCellEditable(int row,int column){return false;}@Override public Class<?> getColumnClass(int column){return getRowCount()>0 && getValueAt(0,column)!=null?getValueAt(0,column).getClass():Object.class;}};
        private final JTable table=new JTable(model);
        private final JButton submit=new JButton("Thống kê");
        private long revision;
        Report(String name){
            super(new BorderLayout(8,8));filters.setName(name+".filters");table.setName(name+".table");submit.setName(name+".run");summary.setName(name+".message");
            JPanel top=new JPanel(new BorderLayout());top.add(filters);top.add(note,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
            table.setAutoCreateRowSorter(true);table.setRowHeight(26);table.setDefaultRenderer(BigDecimal.class,new javax.swing.table.DefaultTableCellRenderer(){@Override protected void setValue(Object value){setHorizontalAlignment(SwingConstants.RIGHT);setText(value instanceof BigDecimal amount?money(amount):"");}});
            add(new ReportVisuals(table,summary,name));add(summary,BorderLayout.SOUTH);
        }
        void action(Runnable action){submit.addActionListener(event->{try{action.run();}catch(RuntimeException e){clear();summary.setText(message(e));}});filters.add(submit);}
        void clear(){revision++;model.setRowCount(0);summary.setText("Tiêu chí đã thay đổi. Bấm Thống kê để xem kết quả.");}
        void submit(Callable<Data> work){clear();long requested=revision;summary.setText("Đang thống kê...");run(work,data->{if(requested!=revision)return;model.setDataVector(data.rows().toArray(Object[][]::new),data.columns());summary.setText(data.summary());},error->{if(requested==revision)summary.setText(message(error));});}
        void watch(){for(Component component:components(filters)){
            if(component instanceof JTextField field)field.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){clear();}public void removeUpdate(DocumentEvent e){clear();}public void changedUpdate(DocumentEvent e){clear();}});
            if(component instanceof JComboBox<?> combo)combo.addActionListener(event->clear());
        }}
    }
    private static final class Range extends JPanel {
        private final JComboBox<String> mode;
        private final JTextField start=new JTextField(10),end=new JTextField(10);
        private final JLabel label=new JLabel("Từ ngày yyyy-MM-dd"),endLabel=new JLabel("Đến ngày");
        Range(String name,boolean periods){
            super(new FlowLayout(FlowLayout.LEFT,6,0));start.setName(name+".from");end.setName(name+".to");
            mode=new JComboBox<>(new String[]{"Ngày","Tháng","Năm","Khoảng ngày"});mode.setName(name+".period");
            if(periods){add(mode);mode.addActionListener(event->update());}else mode.setSelectedIndex(3);
            add(label);add(start);add(endLabel);add(end);start.putClientProperty("cinema.periodSelector",mode);DateInputs.attach(start,false,false);DateInputs.attach(end,false,false);update();
        }
        private void update(){int index=mode.getSelectedIndex();label.setText(switch(index){case 0->"Ngày yyyy-MM-dd";case 1->"Tháng yyyy-MM";case 2->"Năm yyyy";default->"Từ ngày yyyy-MM-dd";});end.setVisible(index==3);endLabel.setVisible(index==3);start.setText("");end.setText("");}
        KhoangThongKe read(){
            if(start.getText().isBlank() || mode.getSelectedIndex()==3 && end.getText().isBlank())throw new IllegalArgumentException("Vui lòng chọn đầy đủ thời gian thống kê");
            try {String first=start.getText().trim();return switch(mode.getSelectedIndex()){
                case 0->KhoangThongKe.ngay(LocalDate.parse(first));case 1->KhoangThongKe.thang(YearMonth.parse(first));case 2->KhoangThongKe.nam(Integer.parseInt(first));default->new KhoangThongKe(LocalDate.parse(first),LocalDate.parse(end.getText().trim()));
            };}catch(DateTimeParseException|NumberFormatException e){throw new IllegalArgumentException("Thời gian không đúng định dạng đang hiển thị");}
        }
    }
    private static JComboBox<Muc> choices(String name){JComboBox<Muc> box=new JComboBox<>();box.setName(name);box.addItem(new Muc(null,"Tất cả"));box.setPreferredSize(new Dimension(250,26));return box;}
    private static String selected(JComboBox<Muc> combo){return ((Muc)combo.getSelectedItem()).ma();}
    private static String money(BigDecimal amount){return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount);}
    private static String message(Throwable e){return e instanceof IllegalArgumentException?e.getMessage():"Không tải được thống kê. Kiểm tra kết nối CSDL rồi thử lại.";}
    private static List<Component> components(Container root){List<Component> result=new ArrayList<>();for(Component child:root.getComponents()){result.add(child);if(child instanceof Container container)result.addAll(components(container));}return result;}
    private void disable(Component component){enabled.put(component,component.isEnabled());component.setEnabled(false);if(component instanceof Container container)for(Component child:container.getComponents())disable(child);}
    private <T> void run(Callable<T> work,Consumer<T> done){run(work,done,e->{});}
    private <T> void run(Callable<T> work,Consumer<T> done,Consumer<Throwable> failed){
        if(busy)return;busy=true;enabled.clear();disable(this);status.setText("Đang tải dữ liệu...");
        new SwingWorker<T,Void>(){
            @Override protected T doInBackground()throws Exception{return work.call();}
            @Override protected void done(){busy=false;enabled.forEach(Component::setEnabled);enabled.clear();try{T value=get();status.setText("Đã tải thống kê. Dữ liệu gốc được giữ nguyên.");done.accept(value);}catch(Exception e){Throwable cause=e.getCause()==null?e:e.getCause();status.setText(message(cause));failed.accept(cause);}}
        }.execute();
    }
}
