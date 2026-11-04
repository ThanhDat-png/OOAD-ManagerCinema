package cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Ve(String maVe, String maGiaoDich, int maSuat, int maGhe, int maGia,
        String maQR, BigDecimal giaVe, String trangThai, LocalDateTime thoiGianPhatHanh,
        String hangGheLucPhatHanh, int soGheLucPhatHanh, String tenLoaiGheLucPhatHanh) { }
