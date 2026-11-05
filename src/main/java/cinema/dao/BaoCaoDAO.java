package cinema.dao;

import cinema.controller.revenue.KhoangThongKe;
import cinema.db.Database;
import cinema.model.SanPham;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** UC09 read models, not persisted business entities. Every report uses a read-only snapshot. */
public class BaoCaoDAO {
    public record TongHop(long soGiaoDich,long soVeBan,long soVeDaPhatHanh,BigDecimal doanhThu,long giaoDichChoVe) {
        public long soVeChoPhatHanh(){return soVeBan-soVeDaPhatHanh;}
    }
    public record TheoPhim(String maPhim,String tenPhim,long soVe,BigDecimal doanhThu) { }
    public record TheoNhanVien(String maNV,String hoTen,TongHop tongHop) { }
    public record ThongKeVe(long tongVe,long chuaSuDung,long daSuDung) { }
    private interface Row<T>{T read(ResultSet rs)throws SQLException;}

    // Pre-aggregate each one-to-many relation before summing payments: one row per paid transaction.
    private static final String HOP_LE="""
            WITH ghe_chot AS (
                SELECT MaGiaoDich,COUNT(*) SoGhe,SUM(GiaVeChot) TienChot,
                       SUM(CASE WHEN TrangThai='Đã bán' AND MaGia IS NOT NULL AND GiaVeChot>0 THEN 0 ELSE 1 END) Loi
                FROM GHE_SUATCHIEU WHERE MaGiaoDich IS NOT NULL GROUP BY MaGiaoDich
            ), ve_da_co AS (
                SELECT MaGiaoDich,COUNT(*) SoVe FROM VE GROUP BY MaGiaoDich
            ), hop_le AS (
                SELECT g.MaGiaoDich,g.MaNV,g.TongTien,t.ThoiGianThanhToan,h.SoGhe,COALESCE(v.SoVe,0) SoVe
                FROM GIAODICH g JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich
                JOIN ghe_chot h ON h.MaGiaoDich=g.MaGiaoDich LEFT JOIN ve_da_co v ON v.MaGiaoDich=g.MaGiaoDich
                WHERE g.TrangThai='Đã thanh toán' AND g.TongTien>0 AND t.SoTien=g.TongTien
                  AND h.TienChot=g.TongTien AND h.Loi=0
                  AND DATE(t.ThoiGianThanhToan) BETWEEN ? AND ?
                  AND NOT EXISTS (
                      SELECT 1 FROM VE x JOIN GHE_SUATCHIEU gs ON gs.MaSuat=x.MaSuat AND gs.MaGhe=x.MaGhe
                      WHERE x.MaGiaoDich=g.MaGiaoDich AND (x.GiaVe<>gs.GiaVeChot OR x.MaGia<>gs.MaGia)
                  )
            )
            """;
    private static final String TOTAL="""
            COUNT(*) SoGiaoDich,COALESCE(SUM(SoGhe),0) SoVeBan,COALESCE(SUM(SoVe),0) SoVeDaPhatHanh,
            COALESCE(SUM(TongTien),0) DoanhThu,COALESCE(SUM(CASE WHEN SoGhe>SoVe THEN 1 ELSE 0 END),0) ChoVe
            """;
    public TongHop theoThoiGian(KhoangThongKe range)throws SQLException {
        return query(HOP_LE+" SELECT "+TOTAL+" FROM hop_le",BaoCaoDAO::total,range.tuNgay(),range.denNgay()).get(0);
    }
    public List<TheoPhim> theoPhim(KhoangThongKe range,String film)throws SQLException {
        String sql=HOP_LE+"""
                , doanh_thu_phim AS (
                    SELECT s.MaPhim,COUNT(*) SoVe,SUM(v.GiaVe) DoanhThu
                    FROM hop_le h JOIN VE v ON v.MaGiaoDich=h.MaGiaoDich JOIN SUATCHIEU s ON s.MaSuat=v.MaSuat
                    GROUP BY s.MaPhim
                )
                SELECT p.MaPhim,p.TenPhim,COALESCE(d.SoVe,0) SoVe,COALESCE(d.DoanhThu,0) DoanhThu
                FROM PHIM p LEFT JOIN doanh_thu_phim d ON d.MaPhim=p.MaPhim
                WHERE (? IS NULL OR p.MaPhim=?) ORDER BY p.TenPhim,p.MaPhim
                """;
        return query(sql,rs->new TheoPhim(rs.getString("MaPhim"),rs.getString("TenPhim"),rs.getLong("SoVe"),rs.getBigDecimal("DoanhThu")),range.tuNgay(),range.denNgay(),film,film);
    }
    public List<TheoNhanVien> theoNhanVien(KhoangThongKe range,String employee)throws SQLException {
        String sql=HOP_LE+", nhan_vien AS (SELECT MaNV,"+TOTAL+" FROM hop_le GROUP BY MaNV) "+"""
                SELECT n.MaNV,n.HoTen,COALESCE(d.SoGiaoDich,0) SoGiaoDich,COALESCE(d.SoVeBan,0) SoVeBan,
                       COALESCE(d.SoVeDaPhatHanh,0) SoVeDaPhatHanh,COALESCE(d.DoanhThu,0) DoanhThu,COALESCE(d.ChoVe,0) ChoVe
                FROM NHANVIEN n LEFT JOIN nhan_vien d ON d.MaNV=n.MaNV
                WHERE (? IS NULL OR n.MaNV=?) ORDER BY n.MaNV
                """;
        return query(sql,rs->new TheoNhanVien(rs.getString("MaNV"),rs.getString("HoTen"),total(rs)),range.tuNgay(),range.denNgay(),employee,employee);
    }
    public ThongKeVe ve(KhoangThongKe range,String film,Integer show)throws SQLException {
        String sql="""
                SELECT COUNT(*) TongVe,COALESCE(SUM(v.TrangThai='Chưa sử dụng'),0) ChuaSuDung,
                       COALESCE(SUM(v.TrangThai='Đã sử dụng'),0) DaSuDung
                FROM VE v JOIN SUATCHIEU s ON s.MaSuat=v.MaSuat
                WHERE DATE(v.ThoiGianPhatHanh) BETWEEN ? AND ?
                  AND (? IS NULL OR s.MaPhim=?) AND (? IS NULL OR s.MaSuat=?)
                """;
        return query(sql,rs->new ThongKeVe(rs.getLong("TongVe"),rs.getLong("ChuaSuDung"),rs.getLong("DaSuDung")),range.tuNgay(),range.denNgay(),film,film,show,show).get(0);
    }
    public List<SanPham> sanPham(String product,String status,Boolean stock)throws SQLException {
        return query("""
                SELECT * FROM SANPHAM WHERE (? IS NULL OR MaSP=?) AND (? IS NULL OR TinhTrangKinhDoanh=?)
                AND (? IS NULL OR (?=TRUE AND SoLuongTon>0) OR (?=FALSE AND SoLuongTon=0)) ORDER BY MaSP
                """,rs->new SanPham(rs.getString("MaSP"),rs.getString("TenSP"),rs.getString("MoTa"),rs.getString("DonViTinh"),rs.getBigDecimal("GiaBan"),rs.getInt("SoLuongTon"),rs.getString("TinhTrangKinhDoanh")),product,product,status,status,stock,stock,stock);
    }
    private static TongHop total(ResultSet rs)throws SQLException {
        return new TongHop(rs.getLong("SoGiaoDich"),rs.getLong("SoVeBan"),rs.getLong("SoVeDaPhatHanh"),rs.getBigDecimal("DoanhThu"),rs.getLong("ChoVe"));
    }
    private static <T> List<T> query(String sql,Row<T> row,Object... args)throws SQLException {
        try(Connection c=Database.connect()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(true);c.setAutoCommit(false);
            try(PreparedStatement ps=c.prepareStatement(sql)) {
                for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);
                try(ResultSet rs=ps.executeQuery()){List<T> result=new ArrayList<>();while(rs.next())result.add(row.read(rs));c.commit();return List.copyOf(result);}
            } catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
}
