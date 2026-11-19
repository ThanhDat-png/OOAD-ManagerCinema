package cinema.web.security;

import cinema.controller.auth.DangNhapBUS;
import cinema.controller.auth.PhienDangNhap;
import cinema.dao.TaiKhoanDAO;
import cinema.model.NhanVien;
import cinema.model.TaiKhoan;
import cinema.model.VaiTro;
import cinema.web.db.WebDb;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
final class WebAccounts extends TaiKhoanDAO implements AuthenticationProvider {
    private final WebDb db;

    WebAccounts(WebDb db) { this.db = db; }

    @Override
    public Optional<TaiKhoan> findByUsername(String username) {
        return db.query("""
                SELECT t.MaTK, t.TenDangNhap, t.MatKhauHash, t.TrangThai,
                       v.TenVaiTro, n.MaNV, n.HoTen
                FROM TAIKHOAN t JOIN VAITRO v ON t.MaVaiTro = v.MaVaiTro
                LEFT JOIN NHANVIEN n ON t.MaNV = n.MaNV
                WHERE t.TenDangNhap = ?
                """, username).stream().findFirst().map(this::account);
    }

    private TaiKhoan account(Map<String,Object> row) {
        var employee = new NhanVien((String)row.get("MaNV"), (String)row.get("HoTen"),
                null, null, null, null, null);
        return new TaiKhoan(((Number)row.get("MaTK")).intValue(), (String)row.get("TenDangNhap"),
                (String)row.get("MatKhauHash"), (String)row.get("TrangThai"),
                VaiTro.fromDatabase((String)row.get("TenVaiTro")), employee);
    }

    @Override
    public Authentication authenticate(Authentication request) {
        char[] password = request.getCredentials().toString().toCharArray();
        try {
            PhienDangNhap user = new DangNhapBUS(this).login(request.getName(), password);
            String authority = user.vaiTro() == VaiTro.ADMIN ? "ROLE_ADMIN" : "ROLE_STAFF";
            return UsernamePasswordAuthenticationToken.authenticated(user, null,
                    List.of(new SimpleGrantedAuthority(authority)));
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException(ex.getMessage());
        } catch (SQLException | RuntimeException ex) {
            throw new AuthenticationServiceException("Chưa thể kết nối cơ sở dữ liệu. Vui lòng thử lại.", ex);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }
}
