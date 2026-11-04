package cinema.dao;

import cinema.db.Database;
import cinema.model.LoaiThoiDiem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoaiThoiDiemDAO {
    public List<LoaiThoiDiem> all() throws SQLException {
        try (Connection c=Database.connect(); PreparedStatement ps=c.prepareStatement(
                "SELECT MaLoaiThoiDiem,TenLoaiThoiDiem FROM LOAITHOIDIEM ORDER BY MaLoaiThoiDiem");
                ResultSet rs=ps.executeQuery()) {
            List<LoaiThoiDiem> result=new ArrayList<>();
            while(rs.next()) result.add(new LoaiThoiDiem(rs.getInt(1),rs.getString(2)));
            return result;
        }
    }
}
