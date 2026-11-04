package cinema.model;
import java.time.LocalDateTime;

public record SuatChieu(int maSuat, String maPhim, String maPhong, int maLoaiThoiDiem,
        LocalDateTime thoiGianBatDau, LocalDateTime thoiGianTraPhong, String trangThai) {
    public SuatChieu withFilm(String film) {
        return new SuatChieu(maSuat, film, maPhong, maLoaiThoiDiem, thoiGianBatDau, thoiGianTraPhong, trangThai);
    }
    public SuatChieu withRoom(String room) {
        return new SuatChieu(maSuat, maPhim, room, maLoaiThoiDiem, thoiGianBatDau, thoiGianTraPhong, trangThai);
    }
    public SuatChieu withStart(LocalDateTime start) {
        return new SuatChieu(maSuat, maPhim, maPhong, maLoaiThoiDiem, start, thoiGianTraPhong, trangThai);
    }
}
