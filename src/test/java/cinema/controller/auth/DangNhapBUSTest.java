package cinema.controller.auth;

import cinema.dao.TaiKhoanDAO;
import cinema.model.TaiKhoan;
import cinema.model.VaiTro;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DangNhapBUSTest {
    private static final String HASH = PasswordHasher.hash("Cinema@123".toCharArray());

    private DangNhapBUS service(String status, VaiTro role) {
        return new DangNhapBUS(new TaiKhoanDAO() {
            @Override
            public Optional<TaiKhoan> findByUsername(String username) {
                return "demo".equals(username)
                        ? Optional.of(new TaiKhoan(1, username, HASH, status, role, null)) : Optional.empty();
            }
        });
    }

    @Test void acceptsBothRoles() throws Exception {
        for (VaiTro role : VaiTro.values()) {
            PhienDangNhap session = service("Đang hoạt động", role).login(" demo ", "Cinema@123".toCharArray());
            assertEquals(role, session.vaiTro());
            assertEquals(1, session.maTK());
        }
    }

    @Test void rejectsMissingInputBeforeDatabase() {
        DangNhapBUS service = new DangNhapBUS(new TaiKhoanDAO() {
            @Override public Optional<TaiKhoan> findByUsername(String username) { fail("Unexpected DB access"); return Optional.empty(); }
        });
        assertThrows(IllegalArgumentException.class, () -> service.login(" ", new char[]{'a'}));
        assertThrows(IllegalArgumentException.class, () -> service.login("demo", new char[0]));
        assertThrows(IllegalArgumentException.class, () -> service.login(null, null));
    }

    @Test void usesSameMessageForUnknownAccountAndWrongPassword() {
        DangNhapBUS service = service("Đang hoạt động", VaiTro.ADMIN);
        String message = assertThrows(IllegalArgumentException.class,
                () -> service.login("missing", "Cinema@123".toCharArray())).getMessage();
        assertEquals(message, assertThrows(IllegalArgumentException.class,
                () -> service.login("demo", "wrong".toCharArray())).getMessage());
    }

    @Test void rejectsLockedInactiveAndUnknownStatus() {
        for (String status : new String[]{"Bị khóa", "Ngừng hoạt động", "unknown"}) {
            assertEquals("Tài khoản không được phép truy cập. Vui lòng liên hệ Admin",
                    assertThrows(IllegalArgumentException.class,
                            () -> service(status, VaiTro.ADMIN).login("demo", "Cinema@123".toCharArray())).getMessage());
        }
    }

    @Test void propagatesDatabaseFailureWithoutSession() {
        DangNhapBUS service = new DangNhapBUS(new TaiKhoanDAO() {
            @Override public Optional<TaiKhoan> findByUsername(String username) throws SQLException { throw new SQLException("offline"); }
        });
        assertThrows(SQLException.class, () -> service.login("demo", "Cinema@123".toCharArray()));
    }

    @Test void preservesPasswordWhitespace() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> service("Đang hoạt động", VaiTro.ADMIN).login("demo", " Cinema@123 ".toCharArray()));
    }

    @Test void rejectsMalformedHashes() {
        for (String hash : new String[]{"plaintext", "pbkdf2-sha256$x$a$b", "pbkdf2-sha256$2147483647$a$b"}) {
            assertFalse(PasswordHasher.verify("Cinema@123".toCharArray(), hash));
        }
        assertFalse(PasswordHasher.verify(new char[0], null));
    }

    @Test void saltsHashesAndVerifies() {
        String another = PasswordHasher.hash("Cinema@123".toCharArray());
        assertNotEquals(HASH, another);
        assertTrue(PasswordHasher.verify("Cinema@123".toCharArray(), another));
    }
}
