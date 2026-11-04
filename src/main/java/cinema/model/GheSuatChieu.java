package cinema.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record GheSuatChieu(int maSuat,Ghe ghe,String maGiaoDich,String trangThai,
        LocalDateTime thoiGianHetHanGiu,Integer maGia,BigDecimal giaVeChot) { }
