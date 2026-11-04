package cinema.dao;

import cinema.db.Database;
import cinema.model.TheLoai;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TheLoaiDAO {
    public List<TheLoai> all() throws SQLException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT MaTheLoai, TenTheLoai FROM THELOAI ORDER BY TenTheLoai"); ResultSet rs = ps.executeQuery()) {
            List<TheLoai> result = new ArrayList<>();
            while (rs.next()) result.add(new TheLoai(rs.getInt(1), rs.getString(2)));
            return result;
        }
    }
}
