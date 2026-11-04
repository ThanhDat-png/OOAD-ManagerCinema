package cinema.model;

import java.math.BigDecimal;

public record BangGiaVe(int maGia, LoaiGhe loaiGhe, LoaiPhong loaiPhong,
        LoaiThoiDiem loaiThoiDiem, BigDecimal giaVe) { }
