package cinema.web.staff;

import cinema.controller.admission.ThoiGianVaoPhong;
import cinema.web.db.WebDb;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class Admissions {
    private static final String TICKETS = """
        SELECT v.*,p.TenPhim,r.TenPhong,s.MaPhong,s.ThoiGianBatDau,s.TrangThai AS TrangThaiSuat,
            r.TrangThai AS TrangThaiPhong,v.HangGheLucPhatHanh || v.SoGheLucPhatHanh AS Cho,
            strftime('%d/%m/%Y · %H:%M',s.ThoiGianBatDau) AS Lich,
            (gd.TrangThai='Đã thanh toán' AND gd.TongTien>0 AND t.SoTien=gd.TongTien
             AND gd.TongTien=(SELECT SUM(h.GiaVeChot) FROM GHE_SUATCHIEU h WHERE h.MaGiaoDich=gd.MaGiaoDich)
             AND NOT EXISTS (SELECT 1 FROM GHE_SUATCHIEU h WHERE h.MaGiaoDich=gd.MaGiaoDich
             AND (h.TrangThai<>'Đã bán' OR h.MaGia IS NULL OR h.GiaVeChot IS NULL))) AS DaThanhToan,
            (h.TrangThai='Đã bán' AND h.MaGiaoDich=v.MaGiaoDich AND h.MaGia=v.MaGia AND h.GiaVeChot=v.GiaVe) AS GheDaBan
        FROM VE v JOIN SUATCHIEU s ON s.MaSuat=v.MaSuat JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong
        JOIN PHIM p ON p.MaPhim=s.MaPhim JOIN GIAODICH gd ON gd.MaGiaoDich=v.MaGiaoDich
        LEFT JOIN THANHTOAN t ON t.MaGiaoDich=gd.MaGiaoDich JOIN GHE_SUATCHIEU h ON h.MaSuat=v.MaSuat AND h.MaGhe=v.MaGhe
        """;
    private final WebDb db;
    final ThoiGianVaoPhong window=ThoiGianVaoPhong.load();
    public Admissions(WebDb db) { this.db=db; }

    public List<Map<String,Object>> rooms() { return db.query("SELECT MaPhong,TenPhong FROM PHONGCHIEU ORDER BY TenPhong"); }
    public List<Map<String,Object>> shows(String day) {
        return db.query("""
            SELECT s.MaSuat,r.MaPhong,p.TenPhim,r.TenPhong,strftime('%H:%M',s.ThoiGianBatDau) AS Gio
            FROM SUATCHIEU s JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong JOIN PHIM p ON p.MaPhim=s.MaPhim
            WHERE date(s.ThoiGianBatDau)=? AND s.TrangThai<>'Đã hủy' ORDER BY s.ThoiGianBatDau,r.TenPhong
            """,day);
    }
    public Map<String,Object> lookup(String code) {
        if(code==null||code.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mã vé hoặc quét mã QR");
        return CounterSales.first(db.query(TICKETS+" WHERE v.MaVe=? OR v.MaQR=?",code.strip(),code.strip()),"Vé không tồn tại");
    }
    public List<Map<String,Object>> tickets(String order) {
        return db.query(TICKETS+" WHERE v.MaGiaoDich=? ORDER BY v.HangGheLucPhatHanh,v.SoGheLucPhatHanh",order);
    }
    private static boolean affirmative(Object value) { return value instanceof Number n && n.intValue()==1; }

    public String failure(Map<String,Object> ticket,String room,Integer show) {
        if(room==null||room.isBlank()||show==null) return "Vui lòng chọn phòng và suất chiếu đang kiểm soát";
        if("Đã sử dụng".equals(ticket.get("TrangThai"))) return "Vé đã được sử dụng";
        if(!"Chưa sử dụng".equals(ticket.get("TrangThai"))) return "Trạng thái vé không hợp lệ";
        if(!affirmative(ticket.get("DaThanhToan"))) return "Vé chưa có thanh toán hợp lệ";
        if(!affirmative(ticket.get("GheDaBan"))) return "Thông tin ghế hoặc giá đã chốt của vé không hợp lệ";
        if(!room.equals(ticket.get("MaPhong"))) return "Vé không đúng phòng đang kiểm soát";
        if(show!=CounterSales.number(ticket,"MaSuat")) return "Vé không đúng suất chiếu đang kiểm soát";
        if("Đã hủy".equals(ticket.get("TrangThaiSuat"))) return "Suất chiếu đã bị hủy";
        if(!"Đang hoạt động".equals(ticket.get("TrangThaiPhong"))) return "Phòng chiếu không hoạt động";
        var start=LocalDateTime.parse(ticket.get("ThoiGianBatDau").toString(),CounterSales.SQL_TIME);
        var current=LocalDateTime.now(CounterSales.CINEMA_ZONE).withNano(0);
        if(current.isBefore(start.minusMinutes(window.truocPhut()))||current.isAfter(start.plusMinutes(window.sauPhut())))
            return "Ngoài thời gian cho phép vào phòng";
        return null;
    }
    public void confirm(String id,String room,int show) {
        db.write(tx->{
            var ticket=CounterSales.first(tx.query(TICKETS+" WHERE v.MaVe=?",id),"Vé không tồn tại");
            var error=failure(ticket,room,show);
            if(error!=null) throw new IllegalArgumentException(error);
            if(tx.update("UPDATE VE SET TrangThai='Đã sử dụng' WHERE MaVe=? AND TrangThai='Chưa sử dụng'",id)!=1)
                throw new IllegalArgumentException("Vé đã được sử dụng");
            return null;
        });
    }
    public byte[] qr(String id) throws IOException {
        var ticket=CounterSales.first(db.query("SELECT MaQR FROM VE WHERE MaVe=?",id),"Vé không tồn tại");
        try(var out=new ByteArrayOutputStream()) {
            var qr=new QRCodeWriter().encode(ticket.get("MaQR").toString(),BarcodeFormat.QR_CODE,240,240);
            MatrixToImageWriter.writeToStream(qr,"PNG",out);
            return out.toByteArray();
        } catch(WriterException ex) { throw new IllegalStateException("Không tạo được ảnh QR của vé đã lưu",ex); }
    }
}
