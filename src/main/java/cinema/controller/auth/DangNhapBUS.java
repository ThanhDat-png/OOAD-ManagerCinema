package cinema.controller.auth;

import cinema.dao.TaiKhoanDAO;
import cinema.model.TaiKhoan;
import java.sql.SQLException;

public final class DangNhapBUS {
    private final TaiKhoanDAO accounts;
    public DangNhapBUS(TaiKhoanDAO accounts) { this.accounts = accounts; }

    public PhienDangNhap login(String username, char[] password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
        }
        TaiKhoan account = accounts.findByUsername(username.trim()).orElse(null);
        if (account == null || !PasswordHasher.verify(password, account.matKhauHash())) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }
        if (!account.dangHoatDong()) {
            throw new IllegalArgumentException("Tài khoản không được phép truy cập. Vui lòng liên hệ Admin");
        }
        return new PhienDangNhap(account.maTK(), account.tenDangNhap(), account.vaiTro(), account.nhanVien());
    }
}
