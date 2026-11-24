package cinema.model;

import cinema.dao.BanVeDAO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ChonGheKhach(
        String maChonGhe,
        int maSuat,
        String maPhim,
        String tenPhim,
        String maPhong,
        String tenPhong,
        LocalDateTime thoiGianBatDau,
        LocalDateTime thoiGianHetHan,
        String maGiaoDich,
        List<GheChon> ghe,
        BigDecimal tongTamTinh,
        boolean daTiepNhan,
        boolean daThanhToan,
        boolean lichThayDoi
) {
    public ChonGheKhach {
        ghe = List.copyOf(ghe);
    }

    public record PhimChieu(Phim phim, String theLoai) { }

    public record SuatKhach(SuatChieu suat, Phim phim, String tenPhong) { }

    public record GheSanh(
            int maGhe,
            String hangGhe,
            int soGhe,
            String viTri,
            String tenLoaiGhe,
            BigDecimal giaTamTinh,
            boolean khaDung
    ) { }

    public record GheChon(
            int maGhe,
            String viTri,
            String tenLoaiGhe,
            BigDecimal giaTamTinh
    ) { }

    public record YeuCauCho(
            String maChonGhe,
            int maSuat,
            String tenPhim,
            String tenPhong,
            LocalDateTime thoiGianBatDau,
            String danhSachGhe
    ) {
        @Override
        public String toString() {
            return tenPhim + " · " + tenPhong + " · Ghế " + danhSachGhe + " · " + maChonGhe;
        }
    }

    public record KetQuaTiepNhan(
            BanVeDAO.GioHang gioHang,
            int maSuat,
            boolean daThanhToan
    ) { }
}
