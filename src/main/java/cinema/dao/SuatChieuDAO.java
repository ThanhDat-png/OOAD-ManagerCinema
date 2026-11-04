package cinema.dao;

import cinema.db.Database;
import cinema.model.SuatChieu;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.UnaryOperator;

public class SuatChieuDAO {
    public List<SuatChieu> list(LocalDate date, String room) throws SQLException {
        try(Connection c=Database.connect(); PreparedStatement ps=c.prepareStatement(
                "SELECT * FROM SUATCHIEU WHERE (? IS NULL OR DATE(ThoiGianBatDau)=?) AND (? IS NULL OR MaPhong=?) ORDER BY ThoiGianBatDau, MaPhong")) {
            ps.setObject(1,date); ps.setObject(2,date); ps.setString(3,room); ps.setString(4,room);
            try(ResultSet rs=ps.executeQuery()) {
                List<SuatChieu> result=new ArrayList<>(); while(rs.next()) result.add(read(rs)); return result;
            }
        }
    }

    public SuatChieu find(int id) throws SQLException {
        try(Connection c=Database.connect()) { return get(c,id,false); }
    }

    public SuatChieu editable(int id) throws SQLException {
        try(Connection c=Database.connect()) {
            begin(c);
            try {
                SuatChieu initial=get(c,id,false); lockRooms(c,initial.maPhong(),initial.maPhong());
                SuatChieu show=get(c,id,true); requireRoomUnchanged(initial,show);
                requireEditable(c,show); c.commit(); return show;
            } catch(SQLException|RuntimeException e) { rollback(c,e); throw e; }
        }
    }

    public SuatChieu create(String film, String room, LocalDateTime start, Set<LocalDate> holidays) throws SQLException {
        try(Connection c=Database.connect()) {
            begin(c);
            try {
                lockRooms(c,room,room);
                SuatChieu candidate=validate(c,new SuatChieu(0,film,room,0,start,start,"Sắp chiếu"),holidays);
                checkConflict(c,candidate);
                int id;
                try(PreparedStatement ps=c.prepareStatement(
                        "INSERT INTO SUATCHIEU (MaPhim,MaPhong,MaLoaiThoiDiem,ThoiGianBatDau,ThoiGianTraPhong,TrangThai) VALUES (?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps,candidate); ps.executeUpdate();
                    try(ResultSet keys=ps.getGeneratedKeys()) { keys.next(); id=keys.getInt(1); }
                }
                initializeSeats(c,id,room);
                c.commit(); return new SuatChieu(id,candidate.maPhim(),candidate.maPhong(),candidate.maLoaiThoiDiem(),
                        candidate.thoiGianBatDau(),candidate.thoiGianTraPhong(),candidate.trangThai());
            } catch(SQLException|RuntimeException e) { rollback(c,e); throw e; }
        }
    }

    public SuatChieu modify(int id, UnaryOperator<SuatChieu> change, Set<LocalDate> holidays) throws SQLException {
        try(Connection c=Database.connect()) {
            begin(c);
            try {
                SuatChieu initial=get(c,id,false);
                SuatChieu preview=change.apply(initial); lockRooms(c,initial.maPhong(),preview.maPhong());
                SuatChieu old=get(c,id,true); requireRoomUnchanged(initial,old);
                requireEditable(c,old);
                SuatChieu proposed=change.apply(old);
                if(proposed.equals(old)) { c.commit(); return old; }
                SuatChieu candidate=validate(c,proposed,holidays);
                checkConflict(c,candidate);
                releaseUnpaidHolds(c,id);
                boolean moved=!old.maPhong().equals(candidate.maPhong());
                if(moved) execute(c,"DELETE FROM GHE_SUATCHIEU WHERE MaSuat=?",id);
                try(PreparedStatement ps=c.prepareStatement(
                        "UPDATE SUATCHIEU SET MaPhim=?,MaPhong=?,MaLoaiThoiDiem=?,ThoiGianBatDau=?,ThoiGianTraPhong=?,TrangThai=? WHERE MaSuat=?")) {
                    bind(ps,candidate); ps.setInt(7,id); ps.executeUpdate();
                }
                if(moved) initializeSeats(c,id,candidate.maPhong());
                c.commit(); return candidate;
            } catch(SQLException|RuntimeException e) { rollback(c,e); throw e; }
        }
    }

    public void cancel(int id) throws SQLException {
        try(Connection c=Database.connect()) {
            begin(c);
            try {
                SuatChieu initial=get(c,id,false); lockRooms(c,initial.maPhong(),initial.maPhong());
                SuatChieu old=get(c,id,true); requireRoomUnchanged(initial,old); requireEditable(c,old);
                releaseUnpaidHolds(c,id);
                execute(c,"UPDATE SUATCHIEU SET TrangThai='Đã hủy' WHERE MaSuat=?",id);
                execute(c,"UPDATE GHE_SUATCHIEU SET TrangThai='Không khả dụng',MaGia=NULL,GiaVeChot=NULL WHERE MaSuat=?",id);
                c.commit();
            } catch(SQLException|RuntimeException e) { rollback(c,e); throw e; }
        }
    }

    private static void requireRoomUnchanged(SuatChieu initial,SuatChieu current) {
        if(!initial.maPhong().equals(current.maPhong()))
            throw new IllegalArgumentException("Suất chiếu đã được chuyển phòng bởi thao tác khác. Vui lòng tải lại");
    }

    private static void begin(Connection c) throws SQLException {
        c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); c.setAutoCommit(false);
    }

    private static SuatChieu get(Connection c,int id,boolean lock) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT * FROM SUATCHIEU WHERE MaSuat=?"+(lock?" FOR UPDATE":""))) {
            ps.setInt(1,id);
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy suất chiếu"); return read(rs);
            }
        }
    }

    private static SuatChieu read(ResultSet rs) throws SQLException {
        return new SuatChieu(rs.getInt("MaSuat"),rs.getString("MaPhim"),rs.getString("MaPhong"),rs.getInt("MaLoaiThoiDiem"),
                rs.getTimestamp("ThoiGianBatDau").toLocalDateTime(),rs.getTimestamp("ThoiGianTraPhong").toLocalDateTime(),rs.getString("TrangThai"));
    }

    private static LocalDateTime now(Connection c) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT NOW()");ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getTimestamp(1).toLocalDateTime(); }
    }

    private static void lockRooms(Connection c,String oldRoom,String newRoom) throws SQLException {
        for(String room:new TreeSet<>(List.of(oldRoom,newRoom))) {
            try(PreparedStatement ps=c.prepareStatement("SELECT MaPhong FROM PHONGCHIEU WHERE MaPhong=? FOR UPDATE")) {
                ps.setString(1,room);
                try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy phòng chiếu"); }
            }
        }
    }

    private static SuatChieu validate(Connection c,SuatChieu proposed,Set<LocalDate> holidays) throws SQLException {
        if(!proposed.thoiGianBatDau().isAfter(now(c))) throw new IllegalArgumentException("Thời điểm bắt đầu phải ở tương lai");
        int duration; String format;
        try(PreparedStatement ps=c.prepareStatement("SELECT ThoiLuong,DinhDangChieu,TrangThai FROM PHIM WHERE MaPhim=? FOR SHARE")) {
            ps.setString(1,proposed.maPhim());
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next() || !List.of("Sắp chiếu","Đang chiếu").contains(rs.getString("TrangThai")))
                    throw new IllegalArgumentException("Phim không được phép xếp lịch");
                duration=rs.getInt("ThoiLuong"); format=rs.getString("DinhDangChieu");
            }
        }
        int cleanup;
        try(PreparedStatement ps=c.prepareStatement(
                "SELECT p.TrangThai,p.ThoiGianDonPhong,l.DinhDangChieu FROM PHONGCHIEU p JOIN LOAIPHONG l ON l.MaLoaiPhong=p.MaLoaiPhong WHERE p.MaPhong=? FOR SHARE")) {
            ps.setString(1,proposed.maPhong());
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next() || !"Đang hoạt động".equals(rs.getString(1))) throw new IllegalArgumentException("Phòng chiếu không hoạt động");
                cleanup=rs.getInt(2);
                if(!format.equalsIgnoreCase(rs.getString(3))) throw new IllegalArgumentException("Định dạng phim không phù hợp với phòng chiếu");
            }
        }
        boolean ready=false;
        try(PreparedStatement ps=c.prepareStatement("SELECT TrangThai FROM GHE WHERE MaPhong=? FOR SHARE")) {
            ps.setString(1,proposed.maPhong());
            try(ResultSet rs=ps.executeQuery()) { while(rs.next()) ready|="Sẵn sàng sử dụng".equals(rs.getString(1)); }
        }
        if(!ready) throw new IllegalArgumentException("Phòng chưa có sơ đồ ghế sẵn sàng sử dụng");
        LocalDate date=proposed.thoiGianBatDau().toLocalDate();
        String type=holidays.contains(date)?"Ngày lễ":(date.getDayOfWeek()==DayOfWeek.SATURDAY || date.getDayOfWeek()==DayOfWeek.SUNDAY?"Cuối tuần":"Ngày thường");
        int typeId;
        try(PreparedStatement ps=c.prepareStatement("SELECT MaLoaiThoiDiem FROM LOAITHOIDIEM WHERE TenLoaiThoiDiem=? FOR SHARE")) {
            ps.setString(1,type);
            try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("Chưa cấu hình loại thời điểm "+type); typeId=rs.getInt(1); }
        }
        return new SuatChieu(proposed.maSuat(),proposed.maPhim(),proposed.maPhong(),typeId,proposed.thoiGianBatDau(),
                proposed.thoiGianBatDau().plusMinutes((long)duration+cleanup),proposed.trangThai());
    }

    private static void checkConflict(Connection c,SuatChieu candidate) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(
                "SELECT 1 FROM SUATCHIEU WHERE MaPhong=? AND MaSuat<>? AND TrangThai<>'Đã hủy' AND ThoiGianBatDau<? AND ThoiGianTraPhong>? LIMIT 1")) {
            ps.setString(1,candidate.maPhong()); ps.setInt(2,candidate.maSuat());
            ps.setTimestamp(3,Timestamp.valueOf(candidate.thoiGianTraPhong())); ps.setTimestamp(4,Timestamp.valueOf(candidate.thoiGianBatDau()));
            try(ResultSet rs=ps.executeQuery()) { if(rs.next()) throw new IllegalArgumentException("Lịch chiếu bị trùng"); }
        }
    }

    private static void requireEditable(Connection c,SuatChieu schedule) throws SQLException {
        if("Đã hủy".equals(schedule.trangThai())) throw new IllegalArgumentException("Suất chiếu đã bị hủy");
        if(!schedule.thoiGianBatDau().isAfter(now(c))) throw new IllegalArgumentException("Suất chiếu đã bắt đầu, không được thay đổi hoặc hủy");
        try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM VE WHERE MaSuat=? LIMIT 1")) {
            ps.setInt(1,schedule.maSuat());
            try(ResultSet rs=ps.executeQuery()) { if(rs.next()) throw new IllegalArgumentException("Suất chiếu đã phát sinh vé bán, không được thay đổi hoặc hủy"); }
        }
        try(PreparedStatement ps=c.prepareStatement("SELECT TrangThai FROM GHE_SUATCHIEU WHERE MaSuat=? FOR UPDATE")) {
            ps.setInt(1,schedule.maSuat());
            try(ResultSet rs=ps.executeQuery()) { while(rs.next()) if("Đã bán".equals(rs.getString(1)))
                throw new IllegalArgumentException("Suất chiếu đã bán ghế, không được thay đổi hoặc hủy"); }
        }
        try(PreparedStatement ps=c.prepareStatement(
                "SELECT 1 FROM GHE_SUATCHIEU s JOIN GIAODICH g ON g.MaGiaoDich=s.MaGiaoDich LEFT JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich "
                        + "WHERE s.MaSuat=? AND (g.TrangThai NOT IN ('Chờ thanh toán','Đã hủy','Hết hạn') OR t.MaThanhToan IS NOT NULL) LIMIT 1")) {
            ps.setInt(1,schedule.maSuat());
            try(ResultSet rs=ps.executeQuery()) { if(rs.next()) throw new IllegalArgumentException("Có giao dịch đang xử lý hoặc đã thanh toán. Vui lòng kiểm tra giao dịch trước"); }
        }
    }

    private static void releaseUnpaidHolds(Connection c,int schedule) throws SQLException {
        List<String> owners=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement("SELECT DISTINCT MaGiaoDich FROM GHE_SUATCHIEU WHERE MaSuat=? AND MaGiaoDich IS NOT NULL ORDER BY MaGiaoDich")) {
            ps.setInt(1,schedule); try(ResultSet rs=ps.executeQuery()) { while(rs.next()) owners.add(rs.getString(1)); }
        }
        GiuGheDAO.releaseOwners(c,owners);
        execute(c,"UPDATE GHE_SUATCHIEU SET MaGia=NULL,GiaVeChot=NULL WHERE MaSuat=?",schedule);
    }

    private static void initializeSeats(Connection c,int id,String room) throws SQLException {
        execute(c,"INSERT INTO GHE_SUATCHIEU (MaSuat,MaGhe,TrangThai) SELECT ?,MaGhe,CASE WHEN TrangThai='Sẵn sàng sử dụng' THEN 'Trống' ELSE 'Không khả dụng' END FROM GHE WHERE MaPhong=?",id,room);
    }

    private static void bind(PreparedStatement ps,SuatChieu s) throws SQLException {
        ps.setString(1,s.maPhim()); ps.setString(2,s.maPhong()); ps.setInt(3,s.maLoaiThoiDiem());
        ps.setTimestamp(4,Timestamp.valueOf(s.thoiGianBatDau())); ps.setTimestamp(5,Timestamp.valueOf(s.thoiGianTraPhong())); ps.setString(6,s.trangThai());
    }

    private static void execute(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); }
    }
    private static void rollback(Connection c,Exception e) { try { c.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); } }
}
