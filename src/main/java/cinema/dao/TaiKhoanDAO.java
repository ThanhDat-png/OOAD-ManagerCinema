package cinema.dao;

import cinema.db.Database;
import cinema.model.NhanVien;
import cinema.model.TaiKhoan;
import cinema.model.VaiTro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class TaiKhoanDAO {
    public Optional<TaiKhoan> findByUsername(String username) throws SQLException {
        String sql = """
                SELECT t.MaTK, t.TenDangNhap, t.MatKhauHash, t.TrangThai,
                       v.TenVaiTro, n.MaNV, n.HoTen, n.NgaySinh,
                       n.SoDienThoai, n.Email, n.DiaChi, n.ChucVu
                FROM TAIKHOAN t
                JOIN VAITRO v ON t.MaVaiTro = v.MaVaiTro
                LEFT JOIN NHANVIEN n ON t.MaNV = n.MaNV
                WHERE t.TenDangNhap = ?
                """;
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                java.sql.Date birthDate = rs.getDate("NgaySinh");
                NhanVien employee = rs.getString("MaNV") == null ? null : new NhanVien(
                        rs.getString("MaNV"), rs.getString("HoTen"),
                        birthDate == null ? null : birthDate.toLocalDate(),
                        rs.getString("SoDienThoai"), rs.getString("Email"),
                        rs.getString("DiaChi"), rs.getString("ChucVu"));
                return Optional.of(new TaiKhoan(rs.getInt("MaTK"),
                        rs.getString("TenDangNhap"), rs.getString("MatKhauHash"),
                        rs.getString("TrangThai"), VaiTro.fromDatabase(rs.getString("TenVaiTro")), employee));
            }
        }
    }
}
