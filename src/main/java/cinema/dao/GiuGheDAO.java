package cinema.dao;
import java.sql.*;
import java.util.List;

final class GiuGheDAO {
    private GiuGheDAO() { }
    static void releaseOwners(Connection c,List<String> owners) throws SQLException {
        for(String owner:owners) {
            String state;
            try(PreparedStatement ps=c.prepareStatement("SELECT TrangThai FROM GIAODICH WHERE MaGiaoDich=? FOR UPDATE")) {
                ps.setString(1,owner); try(ResultSet rs=ps.executeQuery()) { rs.next(); state=rs.getString(1); }
            }
            if(!List.of("Chờ thanh toán","Đã hủy","Hết hạn").contains(state)) throw new IllegalArgumentException("Có giao dịch đang xử lý hoặc đã thanh toán. Vui lòng kiểm tra giao dịch trước");
            try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM THANHTOAN WHERE MaGiaoDich=? LIMIT 1")) {
                ps.setString(1,owner); try(ResultSet rs=ps.executeQuery()) { if(rs.next()) throw new IllegalArgumentException("Có giao dịch đã thanh toán, không được thay đổi hoặc hủy"); }
            }
            try(PreparedStatement ps=c.prepareStatement("SELECT TrangThai FROM GHE_SUATCHIEU WHERE MaGiaoDich=? FOR UPDATE")) {
                ps.setString(1,owner); try(ResultSet rs=ps.executeQuery()) { while(rs.next()) if("Đã bán".equals(rs.getString(1)))
                    throw new IllegalArgumentException("Giao dịch có ghế đã bán, không được thay đổi hoặc hủy"); }
            }
            execute(c,"UPDATE GHE_SUATCHIEU SET TrangThai=CASE WHEN TrangThai='Đang giữ' THEN 'Trống' ELSE TrangThai END,MaGiaoDich=NULL,ThoiGianHetHanGiu=NULL,MaGia=NULL,GiaVeChot=NULL WHERE MaGiaoDich=?",owner);
            execute(c,"UPDATE GIAODICH SET TrangThai='Đã hủy' WHERE MaGiaoDich=? AND TrangThai='Chờ thanh toán'",owner);
        }
    }
    static boolean hasUnresolvedSales(Connection c,String room,Integer seat) throws SQLException {
        String sql="SELECT 1 FROM GHE_SUATCHIEU gs JOIN SUATCHIEU s ON s.MaSuat=gs.MaSuat JOIN GIAODICH g ON g.MaGiaoDich=gs.MaGiaoDich "
                +"LEFT JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich WHERE s.MaPhong=? AND (? IS NULL OR gs.MaGhe=?) AND "
                +"(g.TrangThai='Đang thanh toán' OR (g.TrangThai='Đã thanh toán' AND NOT EXISTS(SELECT 1 FROM VE v WHERE v.MaSuat=gs.MaSuat AND v.MaGhe=gs.MaGhe)) "
                +"OR (t.MaThanhToan IS NOT NULL AND g.TrangThai<>'Đã thanh toán')) LIMIT 1";
        try(PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1,room); ps.setObject(2,seat); ps.setObject(3,seat);
            try(ResultSet rs=ps.executeQuery()) { return rs.next(); }
        }
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); }
    }
}
