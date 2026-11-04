package cinema.dao;

import cinema.db.Database;
import cinema.model.Ve;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;

public class VeDAO {
    public enum LoaiMa { MA_VE, MA_QR }
    public record ThongTin(Ve ve, String tenPhim, String maPhong, String tenPhong,
            LocalDateTime batDau, String trangThaiSuat, String trangThaiPhong,
            boolean daThanhToan, boolean gheDaBan, LocalDateTime thoiDiemKiemTra) { }

    private static final String SELECT="""
            SELECT v.*,p.TenPhim,s.MaPhong,r.TenPhong,s.ThoiGianBatDau,
                   s.TrangThai AS TrangThaiSuat,r.TrangThai AS TrangThaiPhong,NOW() AS LucKiemTra,
                   (g.TrangThai='Đã thanh toán' AND t.MaThanhToan IS NOT NULL
                    AND t.SoTien=g.TongTien AND g.TongTien>0
                    AND g.TongTien=(SELECT SUM(h.GiaVeChot) FROM GHE_SUATCHIEU h WHERE h.MaGiaoDich=g.MaGiaoDich)
                    AND NOT EXISTS (SELECT 1 FROM GHE_SUATCHIEU h WHERE h.MaGiaoDich=g.MaGiaoDich
                        AND (h.TrangThai<>'Đã bán' OR h.GiaVeChot IS NULL OR h.MaGia IS NULL))) AS DaThanhToan,
                   (gs.TrangThai='Đã bán' AND gs.MaGiaoDich=v.MaGiaoDich
                    AND gs.GiaVeChot=v.GiaVe AND gs.MaGia=v.MaGia) AS GheDaBan
            FROM VE v JOIN SUATCHIEU s ON s.MaSuat=v.MaSuat JOIN PHIM p ON p.MaPhim=s.MaPhim
            JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong JOIN GIAODICH g ON g.MaGiaoDich=v.MaGiaoDich
            LEFT JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich
            JOIN GHE_SUATCHIEU gs ON gs.MaSuat=v.MaSuat AND gs.MaGhe=v.MaGhe
            """;

    public Optional<ThongTin> lookup(String code,LoaiMa type) throws SQLException {
        try(Connection c=Database.connect()) {
            return lookup(c,code,type);
        }
    }

    public ThongTin confirm(String ticket,int beforeMinutes,int afterMinutes,Consumer<ThongTin> validate) throws SQLException {
        try(Connection c=Database.connect()) {
            PhongChieuDAO.begin(c);
            try {
                ThongTin initial=lookup(c,ticket,LoaiMa.MA_VE).orElseThrow(() -> new IllegalArgumentException("Vé không tồn tại"));
                PhongChieuDAO.lock(c,initial.maPhong());
                lock(c,"SELECT MaSuat FROM SUATCHIEU WHERE MaSuat=? FOR UPDATE",initial.ve().maSuat());
                lock(c,"SELECT MaGhe FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGhe=? FOR UPDATE",initial.ve().maSuat(),initial.ve().maGhe());
                lock(c,"SELECT MaGiaoDich FROM GIAODICH WHERE MaGiaoDich=? FOR UPDATE",initial.ve().maGiaoDich());
                lock(c,"SELECT MaThanhToan FROM THANHTOAN WHERE MaGiaoDich=? FOR SHARE",initial.ve().maGiaoDich());
                lock(c,"SELECT MaVe FROM VE WHERE MaVe=? FOR UPDATE",ticket);
                ThongTin current=lookup(c,ticket,LoaiMa.MA_VE).orElseThrow(() -> new IllegalArgumentException("Vé không tồn tại"));
                if(current.ve().maSuat()!=initial.ve().maSuat() || !current.maPhong().equals(initial.maPhong())
                        || !current.ve().maGiaoDich().equals(initial.ve().maGiaoDich()))
                    throw new IllegalArgumentException("Thông tin vé đã thay đổi. Vui lòng tra cứu lại");
                validate.accept(current);
                try(PreparedStatement ps=c.prepareStatement("""
                        UPDATE VE v JOIN SUATCHIEU s ON s.MaSuat=v.MaSuat SET v.TrangThai='Đã sử dụng'
                        WHERE v.MaVe=? AND v.TrangThai='Chưa sử dụng' AND s.TrangThai<>'Đã hủy'
                        AND NOW()>=TIMESTAMPADD(MINUTE,?,s.ThoiGianBatDau)
                        AND NOW()<=TIMESTAMPADD(MINUTE,?,s.ThoiGianBatDau)
                        """)) {
                    ps.setString(1,ticket); ps.setInt(2,-beforeMinutes); ps.setInt(3,afterMinutes);
                    if(ps.executeUpdate()!=1) throw new IllegalArgumentException("Vé không còn đủ điều kiện xác nhận. Vui lòng kiểm tra lại");
                }
                ThongTin updated=lookup(c,ticket,LoaiMa.MA_VE).orElseThrow();
                c.commit(); return updated;
            } catch(SQLException|RuntimeException e) { PhongChieuDAO.rollback(c,e); throw e; }
        }
    }

    private static Optional<ThongTin> lookup(Connection c,String code,LoaiMa type) throws SQLException {
        String condition=type==LoaiMa.MA_QR?" WHERE BINARY v.MaQR=BINARY ?":" WHERE v.MaVe=?";
        try(PreparedStatement ps=c.prepareStatement(SELECT+condition)) {
            ps.setString(1,code);
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) return Optional.empty();
                return Optional.of(read(rs));
            }
        }
    }
    static java.util.List<ThongTin> forTransaction(Connection c,String id) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(SELECT+" WHERE v.MaGiaoDich=? ORDER BY v.MaSuat,v.MaGhe")) {
            ps.setString(1,id); try(ResultSet rs=ps.executeQuery()) {
                java.util.List<ThongTin> result=new java.util.ArrayList<>(); while(rs.next()) result.add(read(rs)); return result;
            }
        }
    }
    private static ThongTin read(ResultSet rs) throws SQLException {
        Ve ticket=new Ve(rs.getString("MaVe"),rs.getString("MaGiaoDich"),rs.getInt("MaSuat"),rs.getInt("MaGhe"),rs.getInt("MaGia"),
                        rs.getString("MaQR"),rs.getBigDecimal("GiaVe"),rs.getString("TrangThai"),rs.getTimestamp("ThoiGianPhatHanh").toLocalDateTime(),
                        rs.getString("HangGheLucPhatHanh"),rs.getInt("SoGheLucPhatHanh"),rs.getString("TenLoaiGheLucPhatHanh"));
        return new ThongTin(ticket,rs.getString("TenPhim"),rs.getString("MaPhong"),rs.getString("TenPhong"),
                        rs.getTimestamp("ThoiGianBatDau").toLocalDateTime(),rs.getString("TrangThaiSuat"),rs.getString("TrangThaiPhong"),
                        rs.getBoolean("DaThanhToan"),rs.getBoolean("GheDaBan"),rs.getTimestamp("LucKiemTra").toLocalDateTime());
    }
    private static void lock(Connection c,String sql,Object... values) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) {
            for(int i=0;i<values.length;i++) ps.setObject(i+1,values[i]);
            try(ResultSet rs=ps.executeQuery()) { while(rs.next()) { rs.getObject(1); } }
        }
    }
}
