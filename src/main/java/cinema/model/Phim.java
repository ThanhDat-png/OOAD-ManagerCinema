package cinema.model;

import java.time.LocalDate;
import java.util.List;

public record Phim(String maPhim, String tenPhim, int thoiLuong, String daoDien,
        LocalDate ngayKhoiChieu, int gioiHanTuoi, String poster, String trangThai,
        String dinhDangChieu, List<TheLoai> theLoai) {
    public Phim { theLoai = List.copyOf(theLoai); }

    public Phim withTheLoai(List<TheLoai> genres) {
        return new Phim(maPhim, tenPhim, thoiLuong, daoDien, ngayKhoiChieu,
                gioiHanTuoi, poster, trangThai, dinhDangChieu, genres);
    }
}
