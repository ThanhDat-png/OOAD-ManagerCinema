package cinema.controller.pricing;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.*;
import cinema.model.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public final class GiaVeBUS {
    private final PhienDangNhap session;
    private final BangGiaVeDAO dao;
    public GiaVeBUS(PhienDangNhap session,BangGiaVeDAO dao) {
        if(session==null || session.vaiTro()==null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        this.session=session; this.dao=dao;
    }
    private void admin() { if(session.vaiTro()!=VaiTro.ADMIN) throw new IllegalArgumentException("Bạn không có quyền quản lý giá vé"); }
    public List<BangGiaVe> all() throws SQLException { admin(); return dao.all(); }
    public BangGiaVe detail(int id) throws SQLException { admin(); return dao.find(id); }
    public List<LoaiGhe> seatTypes() throws SQLException { admin(); return new GheDAO().types(); }
    public List<LoaiPhong> roomTypes() throws SQLException { admin(); return new PhongChieuDAO().types(); }
    public List<LoaiThoiDiem> periods() throws SQLException { admin(); return new LoaiThoiDiemDAO().all(); }
    public int create(int seat,int room,int period,BigDecimal amount) throws SQLException {
        admin();
        if(seat<=0 || room<=0 || period<=0) throw new IllegalArgumentException("Vui lòng chọn đủ điều kiện áp dụng giá vé");
        return dao.insert(seat,room,period,MoneyRules.positive(amount,"Vui lòng nhập giá vé hợp lệ"));
    }
    public void update(int id,BigDecimal amount) throws SQLException { admin(); dao.updatePrice(id,MoneyRules.positive(amount,"Vui lòng nhập giá vé hợp lệ")); }
}
