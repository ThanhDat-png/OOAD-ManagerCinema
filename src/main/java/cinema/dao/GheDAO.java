package cinema.dao;

import cinema.db.Database;
import cinema.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class GheDAO {
    private static final String SELECT="SELECT g.*,l.TenLoaiGhe FROM GHE g JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe";
    public List<Ghe> list(String room) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(SELECT+" WHERE g.MaPhong=? ORDER BY g.HangGhe,g.SoGhe")) {
            ps.setString(1,room); try(ResultSet rs=ps.executeQuery()) { List<Ghe> result=new ArrayList<>(); while(rs.next()) result.add(read(rs)); return result; }
        }
    }
    public List<LoaiGhe> types() throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("SELECT * FROM LOAIGHE ORDER BY TenLoaiGhe");ResultSet rs=ps.executeQuery()) {
            List<LoaiGhe> result=new ArrayList<>(); while(rs.next()) result.add(new LoaiGhe(rs.getInt(1),rs.getString(2))); return result;
        }
    }
    public Ghe find(int id) throws SQLException { try(Connection c=Database.connect()) { return get(c,id,false); } }

    public Ghe insert(String room,String row,int number,int type,String state) throws SQLException {
        try(Connection c=Database.connect()) {
            PhongChieuDAO.begin(c);
            try {
                PhongChieuDAO.lock(c,room); lockLiveShows(c,room); requireNoLiveSale(c,room,null);
                int id;
                try(PreparedStatement ps=c.prepareStatement("INSERT INTO GHE (MaPhong,HangGhe,SoGhe,MaLoaiGhe,TrangThai) VALUES (?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1,room); ps.setString(2,row); ps.setInt(3,number); ps.setInt(4,type); ps.setString(5,state); ps.executeUpdate();
                    try(ResultSet rs=ps.getGeneratedKeys()) { rs.next(); id=rs.getInt(1); }
                }
                execute(c,"INSERT INTO GHE_SUATCHIEU (MaSuat,MaGhe,TrangThai) SELECT MaSuat,?,? FROM SUATCHIEU WHERE MaPhong=? AND TrangThai<>'Đã hủy' AND ThoiGianTraPhong>NOW()",
                        id,"Sẵn sàng sử dụng".equals(state)?"Trống":"Không khả dụng",room);
                Ghe added=get(c,id,false); c.commit(); return added;
            } catch(SQLException|RuntimeException e) { PhongChieuDAO.rollback(c,e); duplicate(e); throw e; }
        }
    }
    public Ghe updateInfo(int id,String row,int number,String state) throws SQLException {
        return modify(id,old -> new Ghe(id,old.maPhong(),old.loaiGhe(),row,number,state));
    }
    public Ghe classify(int id,int type) throws SQLException {
        return modify(id,old -> new Ghe(id,old.maPhong(),new LoaiGhe(type,""),old.hangGhe(),old.soGhe(),old.trangThai()));
    }

    private Ghe modify(int id,UnaryOperator<Ghe> change) throws SQLException {
        try(Connection c=Database.connect()) {
            PhongChieuDAO.begin(c);
            try {
                Ghe initial=get(c,id,false); PhongChieuDAO.lock(c,initial.maPhong()); lockLiveShows(c,initial.maPhong());
                Ghe old=get(c,id,true),candidate=change.apply(old);
                boolean structure=!old.hangGhe().equals(candidate.hangGhe()) || old.soGhe()!=candidate.soGhe()
                        || old.loaiGhe().maLoaiGhe()!=candidate.loaiGhe().maLoaiGhe();
                boolean status=!old.trangThai().equals(candidate.trangThai());
                if(!structure && !status) { c.commit(); return old; }
                requireNoLiveSale(c,old.maPhong(),structure?null:id);
                releaseHolds(c,id);
                try(PreparedStatement ps=c.prepareStatement("UPDATE GHE SET HangGhe=?,SoGhe=?,MaLoaiGhe=?,TrangThai=? WHERE MaGhe=?")) {
                    ps.setString(1,candidate.hangGhe()); ps.setInt(2,candidate.soGhe()); ps.setInt(3,candidate.loaiGhe().maLoaiGhe());
                    ps.setString(4,candidate.trangThai()); ps.setInt(5,id); ps.executeUpdate();
                }
                execute(c,"UPDATE GHE_SUATCHIEU gs JOIN SUATCHIEU s ON s.MaSuat=gs.MaSuat SET gs.TrangThai=?,gs.MaGia=NULL,gs.GiaVeChot=NULL "
                        + "WHERE gs.MaGhe=? AND s.TrangThai<>'Đã hủy' AND s.ThoiGianTraPhong>NOW() AND gs.TrangThai<>'Đã bán' AND gs.MaGiaoDich IS NULL",
                        "Sẵn sàng sử dụng".equals(candidate.trangThai())?"Trống":"Không khả dụng",id);
                Ghe updated=get(c,id,false); c.commit(); return updated;
            } catch(SQLException|RuntimeException e) { PhongChieuDAO.rollback(c,e); duplicate(e); throw e; }
        }
    }

    private static void releaseHolds(Connection c,int seat) throws SQLException {
        List<String> owners=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement("SELECT DISTINCT gs.MaGiaoDich FROM GHE_SUATCHIEU gs JOIN SUATCHIEU s ON s.MaSuat=gs.MaSuat "
                + "WHERE gs.MaGhe=? AND gs.MaGiaoDich IS NOT NULL AND s.TrangThai<>'Đã hủy' AND s.ThoiGianTraPhong>NOW() ORDER BY gs.MaGiaoDich")) {
            ps.setInt(1,seat); try(ResultSet rs=ps.executeQuery()) { while(rs.next()) owners.add(rs.getString(1)); }
        }
        GiuGheDAO.releaseOwners(c,owners);
    }
    private static void lockLiveShows(Connection c,String room) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT MaSuat FROM SUATCHIEU WHERE MaPhong=? AND TrangThai<>'Đã hủy' AND ThoiGianTraPhong>NOW() ORDER BY MaSuat FOR UPDATE")) {
            ps.setString(1,room); try(ResultSet rs=ps.executeQuery()) { while(rs.next()) { rs.getInt(1); } }
        }
    }
    private static void requireNoLiveSale(Connection c,String room,Integer seat) throws SQLException {
        if(GiuGheDAO.hasUnresolvedSales(c,room,seat)) throw new IllegalArgumentException("Có giao dịch đang thanh toán hoặc chờ phát hành vé, chưa được thay đổi ghế");
        String sql="""
                SELECT 1 FROM SUATCHIEU s WHERE s.MaPhong=? AND s.TrangThai<>'Đã hủy' AND s.ThoiGianTraPhong>NOW()
                AND (EXISTS (SELECT 1 FROM VE v WHERE v.MaSuat=s.MaSuat AND (? IS NULL OR v.MaGhe=?))
                OR EXISTS (SELECT 1 FROM GHE_SUATCHIEU gs LEFT JOIN GIAODICH g ON g.MaGiaoDich=gs.MaGiaoDich
                LEFT JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich
                WHERE gs.MaSuat=s.MaSuat AND (? IS NULL OR gs.MaGhe=?)
                AND (gs.TrangThai='Đã bán' OR g.TrangThai IN ('Đang thanh toán','Đã thanh toán') OR t.MaThanhToan IS NOT NULL))) LIMIT 1
                """;
        try(PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1,room); for(int i=2;i<=5;i++) ps.setObject(i,seat);
            try(ResultSet rs=ps.executeQuery()) { if(rs.next()) throw new IllegalArgumentException(seat==null
                    ?"Phòng có suất chiếu đã bán vé hoặc đang thanh toán, không được thay đổi kết cấu sơ đồ ghế"
                    :"Ghế đang có vé hoặc giao dịch thanh toán liên quan, không được thay đổi trạng thái"); }
        }
    }
    private static Ghe get(Connection c,int id,boolean lock) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(SELECT+" WHERE g.MaGhe=?"+(lock?" FOR UPDATE":""))) {
            ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy ghế"); return read(rs); }
        }
    }
    private static Ghe read(ResultSet rs) throws SQLException {
        return new Ghe(rs.getInt("MaGhe"),rs.getString("MaPhong"),new LoaiGhe(rs.getInt("MaLoaiGhe"),rs.getString("TenLoaiGhe")),
                rs.getString("HangGhe"),rs.getInt("SoGhe"),rs.getString("TrangThai"));
    }
    private static void duplicate(Exception e) {
        if(e instanceof SQLException sql && sql.getErrorCode()==1062) throw new IllegalArgumentException("Vị trí ghế đã tồn tại trong phòng",e);
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); }
    }
}
