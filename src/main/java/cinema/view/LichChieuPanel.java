package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.schedule.LichChieuBUS;
import cinema.dao.SuatChieuDAO;
import cinema.model.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class LichChieuPanel extends JPanel {
    static { Theme.install(); }
    private final LichChieuBUS service;
    private final JTextField dateFilter=new JTextField(12);
    private final JComboBox<String> roomFilter=new JComboBox<>();
    private final JLabel message=new JLabel(" ");
    private final List<Component> controls=new ArrayList<>();
    private final DefaultTableModel model=new DefaultTableModel(
            new String[]{"Mã suất","Phim","Phòng","Bắt đầu","Trả phòng","Loại thời điểm","Trạng thái"},0) {
        @Override public boolean isCellEditable(int row,int column) { return false; }
    };
    private final JTable table=new JTable(model);
    private List<Phim> films=List.of();
    private List<PhongChieu> rooms=List.of();
    private boolean busy;
    private record Data(List<Phim> films,List<PhongChieu> rooms,List<LoaiThoiDiem> periods,List<SuatChieu> shows) { }

    @Override public void addNotify() { super.addNotify(); if(getClientProperty("cinema.page")==null){putClientProperty("cinema.page",true);Design.page(this,"Lịch chiếu","Lập lịch theo phòng · Bảo toàn các giao dịch đã bán",message);} }

    public LichChieuPanel(PhienDangNhap session) {
        super(new BorderLayout(8,8));
        service=new LichChieuBUS(session,new SuatChieuDAO());
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel top=new JPanel(new GridLayout(0,1,4,4));
        JPanel filters=new JPanel();
        filters.add(new JLabel("Ngày (yyyy-MM-dd, trống để xem tất cả)")); filters.add(dateFilter);
        roomFilter.addItem("Tất cả phòng"); filters.add(roomFilter);
        controls.add(dateFilter); controls.add(roomFilter);
        button(filters,"Tìm kiếm",this::refresh); top.add(filters);
        JPanel actions=new JPanel();
        button(actions,"Tạo suất chiếu",() -> new Editor(Mode.CREATE,null).setVisible(true));
        button(actions,"Cập nhật phim chiếu",() -> edit(Mode.FILM));
        button(actions,"Thay đổi phòng chiếu",() -> edit(Mode.ROOM));
        button(actions,"Đổi ngày chiếu",() -> edit(Mode.DATE));
        button(actions,"Đổi giờ chiếu",() -> edit(Mode.TIME));
        button(actions,"Hủy suất chiếu",() -> selected(this::cancel));
        top.add(actions); add(top,BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table),BorderLayout.CENTER); add(message,BorderLayout.SOUTH);
        dateFilter.addActionListener(event -> { if(!busy) refresh(); });
        DateInputs.attach(dateFilter,false,true);refresh();
    }

    private void button(JPanel parent,String title,Runnable action) {
        JButton button=new JButton(title); controls.add(button); parent.add(button);
        button.addActionListener(event -> { if(!busy) action.run(); });
    }

    private void refresh() {
        LocalDate date;
        try { date=dateFilter.getText().isBlank()?null:LocalDate.parse(dateFilter.getText().trim()); }
        catch(DateTimeParseException e) { message.setText("Ngày tra cứu không hợp lệ"); return; }
        int index=roomFilter.getSelectedIndex();
        String room=index<=0?null:rooms.get(index-1).maPhong();
        run(() -> new Data(service.films(),service.rooms(),service.periods(),service.list(date,room)),data -> {
            films=data.films(); rooms=data.rooms();
            roomFilter.removeAllItems(); roomFilter.addItem("Tất cả phòng");
            for(PhongChieu item:rooms) roomFilter.addItem(item.tenPhong());
            for(int i=0;i<rooms.size();i++) if(rooms.get(i).maPhong().equals(room)) roomFilter.setSelectedIndex(i+1);
            model.setRowCount(0);
            for(SuatChieu show:data.shows()) {
                String film=films.stream().filter(p -> p.maPhim().equals(show.maPhim())).map(Phim::tenPhim).findFirst().orElse(show.maPhim());
                String roomName=rooms.stream().filter(p -> p.maPhong().equals(show.maPhong())).map(PhongChieu::tenPhong).findFirst().orElse(show.maPhong());
                String period=data.periods().stream().filter(p -> p.maLoaiThoiDiem()==show.maLoaiThoiDiem()).map(LoaiThoiDiem::tenLoaiThoiDiem).findFirst().orElse("");
                model.addRow(new Object[]{show.maSuat(),film,roomName,show.thoiGianBatDau(),show.thoiGianTraPhong(),period,show.trangThai()});
            }
            message.setText(data.shows().isEmpty()?"Không tìm thấy suất chiếu phù hợp":"Số suất chiếu: "+data.shows().size());
        });
    }

    private void selected(Consumer<Integer> action) {
        int row=table.getSelectedRow();
        if(row<0) { message.setText("Vui lòng chọn suất chiếu"); return; }
        action.accept((Integer)model.getValueAt(table.convertRowIndexToModel(row),0));
    }
    private void edit(Mode mode) { selected(id -> run(() -> service.editable(id),show -> new Editor(mode,show).setVisible(true))); }
    private void cancel(int id) {
        if(JOptionPane.showConfirmDialog(this,"Hủy suất chiếu và các giao dịch giữ ghế chưa thanh toán liên quan?",
                "Xác nhận hủy suất chiếu",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        run(() -> { service.cancel(id); return id; },value -> {
            JOptionPane.showMessageDialog(this,"Hủy suất chiếu thành công"); refresh();
        });
    }
    private enum Mode { CREATE,FILM,ROOM,DATE,TIME }

    private final class Editor extends JDialog {
        private final JComboBox<Phim> film=new JComboBox<>();
        private final JComboBox<PhongChieu> room=new JComboBox<>();
        private final JTextField day=new JTextField(18);
        private final JTextField time=new JTextField(18);
        private final JButton save=new JButton("Lưu");
        private final JButton cancel=new JButton("Hủy");

        Editor(Mode mode,SuatChieu existing) {
            super(SwingUtilities.getWindowAncestor(LichChieuPanel.this),title(mode),Dialog.ModalityType.APPLICATION_MODAL);
            JPanel form=new JPanel(new GridLayout(0,2,8,8));
            form.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
            films.stream().filter(p -> !"Ngừng chiếu".equals(p.trangThai())).forEach(film::addItem);
            rooms.stream().filter(p -> "Đang hoạt động".equals(p.trangThai())).forEach(room::addItem);
            film.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                    JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                    if(value instanceof Phim p) label.setText(p.tenPhim()+" · "+p.thoiLuong()+" phút · "+p.dinhDangChieu());
                    return label;
                }
            });
            LocalDateTime start=existing==null?LocalDateTime.now(ZoneId.of("Asia/Bangkok")).plusHours(1).withSecond(0).withNano(0):existing.thoiGianBatDau();
            day.setText(start.toLocalDate().toString()); time.setText(start.toLocalTime().toString());
            if(existing!=null) {
                for(int i=0;i<film.getItemCount();i++) if(film.getItemAt(i).maPhim().equals(existing.maPhim())) film.setSelectedIndex(i);
                for(int i=0;i<room.getItemCount();i++) if(room.getItemAt(i).maPhong().equals(existing.maPhong())) room.setSelectedIndex(i);
                row(form,"Suất chiếu",new JLabel(Integer.toString(existing.maSuat())));
            }
            if(mode==Mode.CREATE || mode==Mode.FILM) row(form,"Phim",film);
            if(mode==Mode.CREATE || mode==Mode.ROOM) row(form,"Phòng chiếu",room);
            if(mode==Mode.CREATE || mode==Mode.DATE) row(form,"Ngày (yyyy-MM-dd)",day);
            if(mode==Mode.CREATE || mode==Mode.TIME) row(form,"Giờ (HH:mm)",time);
            JPanel body=new JPanel(new BorderLayout()); body.add(form,BorderLayout.CENTER);
            if(existing!=null) body.add(new JLabel("Các giao dịch giữ ghế chưa thanh toán liên quan sẽ bị hủy khi lưu thay đổi."),BorderLayout.NORTH);
            JPanel buttons=new JPanel(); buttons.add(save); buttons.add(cancel); body.add(buttons,BorderLayout.SOUTH);
            cancel.addActionListener(event -> dispose());
            save.addActionListener(event -> {
                Phim chosenFilm=(Phim)film.getSelectedItem(); PhongChieu chosenRoom=(PhongChieu)room.getSelectedItem();
                LocalDate date; LocalTime hour;
                try { date=LocalDate.parse(day.getText().trim()); hour=LocalTime.parse(time.getText().trim()); }
                catch(DateTimeParseException e) { JOptionPane.showMessageDialog(this,"Ngày giờ không hợp lệ. Dùng yyyy-MM-dd và HH:mm."); return; }
                save.setEnabled(false); cancel.setEnabled(false); setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                new SwingWorker<SuatChieu,Void>() {
                    @Override protected SuatChieu doInBackground() throws Exception {
                        return switch(mode) {
                            case CREATE -> service.create(chosenFilm==null?null:chosenFilm.maPhim(),chosenRoom==null?null:chosenRoom.maPhong(),LocalDateTime.of(date,hour));
                            case FILM -> service.changeFilm(existing.maSuat(),chosenFilm==null?null:chosenFilm.maPhim());
                            case ROOM -> service.changeRoom(existing.maSuat(),chosenRoom==null?null:chosenRoom.maPhong());
                            case DATE -> service.changeDate(existing.maSuat(),date);
                            case TIME -> service.changeTime(existing.maSuat(),hour);
                        };
                    }
                    @Override protected void done() {
                        try {
                            SuatChieu saved=get(); dispose();
                            JOptionPane.showMessageDialog(LichChieuPanel.this,title(mode)+" thành công");
                            dateFilter.setText(saved.thoiGianBatDau().toLocalDate().toString()); roomFilter.setSelectedIndex(0); refresh();
                        } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
                        catch(ExecutionException e) { JOptionPane.showMessageDialog(Editor.this,error(e.getCause())); }
                        finally { save.setEnabled(true); cancel.setEnabled(true); setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); }
                    }
                }.execute();
            });
            Component previous=((BorderLayout)body.getLayout()).getLayoutComponent(BorderLayout.NORTH);if(previous!=null)body.remove(previous);JLabel preview=SchedulePreview.create(this,table,film,room,day,time,existing);body.add(previous==null?preview:Design.stack(previous,preview),BorderLayout.NORTH);
            add(body); setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); pack(); setLocationRelativeTo(LichChieuPanel.this);
        }
        private void row(JPanel panel,String label,Component value) { panel.add(new JLabel(label)); panel.add(value); }
    }

    private static String title(Mode mode) {
        return switch(mode) {
            case CREATE -> "Tạo suất chiếu";
            case FILM -> "Cập nhật phim chiếu";
            case ROOM -> "Thay đổi phòng chiếu";
            case DATE -> "Đổi ngày chiếu";
            case TIME -> "Đổi giờ chiếu";
        };
    }
    private <T> void run(Callable<T> task,Consumer<T> success) {
        if(busy) return;
        String previousMessage=message.getText();
        busy=true; controls.forEach(c -> c.setEnabled(false)); message.setText("Đang tải dữ liệu...");
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy=false; controls.forEach(c -> c.setEnabled(true));
                try { T result=get(); message.setText(previousMessage); success.accept(result); }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); }
                catch(ExecutionException e) { message.setText(error(e.getCause())); }
            }
        }.execute();
    }
    private static String error(Throwable e) {
        return e instanceof IllegalArgumentException?e.getMessage():"Không thể lưu hoặc đọc lịch chiếu. Vui lòng kiểm tra kết nối và thử lại.";
    }
}
