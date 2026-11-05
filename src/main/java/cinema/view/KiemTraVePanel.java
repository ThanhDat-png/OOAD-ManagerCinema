package cinema.view;

import cinema.view.ui.*;

import cinema.controller.admission.*;
import cinema.controller.auth.PhienDangNhap;
import cinema.dao.VeDAO;
import cinema.model.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class KiemTraVePanel extends JPanel {
    static { Theme.install(); }
    private final VeBUS service;
    private final JTextField date=new JTextField(LocalDate.now(ZoneId.of("Asia/Bangkok")).toString(),12);
    private final JComboBox<PhongChieu> room=new JComboBox<>();
    private final JComboBox<SuatChieu> show=new JComboBox<>();
    private final JComboBox<String> codeType=new JComboBox<>(new String[]{"Mã vé","Mã QR"});
    private final JTextField code=new JTextField(30);
    private final JTextArea detail=new JTextArea();
    private final JLabel message=new JLabel(" ");
    private final JButton confirm=new JButton("Xác nhận");
    private final List<Component> controls=new ArrayList<>();
    private List<Phim> films=List.of();
    private boolean busy,updatingContext;
    private long revision;
    private LocalDate loadedDate;
    private Checked checked;
    private record Checked(String ticket,String room,int show) { }
    private record Context(List<PhongChieu> rooms,List<SuatChieu> shows,List<Phim> films,String selectedRoom,LocalDate date) { }

    @Override public void addNotify() { super.addNotify(); if(getClientProperty("cinema.page")==null){putClientProperty("cinema.page",true);Design.page(this,"Soát vé","Tra cứu và xác nhận vé vào phòng",message);} }

    public KiemTraVePanel(PhienDangNhap session) {
        super(new BorderLayout(8,8)); service=new VeBUS(session,new VeDAO());
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel top=new JPanel(new GridLayout(0,1,6,6)); JPanel location=new JPanel();
        location.add(new JLabel("Ngày (yyyy-MM-dd)")); location.add(date); location.add(new JLabel("Phòng đang kiểm soát")); location.add(room);
        controls.add(date); controls.add(room); button(location,"Tải suất chiếu",this::reloadContext); top.add(location);
        JPanel schedule=new JPanel(); schedule.add(new JLabel("Suất đang kiểm soát")); show.setPreferredSize(new Dimension(720,28)); schedule.add(show); controls.add(show); top.add(schedule);
        JPanel input=new JPanel(); input.add(codeType); input.add(code); controls.add(codeType); controls.add(code);
        button(input,"Tra cứu vé",this::lookup); button(input,"Đọc QR từ ảnh",this::readImage); top.add(input);
        JPanel actions=new JPanel(); button(actions,"Kiểm tra tính hợp lệ",this::check);
        confirm.setEnabled(false); confirm.addActionListener(event -> confirm()); actions.add(confirm);
        button(actions,"Hủy xác nhận",this::cancelCheck); top.add(actions);
        top.add(new JLabel("Cho phép vào từ "+service.window().truocPhut()+" phút trước đến "+service.window().sauPhut()+" phút sau giờ bắt đầu."));
        add(top,BorderLayout.NORTH); detail.setEditable(false); detail.setLineWrap(true); detail.setWrapStyleWord(true);
        add(new JScrollPane(detail),BorderLayout.CENTER); add(message,BorderLayout.SOUTH);
        show.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                if(value instanceof SuatChieu item) {
                    String name=films.stream().filter(p -> p.maPhim().equals(item.maPhim())).map(Phim::tenPhim).findFirst().orElse(item.maPhim());
                    label.setText(item.maSuat()+" · "+name+" · "+item.thoiGianBatDau().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
                return label;
            }
        });
        room.addActionListener(event -> { if(!updatingContext) { changed(); reloadContext(); } });
        show.addActionListener(event -> { if(!updatingContext) changed(); }); codeType.addActionListener(event -> changed());
        date.getDocument().addDocumentListener(listener(() -> { loadedDate=null; updatingContext=true; show.removeAllItems(); updatingContext=false; changed(); }));
        code.getDocument().addDocumentListener(listener(this::changed));
        date.addActionListener(event -> reloadContext()); code.addActionListener(event -> lookup()); reloadContext();
    }
    private void button(JPanel parent,String title,Runnable action) {
        JButton button=new JButton(title); controls.add(button); parent.add(button); button.addActionListener(event -> { if(!busy) action.run(); });
    }
    private void changed() { revision++; checked=null; confirm.setEnabled(false); detail.setText(""); message.setText("Vui lòng kiểm tra vé theo thông tin đã chọn"); }
    private void cancelCheck() { revision++; checked=null; confirm.setEnabled(false); message.setText("Đã hủy xác nhận. Trạng thái vé được giữ nguyên"); }
    private void reloadContext() {
        if(busy) return;
        LocalDate day;
        try { day=LocalDate.parse(date.getText().trim()); }
        catch(DateTimeParseException e) { message.setText("Ngày kiểm soát không hợp lệ"); return; }
        changed(); PhongChieu selected=(PhongChieu)room.getSelectedItem(); String previous=selected==null?null:selected.maPhong();
        run(() -> {
            List<PhongChieu> rooms=service.rooms();
            String id=rooms.stream().anyMatch(r -> r.maPhong().equals(previous))?previous:rooms.isEmpty()?null:rooms.get(0).maPhong();
            List<SuatChieu> shows=id==null?List.of():service.shows(day,id).stream().filter(s -> !"Đã hủy".equals(s.trangThai())).toList();
            return new Context(rooms,shows,service.films(),id,day);
        },data -> {
            updatingContext=true;
            try {
                films=data.films(); room.removeAllItems(); for(PhongChieu item:data.rooms()) room.addItem(item);
                for(int i=0;i<room.getItemCount();i++) if(room.getItemAt(i).maPhong().equals(data.selectedRoom())) room.setSelectedIndex(i);
                show.removeAllItems(); data.shows().forEach(show::addItem); loadedDate=data.date();
            } finally { updatingContext=false; }
            message.setText("Đã tải "+data.shows().size()+" suất chiếu. Chọn đúng phòng/suất trước khi kiểm tra vé");
        });
    }
    private VeDAO.LoaiMa type() { return codeType.getSelectedIndex()==0?VeDAO.LoaiMa.MA_VE:VeDAO.LoaiMa.MA_QR; }
    private Checked context() {
        PhongChieu selectedRoom=(PhongChieu)room.getSelectedItem(); SuatChieu selectedShow=(SuatChieu)show.getSelectedItem();
        if(loadedDate==null || selectedRoom==null || selectedShow==null) throw new IllegalArgumentException("Vui lòng tải và chọn phòng/suất đang kiểm soát");
        return new Checked(null,selectedRoom.maPhong(),selectedShow.maSuat());
    }
    private void lookup() {
        if(busy) return; checked=null; confirm.setEnabled(false); detail.setText(""); String text=code.getText(); VeDAO.LoaiMa type=type();
        run(() -> service.lookup(text,type),info -> { display(info); message.setText("Đã tra cứu vé. Chọn Kiểm tra tính hợp lệ để tiếp tục"); });
    }
    private void check() {
        if(busy) return;
        Checked location;
        try { location=context(); } catch(IllegalArgumentException e) { message.setText(e.getMessage()); return; }
        checked=null; confirm.setEnabled(false); String text=code.getText(); VeDAO.LoaiMa type=type();
        run(() -> service.check(text,type,location.room(),location.show()),result -> {
            display(result.thongTin()); message.setText(result.thongBao());
            if(result.hopLe()) checked=new Checked(result.thongTin().ve().maVe(),location.room(),location.show());
        });
    }
    private void confirm() {
        if(busy || checked==null) return;
        Checked target=checked;
        if(JOptionPane.showConfirmDialog(this,"Xác nhận cho khách vào và chuyển vé sang Đã sử dụng?","Xác nhận sử dụng vé",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) { cancelCheck(); return; }
        if(checked!=target) { message.setText("Thông tin đã thay đổi. Vui lòng kiểm tra vé lại"); return; }
        checked=null;
        run(() -> service.confirm(target.ticket(),target.room(),target.show()),info -> {
            display(info); message.setText("Xác nhận vé thành công");
        });
    }
    private void readImage() {
        JFileChooser chooser=new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter("Ảnh QR PNG/JPEG","png","jpg","jpeg"));
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        Path file=chooser.getSelectedFile().toPath(); changed();
        run(() -> DocMaQR.read(file),text -> { codeType.setSelectedIndex(1); code.setText(text); lookup(); });
    }
    private void display(VeDAO.ThongTin info) {
        Ve ticket=info.ve();
        detail.setText("Mã vé: "+ticket.maVe()+"\nPhim: "+info.tenPhim()+"\nPhòng: "+info.tenPhong()
                +"\nSuất chiếu: "+ticket.maSuat()+"\nBắt đầu: "+info.batDau().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                +"\nGhế: "+ticket.hangGheLucPhatHanh()+ticket.soGheLucPhatHanh()+" · "+ticket.tenLoaiGheLucPhatHanh()
                +"\nGiá vé: "+ticket.giaVe().toPlainString()+" VND\nThanh toán: "+(info.daThanhToan()?"Đã thanh toán":"Chưa có thanh toán hợp lệ")
                +"\nTrạng thái vé: "+ticket.trangThai()); detail.setCaretPosition(0);
    }
    private <T> void run(Callable<T> task,Consumer<T> success) {
        if(busy) return; long expected=revision; busy=true; controls.forEach(c -> c.setEnabled(false)); confirm.setEnabled(false); message.setText("Đang xử lý...");
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy=false; controls.forEach(c -> c.setEnabled(true));
                try { T value=get(); if(expected==revision) success.accept(value); }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); checked=null; detail.setText(""); message.setText("Tác vụ bị gián đoạn. Vui lòng tra cứu lại"); }
                catch(ExecutionException e) {
                    checked=null; detail.setText(""); message.setText(e.getCause() instanceof IllegalArgumentException?e.getCause().getMessage():
                            "Chưa có kết quả xử lý. Vui lòng kiểm tra kết nối và tra cứu lại trước khi cho khách vào");
                } finally { confirm.setEnabled(!busy && checked!=null); }
            }
        }.execute();
    }
    private static DocumentListener listener(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }
}
