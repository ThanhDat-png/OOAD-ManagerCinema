package cinema.model;

import java.time.LocalDate;

public record NhanVien(String maNV, String hoTen, LocalDate ngaySinh,
        String soDienThoai, String email, String diaChi, String chucVu) { }
