package cinema.dao;

import cinema.db.Database;
import cinema.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PhongChieuDAO {
    private static final String SELECT="SELECT p.*,l.TenLoaiPhong,l.DinhDangChieu FROM PHONGCHIEU p JOIN LOAIPHONG l ON l.MaLoaiPhong=p.MaLoaiPhong";
    public List<PhongChieu> all() throws SQLException { return search("",null,null); }

    public List<PhongChieu> search(String keyword,Integer type,String state) throws SQLException {
        String pattern="%"+keyword.replace("=","==").replace("%","=%").replace("_","=_")+"%";
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(SELECT+
                " WHERE (p.MaPhong LIKE ? ESCAPE '=' OR p.TenPhong LIKE ? ESCAPE '=') AND (? IS NULL OR p.MaLoaiPhong=?) AND (? IS NULL OR p.TrangThai=?) ORDER BY p.TenPhong")) {
            ps.setString(1,pattern); ps.setString(2,pattern); ps.setObject(3,type); ps.setObject(4,type); ps.setString(5,state); ps.setString(6,state);
            try(ResultSet rs=ps.executeQuery()) { List<PhongChieu> result=new ArrayList<>(); while(rs.next()) result.add(read(rs)); return result; }
        }
    }
    public List<LoaiPhong> types() throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("SELECT * FROM LOAIPHONG ORDER BY TenLoaiPhong");ResultSet rs=ps.executeQuery()) {
            List<LoaiPhong> result=new ArrayList<>(); while(rs.next()) result.add(new LoaiPhong(rs.getInt(1),rs.getString(2),rs.getString(3))); return result;
        }
    }
    public PhongChieu find(String id) throws SQLException { try(Connection c=Database.connect()) { return find(c,id); } }
    static PhongChieu find(Connection c,String id) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(SELECT+" WHERE p.MaPhong=?")) {
            ps.setString(1,id); try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy phòng chiếu"); return read(rs);
            }
        }
    }
    public void insert(PhongChieu room) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO PHONGCHIEU VALUES (?,?,?,?,?)")) {
            ps.setString(1,room.maPhong()); ps.setInt(2,room.loaiPhong().maLoaiPhong()); ps.setString(3,room.tenPhong());
            ps.setInt(4,room.thoiGianDonPhong()); ps.setString(5,room.trangThai());
            try { ps.executeUpdate(); } catch(SQLException e) { duplicateName(e); throw e; }
        }
    }
    public void updateInfo(String id,String name,int type,int cleaning) throws SQLException {
        try(Connection c=Database.connect()) {
            begin(c);
            try {
                lock(c,id); PhongChieu old=find(c,id);
                if((old.loaiPhong().maLoaiPhong()!=type || old.thoiGianDonPhong()!=cleaning) && hasLiveShow(c,id))
                    throw new IllegalArgumentException("Phòng còn suất chiếu chưa kết thúc, không được đổi loại phòng hoặc thời gian dọn phòng");
                try(PreparedStatement ps=c.prepareStatement("UPDATE PHONGCHIEU SET TenPhong=?,MaLoaiPhong=?,ThoiGianDonPhong=? WHERE MaPhong=?")) {
                    ps.setString(1,name); ps.setInt(2,type); ps.setInt(3,cleaning); ps.setString(4,id); ps.executeUpdate();
                }
                c.commit();
            } catch(SQLException|RuntimeException e) { rollback(c,e); if(e instanceof SQLException sql) duplicateName(sql); throw e; }
        }
    }
    public void updateState(String id,String state) throws SQLException {
        try(Connection c=Database.connect()) {
            begin(c);
            try {
                lock(c,id);
                if(!"Đang hoạt động".equals(state) && hasLiveShow(c,id))
                    throw new IllegalArgumentException("Phòng còn suất chiếu chưa kết thúc. Vui lòng xử lý các suất chiếu liên quan trước");
                if("Đang hoạt động".equals(state)) {
                    try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM GHE WHERE MaPhong=? AND TrangThai='Sẵn sàng sử dụng' LIMIT 1")) {
                        ps.setString(1,id); try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Phòng chưa có ghế sử dụng được"); }
                    }
                }
                execute(c,"UPDATE PHONGCHIEU SET TrangThai=? WHERE MaPhong=?",state,id); c.commit();
            } catch(SQLException|RuntimeException e) { rollback(c,e); throw e; }
        }
    }
    static void lock(Connection c,String id) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT MaPhong FROM PHONGCHIEU WHERE MaPhong=? FOR UPDATE")) {
            ps.setString(1,id); try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy phòng chiếu"); }
        }
    }
    private static boolean hasLiveShow(Connection c,String id) throws SQLException {
        if(GiuGheDAO.hasUnresolvedSales(c,id,null)) return true;
        try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM SUATCHIEU WHERE MaPhong=? AND TrangThai<>'Đã hủy' AND ThoiGianTraPhong>NOW() LIMIT 1")) {
            ps.setString(1,id); try(ResultSet rs=ps.executeQuery()) { return rs.next(); }
        }
    }
    static void begin(Connection c) throws SQLException { c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); c.setAutoCommit(false); }
    static void rollback(Connection c,Exception e) { try { c.rollback(); } catch(SQLException failure) { e.addSuppressed(failure); } }
    private static void duplicateName(SQLException e) { if(e.getErrorCode()==1062) throw new IllegalArgumentException("Tên phòng chiếu đã tồn tại",e); }
    private static PhongChieu read(ResultSet rs) throws SQLException {
        return new PhongChieu(rs.getString("MaPhong"),new LoaiPhong(rs.getInt("MaLoaiPhong"),rs.getString("TenLoaiPhong"),rs.getString("DinhDangChieu")),
                rs.getString("TenPhong"),rs.getInt("ThoiGianDonPhong"),rs.getString("TrangThai"));
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); }
    }
}
