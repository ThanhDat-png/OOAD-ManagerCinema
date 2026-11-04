package cinema.dao;

import cinema.db.Database;
import cinema.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

public class BanVeDAO {
    public record GioHang(GiaoDich giaoDich,SuatChieu suat,PhongChieu phong,String tenPhim,List<GheSuatChieu> ghe,ThanhToan thanhToan) {
        public GioHang { ghe=List.copyOf(ghe); }
    }
    public record SuatBan(SuatChieu suat,String tenPhim,String tenPhong) {
        @Override public String toString() { return suat.maSuat()+" · "+tenPhim+" · "+tenPhong+" · "+suat.thoiGianBatDau(); }
    }
    private record Slot(SuatChieu show,PhongChieu room,String film,String filmState) { }
    private interface Work<T> { T run(Connection c) throws SQLException; }
    private static final BigDecimal MAX=new BigDecimal("999999999999");

    public List<SuatBan> search(String name,LocalDate date,LocalTime time) throws SQLException {
        String sql="SELECT s.*,p.TenPhim,r.TenPhong FROM SUATCHIEU s JOIN PHIM p ON p.MaPhim=s.MaPhim JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong "
                +"WHERE s.TrangThai<>'Đã hủy' AND s.ThoiGianBatDau>NOW() AND p.TrangThai IN ('Sắp chiếu','Đang chiếu') AND r.TrangThai='Đang hoạt động' "
                +"AND p.TenPhim LIKE ? ESCAPE '=' AND (? IS NULL OR DATE(s.ThoiGianBatDau)=?) AND (? IS NULL OR TIME(s.ThoiGianBatDau)=?) ORDER BY s.ThoiGianBatDau,s.MaSuat";
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1,"%"+name.replace("=","==").replace("%","=%").replace("_","=_")+"%"); ps.setObject(2,date); ps.setObject(3,date); ps.setObject(4,time); ps.setObject(5,time);
            try(ResultSet rs=ps.executeQuery()) { List<SuatBan> result=new ArrayList<>(); while(rs.next()) result.add(new SuatBan(readShow(rs),rs.getString("TenPhim"),rs.getString("TenPhong"))); return result; }
        }
    }
    public List<GheSuatChieu> selectShow(int show) throws SQLException {
        return tx(c->{Slot current=slot(c,show,true);lockSeats(c,show);selling(c,current);expire(c,show);return seats(c,show,null);});
    }
    public List<GheSuatChieu> map(int show) throws SQLException {
        return tx(c -> { slot(c,show,true); lockSeats(c,show); expire(c,show); return seats(c,show,null); });
    }
    public GioHang inspect(String id) throws SQLException {
        try(Connection c=Database.connect()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setReadOnly(true); c.setAutoCommit(false);
            try { GioHang result=cart(c,id); c.commit(); return result; }
            catch(SQLException|RuntimeException e) { PhongChieuDAO.rollback(c,e); throw e; }
        }
    }

    public GioHang hold(String id,String employee,int show,Set<Integer> selected,int minutes) throws SQLException {
        map(show);
        return tx(c -> {
            Slot current=slot(c,show,true); lockSeats(c,show); selling(c,current);
            GiaoDich existing=optional(c,id);
            if(existing==null) {
                execute(c,"INSERT INTO GIAODICH VALUES (?,?,NOW(),0,'Chờ thanh toán')",id,employee);
            } else {
                own(existing,employee);
                GioHang previous=cart(c,id); sameShow(previous,show);
                Set<Integer> old=new HashSet<>(); previous.ghe().forEach(g -> old.add(g.ghe().maGhe()));
                if(old.equals(selected) && List.of("Đang thanh toán","Đã thanh toán").contains(existing.trangThai())) return previous;
                pending(c,existing);
                if(old.equals(selected)) { held(c,previous,true); return previous; }
            }
            GiaoDich locked=GiaoDichDAO.get(c,id,true); own(locked,employee); pending(c,locked);
            List<GheSuatChieu> available=seats(c,show,null);
            for(int seat:selected) {
                GheSuatChieu row=available.stream().filter(g -> g.ghe().maGhe()==seat).findFirst().orElseThrow(() -> new IllegalArgumentException("Ghế không thuộc suất chiếu"));
                if(!"Sẵn sàng sử dụng".equals(row.ghe().trangThai()) || !("Trống".equals(row.trangThai()) || ("Đang giữ".equals(row.trangThai()) && id.equals(row.maGiaoDich()))))
                    throw new IllegalArgumentException("Ghế không thể chọn. Vui lòng cập nhật sơ đồ và chọn lại");
            }
            release(c,id);
            LocalDateTime until=now(c).plusMinutes(minutes); if(until.isAfter(current.show().thoiGianBatDau())) until=current.show().thoiGianBatDau();
            try(PreparedStatement ps=c.prepareStatement("UPDATE GHE_SUATCHIEU SET MaGiaoDich=?,TrangThai='Đang giữ',ThoiGianHetHanGiu=?,MaGia=NULL,GiaVeChot=NULL WHERE MaSuat=? AND MaGhe=?")) {
                for(int seat:new TreeSet<>(selected)) { ps.setString(1,id); ps.setTimestamp(2,Timestamp.valueOf(until)); ps.setInt(3,show); ps.setInt(4,seat); ps.addBatch(); } ps.executeBatch();
            }
            execute(c,"UPDATE GIAODICH SET TongTien=0 WHERE MaGiaoDich=?",id); return cart(c,id);
        });
    }
    public GioHang quote(String id,String employee) throws SQLException {
        sweepOwned(id,employee);
        return tx(c -> {
            GioHang order=lockCart(c,id,employee); pending(c,order.giaoDich()); held(c,order,true); selling(c,slot(c,order.suat().maSuat(),false));
            if(quoted(order)) return order;
            BigDecimal total=BigDecimal.ZERO;
            for(GheSuatChieu seat:order.ghe()) {
                int priceId; BigDecimal price;
                try(PreparedStatement ps=c.prepareStatement("SELECT MaGia,GiaVe FROM BANGGIAVE WHERE MaLoaiGhe=? AND MaLoaiPhong=? AND MaLoaiThoiDiem=? FOR SHARE")) {
                    ps.setInt(1,seat.ghe().loaiGhe().maLoaiGhe()); ps.setInt(2,order.phong().loaiPhong().maLoaiPhong()); ps.setInt(3,order.suat().maLoaiThoiDiem());
                    try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Chưa thiết lập giá vé cho lựa chọn này"); priceId=rs.getInt(1); price=rs.getBigDecimal(2); }
                }
                total=total.add(price);
                execute(c,"UPDATE GHE_SUATCHIEU SET MaGia=?,GiaVeChot=? WHERE MaSuat=? AND MaGhe=? AND MaGiaoDich=?",priceId,price,seat.maSuat(),seat.ghe().maGhe(),id);
            }
            money(total); execute(c,"UPDATE GIAODICH SET TongTien=? WHERE MaGiaoDich=?",total,id); return cart(c,id);
        });
    }
    public GioHang beginPayment(String id,String employee) throws SQLException {
        sweepOwned(id,employee);
        return tx(c -> {
            GioHang order=lockCart(c,id,employee);
            if(List.of("Đang thanh toán","Đã thanh toán").contains(order.giaoDich().trangThai())) return order;
            pending(c,order.giaoDich()); held(c,order,true); selling(c,slot(c,order.suat().maSuat(),false)); requireQuote(order);
            execute(c,"UPDATE GIAODICH SET TrangThai='Đang thanh toán' WHERE MaGiaoDich=?",id); return cart(c,id);
        });
    }
    public GioHang complete(String id,String employee,String method,BigDecimal amount) throws SQLException {
        return tx(c -> {
            GioHang order=lockCart(c,id,employee);
            if("Đã thanh toán".equals(order.giaoDich().trangThai())) {
                if(order.thanhToan()!=null && order.thanhToan().soTien().compareTo(order.giaoDich().tongTien())==0 && order.thanhToan().soTien().compareTo(amount)==0 && order.thanhToan().phuongThuc().equals(method)) return order;
                throw new IllegalArgumentException("Giao dịch đã thanh toán. Vui lòng kiểm tra khoản đã ghi nhận");
            }
            if(!"Đang thanh toán".equals(order.giaoDich().trangThai())) throw new IllegalArgumentException("Vui lòng bắt đầu thanh toán sau khi kiểm tra ghế và tính tiền");
            held(c,order,false); requireQuote(order);
            if(order.thanhToan()!=null) throw new IllegalArgumentException("Đã có thông tin thanh toán. Vui lòng kiểm tra giao dịch");
            if(order.giaoDich().tongTien().compareTo(amount)!=0) throw new IllegalArgumentException("Số tiền xác nhận phải bằng tổng tiền đã chốt");
            execute(c,"INSERT INTO THANHTOAN (MaGiaoDich,PhuongThuc,SoTien,ThoiGianThanhToan) VALUES (?,?,?,NOW())",id,method,amount);
            execute(c,"UPDATE GHE_SUATCHIEU SET TrangThai='Đã bán',ThoiGianHetHanGiu=NULL WHERE MaGiaoDich=?",id);
            execute(c,"UPDATE GIAODICH SET TrangThai='Đã thanh toán' WHERE MaGiaoDich=?",id); return cart(c,id);
        });
    }
    public void cancel(String id,String employee,boolean noPaymentReceived) throws SQLException {
        GioHang initial=inspect(id); own(initial.giaoDich(),employee);
        if(initial.suat()!=null) map(initial.suat().maSuat());
        tx(c -> {
            GioHang order=lockCart(c,id,employee); String state=order.giaoDich().trangThai();
            if(List.of("Đã hủy","Hết hạn").contains(state)) return null;
            if("Đã thanh toán".equals(state) || order.thanhToan()!=null) throw new IllegalArgumentException("Giao dịch đã thanh toán, không được hủy hoặc giải phóng ghế");
            if("Đang thanh toán".equals(state) && !noPaymentReceived) throw new IllegalArgumentException("Đang thanh toán. Cần xác nhận chưa nhận tiền trước khi hủy");
            if(!List.of("Chờ thanh toán","Đang thanh toán").contains(state)) throw new IllegalArgumentException("Trạng thái giao dịch không cho phép hủy");
            if(order.ghe().stream().anyMatch(g -> "Đã bán".equals(g.trangThai()))) throw new IllegalArgumentException("Giao dịch có ghế đã bán, không được hủy");
            release(c,id); execute(c,"UPDATE GIAODICH SET TrangThai='Đã hủy' WHERE MaGiaoDich=?",id); return null;
        });
    }
    public List<VeDAO.ThongTin> issue(String id,String employee) throws SQLException {
        return tx(c -> {
            GioHang order=lockCart(c,id,employee);
            if(!"Đã thanh toán".equals(order.giaoDich().trangThai()) || order.thanhToan()==null
                    || order.thanhToan().soTien().compareTo(order.giaoDich().tongTien())!=0) throw new IllegalArgumentException("Giao dịch chưa được thanh toán hợp lệ");
            requireQuote(order);
            if(order.ghe().isEmpty() || order.ghe().stream().anyMatch(g -> !"Đã bán".equals(g.trangThai()))) throw new IllegalArgumentException("Ghế chưa được ghi nhận đã bán");
            for(GheSuatChieu seat:order.ghe()) {
                try(PreparedStatement ps=c.prepareStatement("SELECT MaGiaoDich,MaGia,GiaVe FROM VE WHERE MaSuat=? AND MaGhe=? FOR UPDATE")) {
                    ps.setInt(1,seat.maSuat()); ps.setInt(2,seat.ghe().maGhe());
                    try(ResultSet rs=ps.executeQuery()) {
                        if(rs.next()) {
                            if(!id.equals(rs.getString(1)) || seat.maGia()!=rs.getInt(2) || seat.giaVeChot().compareTo(rs.getBigDecimal(3))!=0) throw new IllegalArgumentException("Vé đã có thông tin khác. Vui lòng kiểm tra giao dịch");
                            continue;
                        }
                    }
                }
                if(order.suat()==null || "Đã hủy".equals(order.suat().trangThai())) throw new IllegalArgumentException("Suất chiếu không còn phù hợp để phát hành vé");
                execute(c,"INSERT INTO VE (MaVe,MaGiaoDich,MaSuat,MaGhe,MaGia,MaQR,GiaVe,TrangThai,ThoiGianPhatHanh) VALUES (?,?,?,?,?,?,?,'Chưa sử dụng',NOW())",
                        "V"+UUID.randomUUID().toString().replace("-","").substring(0,19),id,seat.maSuat(),seat.ghe().maGhe(),seat.maGia(),UUID.randomUUID().toString(),seat.giaVeChot());
            }
            return VeDAO.forTransaction(c,id);
        });
    }
    public List<VeDAO.ThongTin> tickets(String id) throws SQLException { try(Connection c=Database.connect()) { return VeDAO.forTransaction(c,id); } }

    private void sweepOwned(String id,String employee) throws SQLException {
        GioHang order=inspect(id); own(order.giaoDich(),employee); if(order.suat()!=null) map(order.suat().maSuat());
    }
    private static GioHang lockCart(Connection c,String id,String employee) throws SQLException {
        GioHang initial=cart(c,id); own(initial.giaoDich(),employee);
        if(initial.ghe().size()>0 && initial.suat()==null) throw new IllegalArgumentException("Giao dịch có nhiều suất. Vui lòng kiểm tra dữ liệu trước khi xử lý");
        if(initial.suat()!=null) { slot(c,initial.suat().maSuat(),true); lockSeats(c,initial.suat().maSuat()); }
        GiaoDich latest=GiaoDichDAO.get(c,id,true); own(latest,employee); GioHang current=cart(c,id);
        if(initial.suat()!=null && current.suat()!=null && initial.suat().maSuat()!=current.suat().maSuat()) throw new IllegalArgumentException("Giao dịch đã thay đổi. Vui lòng tải lại");
        return current;
    }
    private static GioHang cart(Connection c,String id) throws SQLException {
        GiaoDich transaction=GiaoDichDAO.get(c,id,false); List<GheSuatChieu> seats=seats(c,null,id);
        Set<Integer> shows=new HashSet<>(); seats.forEach(g -> shows.add(g.maSuat()));
        Slot slot=shows.size()==1?slot(c,shows.iterator().next(),false):null;
        return new GioHang(transaction,slot==null?null:slot.show(),slot==null?null:slot.room(),slot==null?"":slot.film(),seats,GiaoDichDAO.payment(c,id));
    }
    private static Slot slot(Connection c,int id,boolean lock) throws SQLException {
        String room;
        try(PreparedStatement ps=c.prepareStatement("SELECT MaPhong FROM SUATCHIEU WHERE MaSuat=?")) { ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy suất chiếu"); room=rs.getString(1); } }
        if(lock) {
            PhongChieuDAO.lock(c,room);
            try(PreparedStatement ps=c.prepareStatement("SELECT MaPhong FROM SUATCHIEU WHERE MaSuat=? FOR UPDATE")) {
                ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()) { if(!rs.next() || !room.equals(rs.getString(1))) throw new IllegalArgumentException("Suất chiếu đã thay đổi. Vui lòng tải lại"); }
            }
        }
        try(PreparedStatement ps=c.prepareStatement("SELECT s.*,p.TenPhim,p.TrangThai AS FilmState FROM SUATCHIEU s JOIN PHIM p ON p.MaPhim=s.MaPhim WHERE s.MaSuat=?")) {
            ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()) { rs.next(); return new Slot(readShow(rs),PhongChieuDAO.find(c,room),rs.getString("TenPhim"),rs.getString("FilmState")); }
        }
    }
    private static List<GheSuatChieu> seats(Connection c,Integer show,String transaction) throws SQLException {
        String sql="""
                SELECT gs.*,g.MaPhong,g.HangGhe,g.SoGhe,g.TrangThai AS PhysicalState,g.MaLoaiGhe,l.TenLoaiGhe,
                       v.HangGheLucPhatHanh,v.SoGheLucPhatHanh,v.TenLoaiGheLucPhatHanh,b.MaLoaiGhe AS QuotedSeatType
                FROM GHE_SUATCHIEU gs JOIN GHE g ON g.MaGhe=gs.MaGhe JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe
                LEFT JOIN VE v ON v.MaSuat=gs.MaSuat AND v.MaGhe=gs.MaGhe
                LEFT JOIN BANGGIAVE b ON b.MaGia=gs.MaGia
                WHERE (? IS NULL OR gs.MaSuat=?) AND (? IS NULL OR gs.MaGiaoDich=?) ORDER BY gs.MaSuat,g.HangGhe,g.SoGhe
                """;
        try(PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setObject(1,show); ps.setObject(2,show); ps.setString(3,transaction); ps.setString(4,transaction);
            try(ResultSet rs=ps.executeQuery()) {
                List<GheSuatChieu> result=new ArrayList<>();
                while(rs.next()) {
                    boolean snapshot=transaction!=null && rs.getString("HangGheLucPhatHanh")!=null;
                    Ghe seat=new Ghe(rs.getInt("MaGhe"),rs.getString("MaPhong"),new LoaiGhe(snapshot?rs.getInt("QuotedSeatType"):rs.getInt("MaLoaiGhe"),snapshot?rs.getString("TenLoaiGheLucPhatHanh"):rs.getString("TenLoaiGhe")),
                            snapshot?rs.getString("HangGheLucPhatHanh"):rs.getString("HangGhe"),snapshot?rs.getInt("SoGheLucPhatHanh"):rs.getInt("SoGhe"),rs.getString("PhysicalState"));
                    Timestamp expires=rs.getTimestamp("ThoiGianHetHanGiu");
                    result.add(new GheSuatChieu(rs.getInt("MaSuat"),seat,rs.getString("MaGiaoDich"),rs.getString("TrangThai"),expires==null?null:expires.toLocalDateTime(),
                            rs.getObject("MaGia")==null?null:rs.getInt("MaGia"),rs.getBigDecimal("GiaVeChot")));
                }
                return result;
            }
        }
    }
    private static void expire(Connection c,int show) throws SQLException {
        List<String> owners=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement("SELECT DISTINCT g.MaGiaoDich FROM GHE_SUATCHIEU gs JOIN GIAODICH g ON g.MaGiaoDich=gs.MaGiaoDich JOIN SUATCHIEU s ON s.MaSuat=gs.MaSuat LEFT JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich "
                +"WHERE gs.MaSuat=? AND gs.TrangThai='Đang giữ' AND g.TrangThai='Chờ thanh toán' AND t.MaThanhToan IS NULL AND (gs.ThoiGianHetHanGiu IS NULL OR gs.ThoiGianHetHanGiu<=NOW() OR s.ThoiGianBatDau<=NOW()) ORDER BY g.MaGiaoDich")) {
            ps.setInt(1,show); try(ResultSet rs=ps.executeQuery()) { while(rs.next()) owners.add(rs.getString(1)); }
        }
        for(String id:owners) { GiuGheDAO.releaseOwners(c,List.of(id)); execute(c,"UPDATE GIAODICH SET TrangThai='Hết hạn' WHERE MaGiaoDich=?",id); }
    }
    private static void held(Connection c,GioHang cart,boolean deadline) throws SQLException {
        if(cart.ghe().isEmpty() || cart.suat()==null) throw new IllegalArgumentException("Giao dịch không còn ghế được giữ. Vui lòng chọn lại");
        LocalDateTime now=now(c);
        for(GheSuatChieu seat:cart.ghe()) if(!"Đang giữ".equals(seat.trangThai()) || !"Sẵn sàng sử dụng".equals(seat.ghe().trangThai())
                || (deadline && (seat.thoiGianHetHanGiu()==null || !seat.thoiGianHetHanGiu().isAfter(now)))) throw new IllegalArgumentException("Ghế không còn được giữ hợp lệ. Vui lòng kiểm tra lại giao dịch");
    }
    private static boolean quoted(GioHang cart) {
        if(cart.ghe().isEmpty() || cart.ghe().stream().anyMatch(g -> g.maGia()==null || g.giaVeChot()==null)) return false;
        BigDecimal sum=cart.ghe().stream().map(GheSuatChieu::giaVeChot).reduce(BigDecimal.ZERO,BigDecimal::add);
        return sum.signum()>0 && sum.compareTo(cart.giaoDich().tongTien())==0;
    }
    private static void requireQuote(GioHang cart) { if(!quoted(cart)) throw new IllegalArgumentException("Vui lòng tính tiền đầy đủ trước khi thanh toán/phát hành"); }
    private static void pending(Connection c,GiaoDich g) throws SQLException {
        if(!"Chờ thanh toán".equals(g.trangThai()) || GiaoDichDAO.payment(c,g.maGiaoDich())!=null) throw new IllegalArgumentException("Giao dịch không còn chờ thanh toán. Vui lòng kiểm tra giao dịch");
    }
    private static void own(GiaoDich g,String employee) { if(!g.maNV().equalsIgnoreCase(employee)) throw new IllegalArgumentException("Chỉ nhân viên tạo giao dịch được tiếp tục xử lý giao dịch này"); }
    private static void sameShow(GioHang cart,int show) { if(cart.suat()==null || cart.suat().maSuat()!=show) throw new IllegalArgumentException("Vui lòng hủy giao dịch cũ trước khi đổi suất hoặc tạo giao dịch mới"); }
    private static void selling(Connection c,Slot slot) throws SQLException {
        if("Đã hủy".equals(slot.show().trangThai()) || !slot.show().thoiGianBatDau().isAfter(now(c)) || !"Đang hoạt động".equals(slot.room().trangThai())
                || !List.of("Sắp chiếu","Đang chiếu").contains(slot.filmState())) throw new IllegalArgumentException("Suất chiếu không còn được phép bán vé");
    }
    private static void money(BigDecimal amount) { if(amount.signum()<=0 || amount.compareTo(MAX)>0) throw new IllegalArgumentException("Tổng tiền vượt phạm vi cho phép"); }
    private static void release(Connection c,String id) throws SQLException { execute(c,"UPDATE GHE_SUATCHIEU SET TrangThai='Trống',MaGiaoDich=NULL,ThoiGianHetHanGiu=NULL,MaGia=NULL,GiaVeChot=NULL WHERE MaGiaoDich=? AND TrangThai='Đang giữ'",id); }
    private static GiaoDich optional(Connection c,String id) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT MaGiaoDich FROM GIAODICH WHERE MaGiaoDich=?")) { ps.setString(1,id); try(ResultSet rs=ps.executeQuery()) { return rs.next()?GiaoDichDAO.get(c,id,true):null; } }
    }
    private static void lockSeats(Connection c,int show) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT MaGhe FROM GHE_SUATCHIEU WHERE MaSuat=? ORDER BY MaGhe FOR UPDATE")) { ps.setInt(1,show); try(ResultSet rs=ps.executeQuery()) { while(rs.next()) rs.getInt(1); } }
    }
    private static SuatChieu readShow(ResultSet rs) throws SQLException { return new SuatChieu(rs.getInt("MaSuat"),rs.getString("MaPhim"),rs.getString("MaPhong"),rs.getInt("MaLoaiThoiDiem"),rs.getTimestamp("ThoiGianBatDau").toLocalDateTime(),rs.getTimestamp("ThoiGianTraPhong").toLocalDateTime(),rs.getString("TrangThai")); }
    private static LocalDateTime now(Connection c) throws SQLException { try(PreparedStatement ps=c.prepareStatement("SELECT NOW()");ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getTimestamp(1).toLocalDateTime(); } }
    private static <T> T tx(Work<T> work) throws SQLException {
        try(Connection c=Database.connect()) { PhongChieuDAO.begin(c); try { T result=work.run(c); c.commit(); return result; } catch(SQLException|RuntimeException e) { PhongChieuDAO.rollback(c,e); throw e; } }
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException { try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); } }
}
