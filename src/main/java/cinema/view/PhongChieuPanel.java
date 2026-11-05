package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.room.PhongChieuBUS;
import cinema.dao.*;
import cinema.model.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class PhongChieuPanel extends JPanel {
    static { Theme.install(); }
    private final PhongChieuBUS service;
    private final boolean admin;
    private final JTextField keyword=new JTextField(18);
    private final JComboBox<String> typeFilter=new JComboBox<>(),stateFilter=new JComboBox<>();
    private final DefaultTableModel model=tableModel("Mã phòng","Tên phòng","Loại phòng","Định dạng","Dọn phòng (phút)","Trạng thái");
    private final JTable table=new JTable(model);
    private final JLabel message=new JLabel(" ");
    private final List<Component> controls=new ArrayList<>();
    private List<LoaiPhong> roomTypes=List.of();
    private boolean busy;
    private record Data(List<LoaiPhong> types,List<PhongChieu> rooms) { }

    @Override public void addNotify() { super.addNotify(); if(getClientProperty("cinema.page")==null){putClientProperty("cinema.page",true);Design.page(this,"Phòng chiếu","Không gian phòng và cấu trúc sơ đồ ghế",message);} }

    public PhongChieuPanel(PhienDangNhap session) {
        super(new BorderLayout(8,8)); admin=session.vaiTro()==VaiTro.ADMIN;
        service=new PhongChieuBUS(session,new PhongChieuDAO(),new GheDAO());
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel top=new JPanel(new GridLayout(0,1,4,4)); JPanel filters=new JPanel();
        filters.add(new JLabel("Mã hoặc tên phòng")); filters.add(keyword);
        typeFilter.addItem("Tất cả loại phòng"); stateFilter.addItem("Tất cả trạng thái");
        PhongChieuBUS.TRANG_THAI_PHONG.forEach(stateFilter::addItem);
        filters.add(typeFilter); filters.add(stateFilter); controls.add(keyword); controls.add(typeFilter); controls.add(stateFilter);
        button(filters,"Tìm kiếm",this::refresh); top.add(filters);
        JPanel actions=new JPanel();
        button(actions,"Xem phòng và sơ đồ ghế",() -> selected(id -> new SeatMap(id).setVisible(true)));
        if(admin) {
            button(actions,"Thêm phòng chiếu",() -> editRoom(null,false));
            button(actions,"Cập nhật phòng",() -> selected(id -> load(() -> service.detail(id),room -> editRoom(room,false))));
            button(actions,"Đổi trạng thái phòng",() -> selected(id -> load(() -> service.detail(id),room -> editRoom(room,true))));
        }
        top.add(actions); add(top,BorderLayout.NORTH); table.setAutoCreateRowSorter(true); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table),BorderLayout.CENTER); add(message,BorderLayout.SOUTH);
        keyword.addActionListener(event -> { if(!busy) refresh(); }); refresh();
    }
    private void button(JPanel parent,String title,Runnable action) {
        JButton button=new JButton(title); controls.add(button); parent.add(button);
        button.addActionListener(event -> { if(!busy) action.run(); });
    }
    private void selected(Consumer<String> action) {
        int row=table.getSelectedRow();
        if(row<0) { message.setText("Vui lòng chọn phòng chiếu"); return; }
        action.accept((String)model.getValueAt(table.convertRowIndexToModel(row),0));
    }
    private void refresh() {
        int typeIndex=typeFilter.getSelectedIndex(); Integer type=typeIndex<=0?null:roomTypes.get(typeIndex-1).maLoaiPhong();
        String state=stateFilter.getSelectedIndex()==0?null:(String)stateFilter.getSelectedItem(); String text=keyword.getText();
        load(() -> new Data(service.roomTypes(),service.search(text,type,state)),data -> {
            roomTypes=data.types(); typeFilter.removeAllItems(); typeFilter.addItem("Tất cả loại phòng");
            for(int i=0;i<roomTypes.size();i++) { typeFilter.addItem(roomTypes.get(i).tenLoaiPhong()); if(Integer.valueOf(roomTypes.get(i).maLoaiPhong()).equals(type)) typeFilter.setSelectedIndex(i+1); }
            model.setRowCount(0);
            for(PhongChieu room:data.rooms()) model.addRow(new Object[]{room.maPhong(),room.tenPhong(),room.loaiPhong().tenLoaiPhong(),
                    room.loaiPhong().dinhDangChieu(),room.thoiGianDonPhong(),room.trangThai()});
            message.setText(data.rooms().isEmpty()?"Không tìm thấy phòng chiếu phù hợp":"Số phòng: "+data.rooms().size());
        });
    }
    private void editRoom(PhongChieu existing,boolean stateOnly) {
        String title=existing==null?"Thêm phòng chiếu":stateOnly?"Đổi trạng thái phòng":"Cập nhật phòng";
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),title,Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form=form(); JTextField name=new JTextField(existing==null?"":existing.tenPhong(),25);
        JTextField cleaning=new JTextField(existing==null?"15":Integer.toString(existing.thoiGianDonPhong()));
        JComboBox<LoaiPhong> type=new JComboBox<>(roomTypes.toArray(LoaiPhong[]::new));
        type.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                if(value instanceof LoaiPhong roomType) label.setText(roomType.tenLoaiPhong()+" · "+roomType.dinhDangChieu()); return label;
            }
        });
        JComboBox<String> state=new JComboBox<>(PhongChieuBUS.TRANG_THAI_PHONG.toArray(String[]::new));
        if(existing!=null) {
            state.setSelectedItem(existing.trangThai());
            for(int i=0;i<type.getItemCount();i++) if(type.getItemAt(i).maLoaiPhong()==existing.loaiPhong().maLoaiPhong()) type.setSelectedIndex(i);
        }
        if(!stateOnly) { row(form,"Tên phòng",name); row(form,"Loại phòng",type); row(form,"Dọn phòng (phút)",cleaning); }
        if(existing==null || stateOnly) row(form,"Trạng thái",state);
        JButton save=new JButton("Lưu"),cancel=new JButton("Hủy"); form.add(save); form.add(cancel);
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            int minutes;
            try { minutes=Integer.parseInt(cleaning.getText().trim()); }
            catch(NumberFormatException e) { JOptionPane.showMessageDialog(dialog,"Thời gian dọn phòng phải là số nguyên không âm"); return; }
            String newName=name.getText(),newState=(String)state.getSelectedItem(); LoaiPhong chosen=(LoaiPhong)type.getSelectedItem();
            perform(dialog,save,cancel,() -> {
                if(existing==null) return service.create(newName,chosen==null?0:chosen.maLoaiPhong(),minutes,newState);
                if(stateOnly) service.updateState(existing.maPhong(),newState);
                else service.updateInfo(existing.maPhong(),newName,chosen==null?0:chosen.maLoaiPhong(),minutes);
                return service.detail(existing.maPhong());
            },room -> {
                keyword.setText(room.maPhong()); typeFilter.setSelectedIndex(0); stateFilter.setSelectedIndex(0);
                JOptionPane.showMessageDialog(this,title+" thành công"); refresh();
            });
        });
        dialog.add(form); dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); dialog.pack(); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    private final class SeatMap extends JDialog {
        private final String roomId;
        private final DefaultTableModel seatsModel=tableModel("Mã ghế","Hàng","Số","Loại ghế","Trạng thái");
        private final JTable seatsTable=new JTable(seatsModel);
        private final JPanel map=new JPanel(new GridBagLayout()),actions=new JPanel();
        private final JLabel summary=new JLabel(" "),status=new JLabel(" ");
        private List<LoaiGhe> types=List.of();
        private record SeatData(PhongChieu room,List<Ghe> seats,List<LoaiGhe> types) { }

        SeatMap(String id) {
            super(SwingUtilities.getWindowAncestor(PhongChieuPanel.this),"Sơ đồ ghế",Dialog.ModalityType.APPLICATION_MODAL); roomId=id;
            JPanel body=new JPanel(new BorderLayout(8,8)); body.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            JPanel header=new JPanel(new GridLayout(0,1,4,4)); header.add(summary); header.add(actions);header.add(SeatVisual.screen());header.add(SeatVisual.legend("Trống","Không khả dụng")); body.add(header,BorderLayout.NORTH);
            if(admin) {
                seatButton("Thêm ghế",() -> editSeat(null,false));
                seatButton("Cập nhật ghế",() -> selectedSeat(seat -> editSeat(seat,false)));
                seatButton("Phân loại ghế",() -> selectedSeat(seat -> editSeat(seat,true)));
            }
            seatsTable.setAutoCreateRowSorter(true); seatsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JTabbedPane tabs=new JTabbedPane(); tabs.addTab("Sơ đồ ghế",new JScrollPane(map)); tabs.addTab("Danh sách ghế",new JScrollPane(seatsTable)); body.add(tabs,BorderLayout.CENTER);
            JPanel footer=new JPanel(new BorderLayout()); JButton back=new JButton("Trở về"); back.addActionListener(event -> dispose());
            footer.add(status,BorderLayout.CENTER); footer.add(back,BorderLayout.EAST); body.add(footer,BorderLayout.SOUTH);
            add(body); setSize(940,560); setLocationRelativeTo(PhongChieuPanel.this); setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); reload();
        }
        private void seatButton(String title,Runnable action) { JButton button=new JButton(title); actions.add(button); button.addActionListener(event -> action.run()); }
        private void selectedSeat(Consumer<Ghe> action) {
            int row=seatsTable.getSelectedRow(); if(row<0) { status.setText("Vui lòng chọn ghế trên sơ đồ hoặc danh sách"); return; }
            int id=(Integer)seatsModel.getValueAt(seatsTable.convertRowIndexToModel(row),0);
            new SwingWorker<Ghe,Void>() {
                @Override protected Ghe doInBackground() throws Exception { return service.seat(id); }
                @Override protected void done() {
                    try { action.accept(get()); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
                    catch(ExecutionException e) { status.setText(error(e.getCause())); }
                }
            }.execute();
        }
        private void reload() {
            for(Component c:actions.getComponents()) c.setEnabled(false); status.setText("Đang tải sơ đồ ghế...");
            new SwingWorker<SeatData,Void>() {
                @Override protected SeatData doInBackground() throws Exception { return new SeatData(service.detail(roomId),service.seats(roomId),service.seatTypes()); }
                @Override protected void done() {
                    try {
                        SeatData data=get(); types=data.types(); seatsModel.setRowCount(0); map.removeAll();
                        long ready=data.seats().stream().filter(g -> "Sẵn sàng sử dụng".equals(g.trangThai())).count();
                        summary.setText(data.room().tenPhong()+" · "+data.room().loaiPhong().dinhDangChieu()+" · "+data.room().trangThai()
                                +" · Tổng "+data.seats().size()+" ghế · Sẵn sàng "+ready+" · Bảo trì "+(data.seats().size()-ready));
                        List<String> rows=data.seats().stream().map(Ghe::hangGhe).distinct().sorted().toList();
                        List<Integer> numbers=data.seats().stream().map(Ghe::soGhe).distinct().sorted().toList();
                        for(Ghe seat:data.seats()) {
                            seatsModel.addRow(new Object[]{seat.maGhe(),seat.hangGhe(),seat.soGhe(),seat.loaiGhe().tenLoaiGhe(),seat.trangThai()});
                            JButton button=new JButton(seat.hangGhe()+seat.soGhe()+" · "+seat.loaiGhe().tenLoaiGhe());
                            SeatVisual.install(button,"Bảo trì".equals(seat.trangThai())?"Không khả dụng":"Trống",seat.loaiGhe().tenLoaiGhe(),false);button.setToolTipText(seat.trangThai());
                            if("Bảo trì".equals(seat.trangThai())) button.setText(button.getText()+" · Bảo trì");
                            button.addActionListener(event -> {
                                for(int i=0;i<seatsModel.getRowCount();i++) if(Integer.valueOf(seat.maGhe()).equals(seatsModel.getValueAt(i,0))) {
                                    int view=seatsTable.convertRowIndexToView(i); seatsTable.setRowSelectionInterval(view,view); status.setText(seat.hangGhe()+seat.soGhe()+" · "+seat.trangThai());
                                }
                            });
                            GridBagConstraints constraints=new GridBagConstraints(); constraints.gridx=numbers.indexOf(seat.soGhe()); constraints.gridy=rows.indexOf(seat.hangGhe());
                            constraints.insets=new Insets(Theme.XS,Theme.XS,Theme.XS,Theme.XS); constraints.fill=GridBagConstraints.HORIZONTAL; map.add(button,constraints);
                        }
                        map.revalidate(); map.repaint(); status.setText(data.seats().isEmpty()?"Phòng chưa có ghế":"Chọn ghế để xem hoặc cập nhật thông tin");
                    } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
                    catch(ExecutionException e) { status.setText(error(e.getCause())); }
                    finally { for(Component c:actions.getComponents()) c.setEnabled(true); }
                }
            }.execute();
        }
        private void editSeat(Ghe existing,boolean typeOnly) {
            JDialog dialog=new JDialog(this,existing==null?"Thêm ghế":typeOnly?"Phân loại ghế":"Cập nhật ghế",Dialog.ModalityType.APPLICATION_MODAL);
            JPanel form=form(); JTextField rowText=new JTextField(existing==null?"":existing.hangGhe(),20);
            JTextField number=new JTextField(existing==null?"":Integer.toString(existing.soGhe()));
            JComboBox<LoaiGhe> type=new JComboBox<>(types.toArray(LoaiGhe[]::new));
            JComboBox<String> state=new JComboBox<>(PhongChieuBUS.TRANG_THAI_GHE.toArray(String[]::new));
            if(existing!=null) {
                state.setSelectedItem(existing.trangThai());
                for(int i=0;i<type.getItemCount();i++) if(type.getItemAt(i).maLoaiGhe()==existing.loaiGhe().maLoaiGhe()) type.setSelectedIndex(i);
            }
            if(!typeOnly) { row(form,"Hàng ghế",rowText); row(form,"Số ghế",number); row(form,"Trạng thái",state); }
            if(existing==null || typeOnly) row(form,"Loại ghế",type);
            JButton save=new JButton("Lưu"),cancel=new JButton("Hủy"); form.add(save); form.add(cancel); cancel.addActionListener(event -> dialog.dispose());
            save.addActionListener(event -> {
                int seatNumber;
                try { seatNumber=typeOnly?existing.soGhe():Integer.parseInt(number.getText().trim()); }
                catch(NumberFormatException e) { JOptionPane.showMessageDialog(dialog,"Số ghế phải là số nguyên lớn hơn 0"); return; }
                LoaiGhe chosen=(LoaiGhe)type.getSelectedItem(); String selectedState=(String)state.getSelectedItem(),newRow=rowText.getText();
                perform(dialog,save,cancel,() -> {
                    if(existing==null) return service.addSeat(roomId,newRow,seatNumber,chosen==null?0:chosen.maLoaiGhe(),selectedState);
                    if(typeOnly) return service.classifySeat(existing.maGhe(),chosen==null?0:chosen.maLoaiGhe());
                    return service.updateSeat(existing.maGhe(),newRow,seatNumber,selectedState);
                },seat -> { JOptionPane.showMessageDialog(SeatMap.this,dialog.getTitle()+" thành công"); reload(); });
            });
            JPanel body=new JPanel(new BorderLayout()); body.add(form,BorderLayout.CENTER);
            if(existing!=null) body.add(new JLabel("Thay đổi sẽ hủy các giao dịch giữ ghế chưa thanh toán liên quan."),BorderLayout.NORTH);
            dialog.add(body); dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); dialog.pack(); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
        }
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
    private static JPanel form() { JPanel panel=new JPanel(new GridLayout(0,2,8,8)); panel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16)); return panel; }
    private static void row(JPanel panel,String label,Component value) { panel.add(new JLabel(label)); panel.add(value); }
    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns,0) { @Override public boolean isCellEditable(int row,int column) { return false; } };
    }
    private static String error(Throwable e) { return e instanceof IllegalArgumentException?e.getMessage():"Không thể đọc hoặc lưu phòng/ghế. Vui lòng kiểm tra kết nối và thử lại."; }
}
