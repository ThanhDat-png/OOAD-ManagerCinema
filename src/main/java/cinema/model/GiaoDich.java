package cinema.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record GiaoDich(String maGiaoDich,String maNV,LocalDateTime thoiGianTao,BigDecimal tongTien,String trangThai) { }
