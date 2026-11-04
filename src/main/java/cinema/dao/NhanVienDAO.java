package cinema.dao;

import cinema.db.Database;
import cinema.model.NhanVien;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NhanVienDAO {
    private static final String COLUMNS = "MaNV, HoTen, NgaySinh, SoDienThoai, Email, DiaChi, ChucVu";

    public List<NhanVien> search(String keyword) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM NHANVIEN WHERE MaNV LIKE ? ESCAPE '='"
                + " OR HoTen LIKE ? ESCAPE '=' OR SoDienThoai LIKE ? ESCAPE '=' ORDER BY MaNV";
        String pattern = "%" + keyword.replace("=", "==").replace("%", "=%").replace("_", "=_") + "%";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) ps.setString(i, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<NhanVien> result = new ArrayList<>();
                while (rs.next()) result.add(read(rs));
                return result;
            }
        }
    }

    public Optional<NhanVien> find(String id) throws SQLException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM NHANVIEN WHERE MaNV = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    public void insert(NhanVien employee) throws SQLException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO NHANVIEN (HoTen, NgaySinh, SoDienThoai, Email, DiaChi, ChucVu, MaNV) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            bind(ps, employee);
            ps.executeUpdate();
        }
    }

    public void update(NhanVien employee) throws SQLException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "UPDATE NHANVIEN SET HoTen=?, NgaySinh=?, SoDienThoai=?, Email=?, DiaChi=?, ChucVu=? WHERE MaNV=?")) {
            bind(ps, employee);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên");
        }
    }

    public void delete(String id) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement("SELECT MaNV FROM NHANVIEN WHERE MaNV=? FOR UPDATE")) {
                    lock.setString(1, id);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên");
                    }
                }
                try (PreparedStatement check = c.prepareStatement("SELECT 1 FROM GIAODICH WHERE MaNV=? LIMIT 1")) {
                    check.setString(1, id);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) throw new IllegalArgumentException("Không thể xóa nhân viên vì có dữ liệu giao dịch liên quan");
                    }
                }
                try (PreparedStatement account = c.prepareStatement("DELETE FROM TAIKHOAN WHERE MaNV=?");
                        PreparedStatement employee = c.prepareStatement("DELETE FROM NHANVIEN WHERE MaNV=?")) {
                    account.setString(1, id);
                    account.executeUpdate();
                    employee.setString(1, id);
                    employee.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    private static NhanVien read(ResultSet rs) throws SQLException {
        Date birth = rs.getDate("NgaySinh");
        return new NhanVien(rs.getString("MaNV"), rs.getString("HoTen"),
                birth == null ? null : birth.toLocalDate(), rs.getString("SoDienThoai"),
                rs.getString("Email"), rs.getString("DiaChi"), rs.getString("ChucVu"));
    }

    private static void bind(PreparedStatement ps, NhanVien e) throws SQLException {
        ps.setString(1, e.hoTen());
        ps.setDate(2, e.ngaySinh() == null ? null : Date.valueOf(e.ngaySinh()));
        ps.setString(3, e.soDienThoai());
        ps.setString(4, e.email());
        ps.setString(5, e.diaChi());
        ps.setString(6, e.chucVu());
        ps.setString(7, e.maNV());
    }
}
