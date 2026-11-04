package cinema.dao;

import cinema.db.Database;
import cinema.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BangGiaVeDAO {
    private static final String SELECT="""
            SELECT b.*,g.TenLoaiGhe,p.TenLoaiPhong,p.DinhDangChieu,t.TenLoaiThoiDiem
            FROM BANGGIAVE b JOIN LOAIGHE g ON g.MaLoaiGhe=b.MaLoaiGhe
            JOIN LOAIPHONG p ON p.MaLoaiPhong=b.MaLoaiPhong
            JOIN LOAITHOIDIEM t ON t.MaLoaiThoiDiem=b.MaLoaiThoiDiem
            """;

    public List<BangGiaVe> all() throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(
                SELECT+" ORDER BY p.TenLoaiPhong,g.TenLoaiGhe,t.MaLoaiThoiDiem");ResultSet rs=ps.executeQuery()) {
            List<BangGiaVe> result=new ArrayList<>();
            while(rs.next()) result.add(read(rs));
            return result;
        }
    }
    public BangGiaVe find(int id) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(SELECT+" WHERE b.MaGia=?")) {
            ps.setInt(1,id);
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy giá vé");
                return read(rs);
            }
        }
    }
    public int insert(int seat,int room,int period,BigDecimal price) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(
                "INSERT INTO BANGGIAVE (MaLoaiGhe,MaLoaiPhong,MaLoaiThoiDiem,GiaVe) VALUES (?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,seat); ps.setInt(2,room); ps.setInt(3,period); ps.setBigDecimal(4,price);
            try { ps.executeUpdate(); }
            catch(SQLException e) {
                if(e.getErrorCode()==1062) throw new IllegalArgumentException("Giá vé cho điều kiện này đã tồn tại",e);
                if(e.getErrorCode()==1452) throw new IllegalArgumentException("Loại ghế, loại phòng hoặc loại thời điểm không hợp lệ",e);
                throw e;
            }
            try(ResultSet keys=ps.getGeneratedKeys()) { keys.next(); return keys.getInt(1); }
        }
    }
    public void updatePrice(int id,BigDecimal price) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("UPDATE BANGGIAVE SET GiaVe=? WHERE MaGia=?")) {
            ps.setBigDecimal(1,price); ps.setInt(2,id);
            if(ps.executeUpdate()!=1) throw new IllegalArgumentException("Không tìm thấy giá vé");
        }
    }
    private static BangGiaVe read(ResultSet rs) throws SQLException {
        return new BangGiaVe(rs.getInt("MaGia"),new LoaiGhe(rs.getInt("MaLoaiGhe"),rs.getString("TenLoaiGhe")),
                new LoaiPhong(rs.getInt("MaLoaiPhong"),rs.getString("TenLoaiPhong"),rs.getString("DinhDangChieu")),
                new LoaiThoiDiem(rs.getInt("MaLoaiThoiDiem"),rs.getString("TenLoaiThoiDiem")),rs.getBigDecimal("GiaVe"));
    }
}
