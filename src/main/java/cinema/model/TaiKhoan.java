package cinema.model;

public record TaiKhoan(int maTK, String tenDangNhap, String matKhauHash,
        String trangThai, VaiTro vaiTro, NhanVien nhanVien) {
    public boolean dangHoatDong() {
        return "Đang hoạt động".equals(trangThai);
    }

    @Override
    public String toString() {
        return "TaiKhoan[maTK=" + maTK + ", tenDangNhap=" + tenDangNhap + "]";
    }
}
