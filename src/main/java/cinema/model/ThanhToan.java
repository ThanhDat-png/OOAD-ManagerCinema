package cinema.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ThanhToan(int maThanhToan,String maGiaoDich,String phuongThuc,BigDecimal soTien,LocalDateTime thoiGianThanhToan) { }
