package cinema.controller.room;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.*;
import cinema.model.*;
import java.sql.SQLException;
import java.util.*;

public final class PhongChieuBUS {
    public static final List<String> TRANG_THAI_PHONG=List.of("Đang hoạt động","Tạm ngừng hoạt động","Bảo trì");
    public static final List<String> TRANG_THAI_GHE=List.of("Sẵn sàng sử dụng","Bảo trì");
    private final PhienDangNhap session;
    private final PhongChieuDAO rooms;
    private final GheDAO seats;
    public PhongChieuBUS(PhienDangNhap session,PhongChieuDAO rooms,GheDAO seats) {
        if(session==null || session.vaiTro()==null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        this.session=session; this.rooms=rooms; this.seats=seats;
    }
    private void admin() { if(session.vaiTro()!=VaiTro.ADMIN) throw new IllegalArgumentException("Bạn không có quyền quản lý phòng hoặc ghế"); }
    public List<PhongChieu> search(String text,Integer type,String state) throws SQLException {
        if(state!=null && !TRANG_THAI_PHONG.contains(state)) throw new IllegalArgumentException("Trạng thái phòng không hợp lệ");
        return rooms.search(text==null?"":text.trim(),type,state);
    }
    public PhongChieu detail(String id) throws SQLException { return rooms.find(id); }
    public List<Ghe> seats(String room) throws SQLException { rooms.find(room); return seats.list(room); }
    public List<LoaiPhong> roomTypes() throws SQLException { return rooms.types(); }
    public List<LoaiGhe> seatTypes() throws SQLException { return seats.types(); }
    public Ghe seat(int id) throws SQLException { return seats.find(id); }

    public PhongChieu create(String name,int type,int cleaning,String state) throws SQLException {
        admin(); name=roomName(name); checkCleaning(cleaning); checkState(state,TRANG_THAI_PHONG);
        LoaiPhong category=roomType(type);
        PhongChieu room=new PhongChieu("R"+UUID.randomUUID().toString().replace("-","").substring(0,9).toUpperCase(Locale.ROOT),category,name,cleaning,state);
        rooms.insert(room); return room;
    }
    public void updateInfo(String id,String name,int type,int cleaning) throws SQLException {
        admin(); name=roomName(name); checkCleaning(cleaning); roomType(type); rooms.updateInfo(id,name,type,cleaning);
    }
    public void updateState(String id,String state) throws SQLException { admin(); checkState(state,TRANG_THAI_PHONG); rooms.updateState(id,state); }
    public Ghe addSeat(String room,String row,int number,int type,String state) throws SQLException {
        admin(); row=seatRow(row); checkNumber(number); checkState(state,TRANG_THAI_GHE); seatType(type);
        return seats.insert(room,row,number,type,state);
    }
    public Ghe updateSeat(int id,String row,int number,String state) throws SQLException {
        admin(); row=seatRow(row); checkNumber(number); checkState(state,TRANG_THAI_GHE); return seats.updateInfo(id,row,number,state);
    }
    public Ghe classifySeat(int id,int type) throws SQLException { admin(); seatType(type); return seats.classify(id,type); }

    private LoaiPhong roomType(int id) throws SQLException {
        return rooms.types().stream().filter(t -> t.maLoaiPhong()==id).findFirst().orElseThrow(() -> new IllegalArgumentException("Loại phòng không hợp lệ"));
    }
    private void seatType(int id) throws SQLException {
        if(seats.types().stream().noneMatch(t -> t.maLoaiGhe()==id)) throw new IllegalArgumentException("Loại ghế không hợp lệ");
    }
    private String roomName(String value) {
        if(value==null || value.isBlank()) throw new IllegalArgumentException("Vui lòng nhập tên phòng");
        String text=value.trim(); if(text.length()>50) throw new IllegalArgumentException("Tên phòng tối đa 50 ký tự"); return text;
    }
    private String seatRow(String value) {
        String row=value==null?"":value.trim().toUpperCase(Locale.ROOT);
        if(!row.matches("[A-Z]{1,2}")) throw new IllegalArgumentException("Hàng ghế gồm 1–2 chữ cái A–Z"); return row;
    }
    private void checkNumber(int value) { if(value<=0) throw new IllegalArgumentException("Số ghế phải lớn hơn 0"); }
    private void checkCleaning(int value) { if(value<0) throw new IllegalArgumentException("Thời gian dọn phòng không được âm"); }
    private void checkState(String value,List<String> allowed) { if(value==null || !allowed.contains(value)) throw new IllegalArgumentException("Trạng thái không hợp lệ"); }
}
