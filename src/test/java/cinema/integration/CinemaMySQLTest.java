package cinema.integration;

import cinema.controller.auth.*;
import cinema.controller.staff.NhanVienBUS;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in only, requires a dedicated demo database with migrations 001–003. */
@EnabledIfSystemProperty(named = "cinema.mysql.tests", matches = "true")
class CinemaMySQLTest {
    private final NhanVienDAO dao = new NhanVienDAO();
    private final String id = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
    private final String transaction = "TEST" + UUID.randomUUID().toString().substring(0, 12);
    private final String username = "test_" + id;
    private NhanVienBUS admin;

    @BeforeEach void setup() throws Exception {
        admin = new NhanVienBUS(new PhienDangNhap(1, "test_admin", VaiTro.ADMIN, null), dao);
        dao.insert(employee(id, "Hồ sơ %_ thử nghiệm"));
    }

    @AfterEach void cleanup() throws Exception {
        try (Connection c = Database.connect()) {
            for (String table : new String[]{"GIAODICH", "TAIKHOAN", "NHANVIEN"}) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE MaNV=?")) {
                    ps.setString(1, id); ps.executeUpdate();
                }
            }
        }
    }

    private NhanVien employee(String code, String name) {
        return new NhanVien(code, name, LocalDate.of(2000, 1, 2), "0912345678",
                "demo@example.com", "Địa chỉ thử nghiệm", "Nhân viên");
    }

    @Test void loginThroughJdbcForBothRolesAndRejectsBadStates() throws Exception {
        DangNhapBUS auth = new DangNhapBUS(new TaiKhoanDAO());
        assertEquals(VaiTro.ADMIN, auth.login("admin_demo", "Cinema@123".toCharArray()).vaiTro());
        assertEquals(VaiTro.NHAN_VIEN, auth.login("staff_demo", "Cinema@123".toCharArray()).vaiTro());
        assertThrows(IllegalArgumentException.class, () -> auth.login("locked_demo", "Cinema@123".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> auth.login("inactive_demo", "Cinema@123".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> auth.login("admin_demo", "wrong".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> auth.login("' OR 1=1 --", "wrong".toCharArray()));
        assertNotNull(auth.login("staff_demo", "Cinema@123".toCharArray()).nhanVien());
    }

    @Test void searchEscapesWildcardsAndSqlInput() throws Exception {
        assertTrue(admin.search("%_").stream().anyMatch(e -> e.maNV().equals(id)));
        assertEquals(1, admin.search(id).size());
        assertTrue(admin.search("' OR 1=1 --").isEmpty());
        assertFalse(admin.search("").isEmpty());
        assertTrue(admin.search("none_" + id).isEmpty());
    }

    @Test void createsUpdatesAndRejectsInvalidDataWithoutChangingStoredRecord() throws Exception {
        NhanVien created = admin.create(employee(null, "Nhân viên mới"));
        try {
            assertEquals(10, created.maNV().length());
            assertEquals("Nhân viên mới", admin.detail(created.maNV()).hoTen());
            admin.update(employee(created.maNV(), "Tên đã cập nhật"));
            assertEquals("Tên đã cập nhật", admin.detail(created.maNV()).hoTen());
            assertThrows(IllegalArgumentException.class, () -> admin.update(employee(created.maNV(), " ")));
            assertEquals("Tên đã cập nhật", admin.detail(created.maNV()).hoTen());
            NhanVien invalid = new NhanVien(created.maNV(), "A", LocalDate.now().plusYears(1), "x", "x", "A", "A");
            assertThrows(IllegalArgumentException.class, () -> admin.update(invalid));
        } finally { admin.delete(created.maNV()); }
    }

    @Test void staffCanOnlyReadOwnProfileAndCannotManage() throws Exception {
        NhanVienBUS staff = new NhanVienBUS(new PhienDangNhap(2, username, VaiTro.NHAN_VIEN,
                employee(id, "Tên trong phiên")), dao);
        assertEquals(id, staff.detail(id).maNV());
        assertThrows(IllegalArgumentException.class, () -> staff.detail("NV001"));
        assertThrows(IllegalArgumentException.class, () -> staff.search(""));
        assertThrows(IllegalArgumentException.class, () -> staff.create(employee(null, "A")));
        assertThrows(IllegalArgumentException.class, () -> staff.update(employee(id, "A")));
        assertThrows(IllegalArgumentException.class, () -> staff.delete(id));
        assertTrue(dao.find(id).isPresent());
    }

    private void account() throws Exception {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO TAIKHOAN (MaTK, MaVaiTro, MaNV, TenDangNhap, MatKhauHash, TrangThai) "
                        + "SELECT ?, MaVaiTro, ?, ?, ?, 'Đang hoạt động' FROM VAITRO WHERE TenVaiTro='Nhân viên'")) {
            ps.setInt(1, 100000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(1_000_000_000));
            ps.setString(2, id); ps.setString(3, username);
            ps.setString(4, PasswordHasher.hash("Test@123".toCharArray())); ps.executeUpdate();
        }
    }

    @Test void deletesDependentAccountWithEmployee() throws Exception {
        account();
        admin.delete(id);
        assertTrue(dao.find(id).isEmpty());
        assertTrue(new TaiKhoanDAO().findByUsername(username).isEmpty());
    }

    @Test void preservesEmployeeAccountAndTransactionWhenHistoryExists() throws Exception {
        account();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO GIAODICH VALUES (?, ?, NOW(), 100000, 'Đã thanh toán')")) {
            ps.setString(1, transaction); ps.setString(2, id); ps.executeUpdate();
        }
        assertEquals("Không thể xóa nhân viên vì có dữ liệu giao dịch liên quan",
                assertThrows(IllegalArgumentException.class, () -> admin.delete(id)).getMessage());
        assertTrue(dao.find(id).isPresent());
        assertTrue(new TaiKhoanDAO().findByUsername(username).isPresent());
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM GIAODICH WHERE MaGiaoDich=?")) {
            ps.setString(1, transaction);
            try (ResultSet rs = ps.executeQuery()) { assertTrue(rs.next()); }
        }
    }

    @Test void reportsMissingEmployeeForDetailUpdateDelete() throws Exception {
        admin.delete(id);
        assertThrows(IllegalArgumentException.class, () -> admin.detail(id));
        assertThrows(IllegalArgumentException.class, () -> admin.update(employee(id, "A")));
        assertThrows(IllegalArgumentException.class, () -> admin.delete(id));
    }
}
