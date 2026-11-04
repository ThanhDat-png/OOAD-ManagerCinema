package cinema.controller.admission;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.*;
import cinema.model.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class VeBUS {
    public record KetQua(VeDAO.ThongTin thongTin,boolean hopLe,String thongBao) { }
    private final PhienDangNhap session;
    private final VeDAO dao;
    private final ThoiGianVaoPhong window;
    public VeBUS(PhienDangNhap session,VeDAO dao) { this(session,dao,ThoiGianVaoPhong.load()); }
    public VeBUS(PhienDangNhap session,VeDAO dao,ThoiGianVaoPhong window) {
        if(session==null || session.vaiTro()==null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        if(window==null) throw new IllegalArgumentException("Cấu hình vào phòng không hợp lệ");
        this.session=session; this.dao=dao; this.window=window;
    }
    private void staff() { if(session.vaiTro()!=VaiTro.NHAN_VIEN) throw new IllegalArgumentException("Bạn không có quyền kiểm tra và xác nhận vé"); }
    public List<PhongChieu> rooms() throws SQLException { staff(); return new PhongChieuDAO().all(); }
    public List<SuatChieu> shows(LocalDate date,String room) throws SQLException { staff(); return new SuatChieuDAO().list(date,room); }
    public List<Phim> films() throws SQLException { staff(); return new PhimDAO().search("",null,null); }
    public ThoiGianVaoPhong window() { return window; }
    public VeDAO.ThongTin lookup(String code,VeDAO.LoaiMa type) throws SQLException {
        staff();
        if(type==null) throw new IllegalArgumentException("Vui lòng chọn loại mã");
        if(code==null || code.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mã vé hoặc quét mã QR");
        String text=code.trim();
        if(text.length()>(type==VeDAO.LoaiMa.MA_VE?20:100)) throw new IllegalArgumentException("Mã vé hoặc mã QR không hợp lệ");
        return dao.lookup(text,type).orElseThrow(() -> new IllegalArgumentException("Vé không tồn tại"));
    }
    public KetQua check(String code,VeDAO.LoaiMa type,String room,int show) throws SQLException {
        staff(); context(room,show);
        VeDAO.ThongTin info=lookup(code,type);
        try { validate(info,room,show); return new KetQua(info,true,"Vé hợp lệ"); }
        catch(IllegalArgumentException e) { return new KetQua(info,false,e.getMessage()); }
    }
    public VeDAO.ThongTin confirm(String ticket,String room,int show) throws SQLException {
        staff(); context(room,show);
        if(ticket==null || ticket.isBlank() || ticket.trim().length()>20) throw new IllegalArgumentException("Mã vé không hợp lệ");
        return dao.confirm(ticket.trim(),window.truocPhut(),window.sauPhut(),info -> validate(info,room,show));
    }
    private void context(String room,int show) {
        if(room==null || room.isBlank() || show<=0) throw new IllegalArgumentException("Vui lòng chọn phòng và suất chiếu đang kiểm soát");
    }
    void validate(VeDAO.ThongTin info,String room,int show) {
        if("Đã sử dụng".equals(info.ve().trangThai())) throw new IllegalArgumentException("Vé đã được sử dụng");
        if(!"Chưa sử dụng".equals(info.ve().trangThai())) throw new IllegalArgumentException("Trạng thái vé không hợp lệ");
        if(!info.daThanhToan()) throw new IllegalArgumentException("Vé chưa có thanh toán hợp lệ");
        if(!info.gheDaBan()) throw new IllegalArgumentException("Thông tin ghế hoặc giá đã chốt của vé không hợp lệ");
        if(!room.equals(info.maPhong())) throw new IllegalArgumentException("Vé không đúng phòng đang kiểm soát");
        if(show!=info.ve().maSuat()) throw new IllegalArgumentException("Vé không đúng suất chiếu đang kiểm soát");
        if("Đã hủy".equals(info.trangThaiSuat())) throw new IllegalArgumentException("Suất chiếu đã bị hủy");
        if(!"Đang hoạt động".equals(info.trangThaiPhong())) throw new IllegalArgumentException("Phòng chiếu không hoạt động");
        if(info.thoiDiemKiemTra().isBefore(info.batDau().minusMinutes(window.truocPhut()))
                || info.thoiDiemKiemTra().isAfter(info.batDau().plusMinutes(window.sauPhut())))
            throw new IllegalArgumentException("Ngoài thời gian cho phép vào phòng");
    }
}
