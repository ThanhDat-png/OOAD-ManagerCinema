package cinema.controller.auth;

import cinema.model.NhanVien;
import cinema.model.VaiTro;

/** Chỉ mang thông tin cần cho phiên, mật khẩu băm nằm tại bước xác thực. */
public record PhienDangNhap(int maTK, String tenDangNhap, VaiTro vaiTro, NhanVien nhanVien) { }
