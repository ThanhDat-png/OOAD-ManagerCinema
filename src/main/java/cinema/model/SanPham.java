package cinema.model;

import java.math.BigDecimal;

public record SanPham(String maSP, String tenSP, String moTa, String donViTinh,
        BigDecimal giaBan, int soLuongTon, String tinhTrangKinhDoanh) { }
