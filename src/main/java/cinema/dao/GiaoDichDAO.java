package cinema.dao;

import cinema.db.Database;
import cinema.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GiaoDichDAO {
    public record Dong(GiaoDich giaoDich,String hoTenNhanVien,ThanhToan thanhToan) { }
    public List<Dong> search(String code,LocalDate from,LocalDate to) throws SQLException {
        String withCustomer="""
                SELECT g.*,n.HoTen,t.MaThanhToan,t.PhuongThuc,t.SoTien,t.ThoiGianThanhToan
                FROM GIAODICH g JOIN NHANVIEN n ON n.MaNV=g.MaNV
                LEFT JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich
                WHERE (? IS NULL OR g.MaGiaoDich=? OR EXISTS(SELECT 1 FROM VE v WHERE v.MaGiaoDich=g.MaGiaoDich AND v.MaVe=?)
                       OR EXISTS(SELECT 1 FROM CHONGHE_KHACH w WHERE w.MaGiaoDich=g.MaGiaoDich AND w.MaChonGhe=?))
                AND (? IS NULL OR DATE(g.ThoiGianTao)>=?) AND (? IS NULL OR DATE(g.ThoiGianTao)<=?)
                ORDER BY g.ThoiGianTao DESC,g.MaGiaoDich
                """;
        String legacy="""
                SELECT g.*,n.HoTen,t.MaThanhToan,t.PhuongThuc,t.SoTien,t.ThoiGianThanhToan
                FROM GIAODICH g JOIN NHANVIEN n ON n.MaNV=g.MaNV
                LEFT JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich
                WHERE (? IS NULL OR g.MaGiaoDich=? OR EXISTS(SELECT 1 FROM VE v WHERE v.MaGiaoDich=g.MaGiaoDich AND v.MaVe=?))
                AND (? IS NULL OR DATE(g.ThoiGianTao)>=?) AND (? IS NULL OR DATE(g.ThoiGianTao)<=?)
                ORDER BY g.ThoiGianTao DESC,g.MaGiaoDich
                """;
        try(Connection c=Database.connect()) {
            try { return runSearch(c,withCustomer,code,from,to,true); }
            catch(SQLException e) {
                if(e.getErrorCode()==1146) return runSearch(c,legacy,code,from,to,false);
                throw e;
            }
        }
    }
    private static List<Dong> runSearch(Connection c,String sql,String code,LocalDate from,LocalDate to,boolean includeCustomer) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) {
            int i=1;
            ps.setString(i++,code); ps.setString(i++,code); ps.setString(i++,code);
            if(includeCustomer) ps.setString(i++,code);
            ps.setObject(i++,from); ps.setObject(i++,from); ps.setObject(i++,to); ps.setObject(i,to);
            try(ResultSet rs=ps.executeQuery()) {
                List<Dong> result=new ArrayList<>();
                while(rs.next()) result.add(new Dong(read(rs),rs.getString("HoTen"),payment(rs)));
                return result;
            }
        }
    }
    static GiaoDich get(Connection c,String id,boolean lock) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT * FROM GIAODICH WHERE MaGiaoDich=?"+(lock?" FOR UPDATE":""))) {
            ps.setString(1,id); try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy giao dịch"); return read(rs);
            }
        }
    }
    static ThanhToan payment(Connection c,String id) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT * FROM THANHTOAN WHERE MaGiaoDich=?")) {
            ps.setString(1,id); try(ResultSet rs=ps.executeQuery()) { return rs.next()?payment(rs):null; }
        }
    }
    private static GiaoDich read(ResultSet rs) throws SQLException {
        return new GiaoDich(rs.getString("MaGiaoDich"),rs.getString("MaNV"),rs.getTimestamp("ThoiGianTao").toLocalDateTime(),rs.getBigDecimal("TongTien"),rs.getString("TrangThai"));
    }
    private static ThanhToan payment(ResultSet rs) throws SQLException {
        if(rs.getObject("MaThanhToan")==null) return null;
        return new ThanhToan(rs.getInt("MaThanhToan"),rs.getString("MaGiaoDich"),rs.getString("PhuongThuc"),rs.getBigDecimal("SoTien"),rs.getTimestamp("ThoiGianThanhToan").toLocalDateTime());
    }
}
