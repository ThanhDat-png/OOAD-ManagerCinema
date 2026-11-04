package cinema.controller.sales;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.pricing.MoneyRules;
import cinema.dao.*;
import cinema.model.*;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

public final class BanVeBUS {
    public static final List<String> PHUONG_THUC=List.of("Tiền mặt","Chuyển khoản","Thẻ");
    private final PhienDangNhap session;
    private final BanVeDAO dao;
    private final int holdMinutes;
    public BanVeBUS(PhienDangNhap session,BanVeDAO dao) { this(session,dao,loadMinutes()); }
    public BanVeBUS(PhienDangNhap session,BanVeDAO dao,int minutes) {
        if(session==null || session.vaiTro()==null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        if(minutes<=0 || minutes>60) throw new IllegalArgumentException("Thời hạn giữ ghế cần từ 1 đến 60 phút");
        this.session=session; this.dao=dao; this.holdMinutes=minutes;
    }
    private void staff() { if(session.vaiTro()!=VaiTro.NHAN_VIEN) throw new IllegalArgumentException("Bạn không có quyền bán vé"); }
    private String employee() { staff(); if(session.nhanVien()==null) throw new IllegalArgumentException("Tài khoản chưa gắn hồ sơ nhân viên để ghi nhận giao dịch"); return session.nhanVien().maNV(); }
    public String employeeId() { return employee(); }
    public int holdMinutes() { return holdMinutes; }
    public static String newId() { return "G"+UUID.randomUUID().toString().replace("-","").substring(0,19); }
    public List<BanVeDAO.SuatBan> search(String film,LocalDate date,LocalTime time) throws SQLException { staff(); return dao.search(film==null?"":film.trim(),date,time); }
    public List<GheSuatChieu> selectShow(int show) throws SQLException {staff();if(show<=0)throw new IllegalArgumentException("Vui lòng chọn suất chiếu");return dao.selectShow(show);}
    public List<GheSuatChieu> seats(int show) throws SQLException { staff(); if(show<=0) throw new IllegalArgumentException("Vui lòng chọn suất chiếu"); return dao.map(show); }
    public BanVeDAO.GioHang hold(String id,int show,Collection<Integer> seats) throws SQLException {
        String actor=employee(); id=id(id);
        if(show<=0 || seats==null || seats.isEmpty() || seats.stream().anyMatch(n -> n==null || n<=0)) throw new IllegalArgumentException("Vui lòng chọn ghế còn trống");
        return dao.hold(id,actor,show,new HashSet<>(seats),holdMinutes);
    }
    public BanVeDAO.GioHang quote(String id) throws SQLException { return dao.quote(id(id),employee()); }
    public BanVeDAO.GioHang beginPayment(String id) throws SQLException { return dao.beginPayment(id(id),employee()); }
    public BanVeDAO.GioHang complete(String id,String method,BigDecimal amount,boolean received) throws SQLException {
        String actor=employee(); id=id(id);
        if(!PHUONG_THUC.contains(method==null?"":method)) throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán");
        if(!received) throw new IllegalArgumentException("Chỉ hoàn tất sau khi đã nhận đủ tiền hoặc xác nhận thành công tại quầy");
        return dao.complete(id,actor,method,MoneyRules.positive(amount,"Số tiền thanh toán không hợp lệ"));
    }
    public void cancel(String id) throws SQLException { dao.cancel(id(id),employee(),false); }
    public void cancelPaymentBeforeReceivingMoney(String id) throws SQLException { dao.cancel(id(id),employee(),true); }
    public List<VeDAO.ThongTin> issue(String id) throws SQLException { return dao.issue(id(id),employee()); }
    public BanVeDAO.GioHang inspect(String id) throws SQLException { staff(); return dao.inspect(id(id)); }
    public List<VeDAO.ThongTin> savedTickets(String id) throws SQLException { staff(); return dao.tickets(id(id)); }
    public List<VeDAO.ThongTin> printableTickets(String id) throws SQLException {
        List<VeDAO.ThongTin> result=savedTickets(id);
        if(result.stream().anyMatch(v->!v.daThanhToan() || !v.gheDaBan()))throw new IllegalArgumentException("Không được in vé chưa thanh toán hợp lệ");
        return result;
    }
    public List<GiaoDichDAO.Dong> transactions(String code,LocalDate from,LocalDate to) throws SQLException {
        staff(); String text=code==null || code.isBlank()?null:id(code);
        if(text==null && from==null && to==null) throw new IllegalArgumentException("Vui lòng nhập mã giao dịch, mã vé hoặc khoảng thời gian");
        if((from==null)!=(to==null) || (from!=null && (from.isAfter(to) || from.getYear()<1000 || to.getYear()>9999))) throw new IllegalArgumentException("Khoảng thời gian không hợp lệ");
        return new GiaoDichDAO().search(text,from,to);
    }
    private String id(String id) { staff(); if(id==null || id.isBlank() || id.trim().length()>20) throw new IllegalArgumentException("Mã giao dịch hoặc mã vé không hợp lệ"); return id.trim(); }
    private static int loadMinutes() {
        Path file=Path.of("config","sales.properties"); if(!Files.exists(file)) return 5;
        Properties values=new Properties();
        try(Reader reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)) { values.load(reader); return Integer.parseInt(values.getProperty("holdMinutes","5").trim()); }
        catch(IOException|NumberFormatException e) { throw new IllegalArgumentException("Cấu hình thời gian giữ ghế không hợp lệ",e); }
    }
}
