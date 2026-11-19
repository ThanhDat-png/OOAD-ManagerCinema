package cinema.web.staff;

import cinema.controller.sales.BanVeBUS;
import cinema.web.db.WebDb;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CounterSales {
    static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    static final String WAIT = "Chờ thanh toán", PAYING = "Đang thanh toán", PAID = "Đã thanh toán";
    private static final String SHOW = """
        SELECT s.*, p.TenPhim, p.ThoiLuong, p.DinhDangChieu, r.TenPhong,
               strftime('%d/%m · %H:%M',s.ThoiGianBatDau) AS Lich
        FROM SUATCHIEU s JOIN PHIM p ON p.MaPhim=s.MaPhim JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong
        """;
    private static final String SELLABLE = """
        s.TrangThai<>'Đã hủy' AND s.ThoiGianBatDau>? AND p.TrangThai IN ('Sắp chiếu','Đang chiếu')
        AND r.TrangThai='Đang hoạt động'
        """;
    private static final String CLEAR = """
        UPDATE GHE_SUATCHIEU SET TrangThai='Trống',MaGiaoDich=NULL,ThoiGianHetHanGiu=NULL,MaGia=NULL,GiaVeChot=NULL
        WHERE TrangThai='Đang giữ' AND """;
    private static final String ORDER = """
        SELECT gd.*,n.HoTen,t.PhuongThuc,t.ThoiGianThanhToan
        FROM GIAODICH gd JOIN NHANVIEN n ON n.MaNV=gd.MaNV LEFT JOIN THANHTOAN t ON t.MaGiaoDich=gd.MaGiaoDich
        """;
    private static final String SEATS = """
        SELECT h.MaSuat,h.MaGhe,h.MaGiaoDich,h.TrangThai,h.GiaVeChot,h.ThoiGianHetHanGiu,g.HangGhe,g.SoGhe,
               l.TenLoaiGhe,g.TrangThai AS TinhTrang, g.HangGhe || g.SoGhe AS Cho
        FROM GHE_SUATCHIEU h JOIN GHE g ON g.MaGhe=h.MaGhe JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe
        """;
    private final WebDb db;
    public CounterSales(WebDb db) { this.db=db; }

    static Map<String,Object> first(List<Map<String,Object>> values, String message) {
        if(values.isEmpty()) throw new IllegalArgumentException(message);
        return values.get(0);
    }
    static String now() { return LocalDateTime.now(CINEMA_ZONE).format(SQL_TIME); }
    static int number(Map<String,Object> row,String key) { return ((Number)row.get(key)).intValue(); }
    static String id(String prefix) { return prefix+UUID.randomUUID().toString().replace("-","").substring(0,19); }

    public List<Map<String,Object>> shows(String film,String day,String time) {
        String pattern=film.strip().replace("=","==").replace("%","=%").replace("_","=_");
        return db.query(SHOW+" WHERE "+SELLABLE+"""
             AND p.TenPhim LIKE ? ESCAPE '=' AND (?='' OR date(s.ThoiGianBatDau)=?)
             AND (?='' OR strftime('%H:%M',s.ThoiGianBatDau)=?) ORDER BY s.ThoiGianBatDau,s.MaSuat
             """,now(),"%"+pattern+"%",day,day,time,time);
    }
    public Map<String,Object> show(int show) {
        return first(db.query(SHOW+" WHERE s.MaSuat=?",show),"Không tìm thấy suất chiếu");
    }
    public Map<String,Object> sellableShow(int show) {
        return first(db.query(SHOW+" WHERE "+SELLABLE+" AND s.MaSuat=?",now(),show),
                "Suất chiếu không còn được phép bán vé. Vui lòng chọn suất chiếu khác");
    }
    public List<Map<String,Object>> seats(int show) {
        return db.write(tx->{ expire(tx); return tx.query(SEATS+" WHERE h.MaSuat=? ORDER BY g.HangGhe,g.SoGhe",show); });
    }
    public Map<String,Object> order(String id) {
        return first(db.write(tx->{ expire(tx); return tx.query(ORDER+" WHERE gd.MaGiaoDich=?",id); }),
                "Không tìm thấy giao dịch phù hợp");
    }
    public List<Map<String,Object>> selected(String id) {
        return db.query(SEATS+" WHERE h.MaGiaoDich=? ORDER BY g.HangGhe,g.SoGhe",id);
    }

    private static void expire(WebDb.Tx tx) {
        tx.update("""
            UPDATE GIAODICH SET TrangThai='Hết hạn' WHERE TrangThai='Chờ thanh toán' AND MaGiaoDich IN
            (SELECT h.MaGiaoDich FROM GHE_SUATCHIEU h JOIN SUATCHIEU s ON s.MaSuat=h.MaSuat
             WHERE h.TrangThai='Đang giữ' AND (h.ThoiGianHetHanGiu<=? OR s.ThoiGianBatDau<=?))
            """,now(),now());
        tx.update(CLEAR+" MaGiaoDich IN (SELECT MaGiaoDich FROM GIAODICH WHERE TrangThai='Hết hạn')");
    }
    private static Map<String,Object> owned(WebDb.Tx tx,String order,String employee) {
        return first(tx.query("SELECT * FROM GIAODICH WHERE MaGiaoDich=? AND MaNV=?",order,employee),
                "Bạn không thể tiếp tục giao dịch này");
    }
    private static void require(boolean valid,String message) {
        if(!valid) throw new IllegalArgumentException(message);
    }
    private static void sellable(WebDb.Tx tx,int show) {
        first(tx.query(SHOW+" WHERE "+SELLABLE+" AND s.MaSuat=?",now(),show),"Suất chiếu không còn được phép bán vé");
    }
    public void hold(String order,String employee,int show,List<Integer> selection) {
        require(selection!=null&&!selection.isEmpty(),"Vui lòng chọn ghế còn trống");
        var unique=new LinkedHashSet<>(selection);
        db.write(tx->{
            expire(tx);
            sellable(tx,show);
            var old=tx.query("SELECT * FROM GIAODICH WHERE MaGiaoDich=?",order);
            if(old.isEmpty()) tx.update("INSERT INTO GIAODICH(MaGiaoDich,MaNV,ThoiGianTao,TongTien,TrangThai) VALUES(?,?,?,0,?)",order,employee,now(),WAIT);
            var cart=owned(tx,order,employee);
            require(WAIT.equals(cart.get("TrangThai")),"Giao dịch không còn chờ thanh toán. Vui lòng kiểm tra giao dịch");
            tx.update(CLEAR+" MaGiaoDich=?",order);
            String end=LocalDateTime.now(CINEMA_ZONE).plusMinutes(holdMinutes()).format(SQL_TIME);
            var placeholders=String.join(",",Collections.nCopies(unique.size(),"?"));
            var parameters=new ArrayList<Object>(List.of(order,end,show,show));
            parameters.addAll(unique);
            int changed=tx.update("""
                    UPDATE GHE_SUATCHIEU SET TrangThai='Đang giữ',MaGiaoDich=?,
                    ThoiGianHetHanGiu=min(?,(SELECT ThoiGianBatDau FROM SUATCHIEU WHERE MaSuat=?))
                    WHERE MaSuat=? AND TrangThai='Trống' AND EXISTS
                    (SELECT 1 FROM GHE g JOIN SUATCHIEU s ON s.MaPhong=g.MaPhong
                     WHERE g.MaGhe=GHE_SUATCHIEU.MaGhe AND s.MaSuat=GHE_SUATCHIEU.MaSuat AND g.TrangThai='Sẵn sàng sử dụng')
                    AND MaGhe IN ("""+placeholders+")",parameters.toArray());
            require(changed==unique.size(),"Ghế không thể chọn. Vui lòng cập nhật sơ đồ và chọn lại");
            tx.update("UPDATE GIAODICH SET TongTien=0 WHERE MaGiaoDich=?",order);
            return null;
        });
    }
    private static int holdMinutes() {
        var file=java.nio.file.Path.of("config","sales.properties");
        if(!java.nio.file.Files.exists(file)) return 5;
        var config=new Properties();
        try(var reader=java.nio.file.Files.newBufferedReader(file)) { config.load(reader); }
        catch(java.io.IOException ex) { throw new IllegalStateException("Không đọc được cấu hình giữ ghế",ex); }
        int minutes=Integer.parseInt(config.getProperty("holdMinutes","5").strip());
        require(minutes>0&&minutes<=60,"Thời hạn giữ ghế cần từ 1 đến 60 phút");
        return minutes;
    }
    private static List<Map<String,Object>> held(WebDb.Tx tx,String id) {
        var seats=tx.query("""
            SELECT h.*,g.TrangThai AS TinhTrang,b.MaGia AS MucGia,b.GiaVe
            FROM GHE_SUATCHIEU h JOIN GHE g ON g.MaGhe=h.MaGhe JOIN SUATCHIEU s ON s.MaSuat=h.MaSuat
            JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong LEFT JOIN BANGGIAVE b ON b.MaLoaiGhe=g.MaLoaiGhe
            AND b.MaLoaiPhong=r.MaLoaiPhong AND b.MaLoaiThoiDiem=s.MaLoaiThoiDiem WHERE h.MaGiaoDich=?
            """,id);
        require(!seats.isEmpty()&&seats.stream().allMatch(s->"Đang giữ".equals(s.get("TrangThai"))&&"Sẵn sàng sử dụng".equals(s.get("TinhTrang"))),
                "Ghế không còn được giữ hợp lệ. Vui lòng kiểm tra và chọn lại ghế");
        return seats;
    }
    public void quote(String id,String employee) {
        db.write(tx->{
            expire(tx);
            var cart=owned(tx,id,employee);
            require(WAIT.equals(cart.get("TrangThai")),"Giao dịch không còn chờ thanh toán");
            long total=0;
            var heldSeats=held(tx,id);
            sellable(tx,number(heldSeats.get(0),"MaSuat"));
            if(((Number)cart.get("TongTien")).longValue()>0) { quoted(cart,heldSeats); return null; }
            for(var seat:heldSeats) {
                require(seat.get("MucGia")!=null,"Chưa thiết lập giá vé cho lựa chọn này");
                long amount=((Number)seat.get("GiaVe")).longValue();
                total=Math.addExact(total,amount);
            }
            tx.update("""
                UPDATE GHE_SUATCHIEU AS h SET MaGia=b.MaGia,GiaVeChot=b.GiaVe
                FROM SUATCHIEU s JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong JOIN GHE g ON g.MaPhong=r.MaPhong
                JOIN BANGGIAVE b ON b.MaLoaiGhe=g.MaLoaiGhe AND b.MaLoaiPhong=r.MaLoaiPhong AND b.MaLoaiThoiDiem=s.MaLoaiThoiDiem
                WHERE h.MaGiaoDich=? AND h.MaSuat=s.MaSuat AND h.MaGhe=g.MaGhe
                """,id);
            tx.update("UPDATE GIAODICH SET TongTien=? WHERE MaGiaoDich=?",total,id);
            return null;
        });
    }
    private static void quoted(Map<String,Object> cart,List<Map<String,Object>> seats) {
        long sum=0;
        for(var seat:seats) {
            require(seat.get("MaGia")!=null&&seat.get("GiaVeChot")!=null,"Vui lòng tính tiền trước khi thanh toán");
            sum=Math.addExact(sum,((Number)seat.get("GiaVeChot")).longValue());
        }
        require(sum>0&&sum==((Number)cart.get("TongTien")).longValue(),"Vui lòng tính lại tiền vé");
    }
    public void startPayment(String id,String employee) {
        db.write(tx->{
            expire(tx); var cart=owned(tx,id,employee);
            if(PAYING.equals(cart.get("TrangThai"))||PAID.equals(cart.get("TrangThai"))) return null;
            require(WAIT.equals(cart.get("TrangThai")),"Ghế không còn được giữ hợp lệ. Vui lòng chọn lại");
            var seats=held(tx,id); sellable(tx,number(seats.get(0),"MaSuat")); quoted(cart,seats);
            tx.update("UPDATE GIAODICH SET TrangThai=? WHERE MaGiaoDich=?",PAYING,id);
            return null;
        });
    }
    public void pay(String id,String employee,String method,boolean confirmed) {
        require(BanVeBUS.PHUONG_THUC.contains(method),"Vui lòng chọn phương thức thanh toán");
        require(confirmed,"Chỉ hoàn tất sau khi đã xác nhận nhận đủ tiền tại quầy");
        db.write(tx->{
            var cart=owned(tx,id,employee);
            if(PAID.equals(cart.get("TrangThai"))) {
                first(tx.query("SELECT MaThanhToan FROM THANHTOAN WHERE MaGiaoDich=? AND PhuongThuc=? AND SoTien=?",id,method,cart.get("TongTien")),"Giao dịch đã thanh toán. Vui lòng kiểm tra giao dịch");
                return null;
            }
            require(PAYING.equals(cart.get("TrangThai")),"Vui lòng bắt đầu thanh toán trước khi nhận tiền");
            quoted(cart,held(tx,id));
            tx.update("INSERT INTO THANHTOAN(MaGiaoDich,PhuongThuc,SoTien,ThoiGianThanhToan) VALUES (?,?,?,?)",id,method,cart.get("TongTien"),now());
            tx.update("UPDATE GHE_SUATCHIEU SET TrangThai='Đã bán',ThoiGianHetHanGiu=NULL WHERE MaGiaoDich=?",id);
            tx.update("UPDATE GIAODICH SET TrangThai=? WHERE MaGiaoDich=?",PAID,id);
            return null;
        });
    }
    public void cancel(String id,String employee,boolean noMoneyReceived) {
        db.write(tx->{
            var cart=owned(tx,id,employee); Object state=cart.get("TrangThai");
            if(Set.of("Đã hủy","Hết hạn").contains(state)) return null;
            require(WAIT.equals(state)||(PAYING.equals(state)&&noMoneyReceived),"Cần kiểm tra giao dịch và xác nhận chưa nhận tiền trước khi hủy");
            tx.update(CLEAR+" MaGiaoDich=?",id);
            tx.update("UPDATE GIAODICH SET TrangThai='Đã hủy' WHERE MaGiaoDich=?",id);
            return null;
        });
    }
    public void issue(String id,String employee) {
        db.write(tx->{
            var cart=owned(tx,id,employee);
            require(PAID.equals(cart.get("TrangThai")),"Giao dịch chưa được thanh toán hợp lệ");
            first(tx.query("SELECT MaThanhToan FROM THANHTOAN WHERE MaGiaoDich=? AND SoTien=? AND SoTien>0",id,cart.get("TongTien")),"Giao dịch chưa được thanh toán hợp lệ");
            var seats=tx.query(SEATS+" WHERE h.MaGiaoDich=?",id);
            require(!seats.isEmpty() && seats.stream().allMatch(seat->"Đã bán".equals(seat.get("TrangThai"))),"Ghế chưa được ghi nhận đã bán");
            tx.update("""
                    INSERT INTO VE(MaVe,MaQR,MaGiaoDich,MaSuat,MaGhe,MaGia,GiaVe,TrangThai,ThoiGianPhatHanh,
                    HangGheLucPhatHanh,SoGheLucPhatHanh,TenLoaiGheLucPhatHanh)
                    SELECT 'V'||lower(substr(hex(randomblob(10)),1,19)),lower(hex(randomblob(16))),h.MaGiaoDich,h.MaSuat,
                    h.MaGhe,h.MaGia,h.GiaVeChot,'Chưa sử dụng',?,g.HangGhe,g.SoGhe,l.TenLoaiGhe
                    FROM GHE_SUATCHIEU h JOIN GHE g ON g.MaGhe=h.MaGhe JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe
                    WHERE h.MaGiaoDich=? ON CONFLICT(MaSuat,MaGhe) DO NOTHING
                    """,now(),id);
            return null;
        });
    }
    public List<Map<String,Object>> search(String code,String from,String to) {
        require(!code.isBlank()||!from.isBlank()||!to.isBlank(),"Vui lòng nhập mã giao dịch, mã vé hoặc khoảng thời gian");
        try {
            if(!from.isBlank()||!to.isBlank()) require(!LocalDate.parse(to).isBefore(LocalDate.parse(from)),"Khoảng thời gian không hợp lệ");
        } catch(java.time.format.DateTimeParseException ex) { throw new IllegalArgumentException("Khoảng thời gian không hợp lệ"); }
        return db.write(tx->{
            expire(tx);
            return tx.query(ORDER+"""
             WHERE (?='' OR gd.MaGiaoDich=? OR EXISTS (SELECT 1 FROM VE v WHERE v.MaGiaoDich=gd.MaGiaoDich AND v.MaVe=?))
             AND (?='' OR date(gd.ThoiGianTao) BETWEEN ? AND ?) ORDER BY gd.ThoiGianTao DESC
             """,code,code,code,from,from,to);
        });
    }
}
