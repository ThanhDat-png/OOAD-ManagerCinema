package cinema.model;
public record PhongChieu(String maPhong, LoaiPhong loaiPhong, String tenPhong,
        int thoiGianDonPhong, String trangThai) {
    @Override public String toString() { return tenPhong + " · " + loaiPhong.dinhDangChieu(); }
}
