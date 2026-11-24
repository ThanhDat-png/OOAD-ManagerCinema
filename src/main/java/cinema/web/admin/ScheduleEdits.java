package cinema.web.admin;

import cinema.web.db.WebDb.Tx;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import static cinema.web.admin.AdminUi.*;
import static cinema.web.admin.AdminWrites.reference;

@Service
class ScheduleEdits {
    private final Set<LocalDate> holidays;
    ScheduleEdits(Environment env){
        Set<LocalDate> days=new HashSet<>();
        addHolidays(days,env.getProperty("CINEMA_HOLIDAYS",""));
        Path file=Path.of("config","calendar.properties");
        if(Files.exists(file)){
            var config=new Properties();
            try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){
                config.load(reader);
                addHolidays(days,config.getProperty("holidays",""));
            }catch(IOException|DateTimeParseException e){
                throw new IllegalArgumentException("Cấu hình ngày lễ không hợp lệ. Dùng yyyy-MM-dd, phân tách bằng dấu phẩy",e);
            }
        }
        holidays=Set.copyOf(days);
    }
    private static void addHolidays(Set<LocalDate> days,String raw){
        for(String date:raw.split(","))if(!date.isBlank())days.add(LocalDate.parse(date.trim()));
    }
    void editable(Tx tx,String id){
        var s=one(tx.query("SELECT * FROM SUATCHIEU WHERE MaSuat=?",id));
        check(!"Đã hủy".equals(s.get("TrangThai")),"Suất chiếu đã bị hủy.");
        check(s.get("ThoiGianBatDau").toString().compareTo(now())>0,"Suất chiếu đã bắt đầu, không thể thay đổi hoặc hủy.");
        check(tx.query("SELECT 1 FROM VE WHERE MaSuat=? LIMIT 1",id).isEmpty()&&tx.query("""
                SELECT 1 FROM GHE_SUATCHIEU h LEFT JOIN GIAODICH g ON g.MaGiaoDich=h.MaGiaoDich
                LEFT JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich WHERE h.MaSuat=?
                AND (h.TrangThai='Đã bán' OR g.TrangThai IN ('Đang thanh toán','Đã thanh toán') OR t.MaThanhToan IS NOT NULL) LIMIT 1
                """,id).isEmpty(),"Suất đã phát sinh vé bán hoặc thanh toán. Không thể thay đổi hoặc hủy.");
    }
    private void release(Tx tx,List<Map<String,Object>> holds) {
        for(var hold:holds){
            Object cart=hold.get("MaGiaoDich");
            check(tx.query("SELECT 1 FROM THANHTOAN WHERE MaGiaoDich=?",cart).isEmpty(),"Giao dịch đang xử lý thanh toán.");
            tx.update("UPDATE GIAODICH SET TrangThai='Đã hủy' WHERE MaGiaoDich=? AND TrangThai='Chờ thanh toán'",cart);
            tx.update("UPDATE WEB_CUSTOMER_SELECTION SET MaGiaoDich=NULL WHERE MaGiaoDich=?",cart);
            tx.update("""
                    UPDATE GHE_SUATCHIEU SET TrangThai=CASE WHEN EXISTS (SELECT 1 FROM GHE g WHERE g.MaGhe=GHE_SUATCHIEU.MaGhe AND g.TrangThai='Sẵn sàng sử dụng') THEN 'Trống' ELSE 'Không khả dụng' END,
                    MaGiaoDich=NULL,ThoiGianHetHanGiu=NULL,MaGia=NULL,GiaVeChot=NULL WHERE MaGiaoDich=? AND TrangThai='Đang giữ'
                    """,cart);
        }
    }
    String show(Tx tx,String op,String id,Map<String,String> p){
        boolean create=op.equals("show-new");
        Map<String,Object> old=Map.of();LocalDateTime oldTime=null;
        if(!create){
            editable(tx,id);old=one(tx.query("SELECT * FROM SUATCHIEU WHERE MaSuat=?",id));
            oldTime=LocalDateTime.parse(old.get("ThoiGianBatDau").toString(),SQL_TIME);
            release(tx,tx.query("SELECT DISTINCT MaGiaoDich FROM GHE_SUATCHIEU WHERE MaSuat=? AND TrangThai='Đang giữ'",id));
        }
        if(op.equals("show-cancel")){
            tx.update("UPDATE SUATCHIEU SET TrangThai='Đã hủy' WHERE MaSuat=?",id);
            tx.update("UPDATE GHE_SUATCHIEU SET TrangThai='Không khả dụng',MaGia=NULL,GiaVeChot=NULL WHERE MaSuat=?",id);
            return id;
        }
        String film=create||op.equals("show-film")?required(p,"MaPhim",10):old.get("MaPhim").toString();
        String room=create||op.equals("show-room")?required(p,"MaPhong",10):old.get("MaPhong").toString();
        LocalDate day=create||op.equals("show-date")?date(p,"date"):oldTime.toLocalDate();
        LocalTime time;
        try{time=create||op.equals("show-time")?LocalTime.parse(val(p,"time")):oldTime.toLocalTime();}
        catch(DateTimeException e){throw new IllegalArgumentException("Giờ chiếu không hợp lệ.");}
        var start=day.atTime(time);check(start.isAfter(LocalDateTime.now(ZONE)),"Thời gian bắt đầu phải ở tương lai.");
        var f=tx.query("SELECT * FROM PHIM WHERE MaPhim=? AND TrangThai IN ('Sắp chiếu','Đang chiếu')",film);
        var r=tx.query(Catalog.ROOM_SQL+" WHERE p.MaPhong=? AND p.TrangThai='Đang hoạt động'",room);
        check(!f.isEmpty(),"Phim không được phép xếp lịch.");check(!r.isEmpty(),"Phòng chiếu không hoạt động.");
        check(f.get(0).get("DinhDangChieu").toString().equalsIgnoreCase(r.get(0).get("DinhDangChieu").toString()),"Định dạng phim không phù hợp với phòng.");
        check(!tx.query("SELECT 1 FROM GHE WHERE MaPhong=? AND TrangThai='Sẵn sàng sử dụng' LIMIT 1",room).isEmpty(),"Phòng chưa có ghế sẵn sàng sử dụng.");
        LocalDateTime end=start.plusMinutes(((Number)f.get(0).get("ThoiLuong")).longValue()+((Number)r.get(0).get("ThoiGianDonPhong")).longValue());
        String from=SQL_TIME.format(start),to=SQL_TIME.format(end);
        check(tx.query("SELECT 1 FROM SUATCHIEU WHERE MaPhong=? AND MaSuat<>? AND TrangThai<>'Đã hủy' AND ThoiGianBatDau<? AND ThoiGianTraPhong>? LIMIT 1",room,create?-1:id,to,from).isEmpty(),"Trùng lịch chiếu trong phòng, bao gồm thời gian dọn phòng.");
        String period=holidays.contains(day)?"Ngày lễ":day.getDayOfWeek().getValue()>5?"Cuối tuần":"Ngày thường";
        Object periodId=one(tx.query("SELECT MaLoaiThoiDiem FROM LOAITHOIDIEM WHERE TenLoaiThoiDiem=?",period)).get("MaLoaiThoiDiem");
        boolean replaceSeats=create||!room.equals(old.get("MaPhong"));
        if(create){
            tx.update("INSERT INTO SUATCHIEU (MaPhim,MaPhong,MaLoaiThoiDiem,ThoiGianBatDau,ThoiGianTraPhong,TrangThai) VALUES (?,?,?,?,?,'Sắp chiếu')",film,room,periodId,from,to);
            id=one(tx.query("SELECT last_insert_rowid() id")).get("id").toString();
        }else{
            if(replaceSeats)tx.update("DELETE FROM GHE_SUATCHIEU WHERE MaSuat=?",id);
            tx.update("UPDATE SUATCHIEU SET MaPhim=?,MaPhong=?,MaLoaiThoiDiem=?,ThoiGianBatDau=?,ThoiGianTraPhong=? WHERE MaSuat=?",film,room,periodId,from,to,id);
        }
        if(replaceSeats)tx.update("INSERT INTO GHE_SUATCHIEU (MaSuat,MaGhe,TrangThai) SELECT ?,MaGhe,CASE WHEN TrangThai='Sẵn sàng sử dụng' THEN 'Trống' ELSE 'Không khả dụng' END FROM GHE WHERE MaPhong=?",id,room);
        return id;
    }
    private static boolean hasUnresolvedRoomSales(Tx tx,String roomId){
        return !tx.query("""
                SELECT 1 FROM GHE_SUATCHIEU h JOIN SUATCHIEU s ON s.MaSuat=h.MaSuat
                LEFT JOIN GIAODICH g ON g.MaGiaoDich=h.MaGiaoDich
                LEFT JOIN THANHTOAN t ON t.MaGiaoDich=h.MaGiaoDich
                WHERE s.MaPhong=? AND s.TrangThai<>'Đã hủy'
                AND (g.TrangThai='Đang thanh toán'
                     OR ((g.TrangThai='Đã thanh toán' OR t.MaThanhToan IS NOT NULL)
                         AND NOT EXISTS (SELECT 1 FROM VE v WHERE v.MaSuat=h.MaSuat AND v.MaGhe=h.MaGhe)))
                LIMIT 1
                """,roomId).isEmpty();
    }
    String room(Tx tx,String op,String id,Map<String,String> p){
        boolean create=op.equals("room-new");
        var old=create?Map.<String,Object>of():one(tx.query("SELECT * FROM PHONGCHIEU WHERE MaPhong=?",id));
        boolean live=!create&&(!tx.query("SELECT 1 FROM SUATCHIEU WHERE MaPhong=? AND TrangThai<>'Đã hủy' AND ThoiGianTraPhong>? LIMIT 1",id,now()).isEmpty()||hasUnresolvedRoomSales(tx,id));
        if(op.equals("room-state")){
            String state=member(p,"TrangThai",roomStates);
            if(!state.equals(old.get("TrangThai"))){
                if(state.equals(roomStates[0]))check(!tx.query("SELECT 1 FROM GHE WHERE MaPhong=? AND TrangThai='Sẵn sàng sử dụng' LIMIT 1",id).isEmpty(),"Phòng phải có ghế sẵn sàng sử dụng để hoạt động.");
                else check(!live,"Phòng còn suất chiếu chưa kết thúc.");
                tx.update("UPDATE PHONGCHIEU SET TrangThai=? WHERE MaPhong=?",state,id);
            }return id;
        }
        String name=required(p,"TenPhong",50);int clean=number(p,"ThoiGianDonPhong",0),type=reference(tx,p,"MaLoaiPhong","LOAIPHONG");
        check(tx.query("SELECT 1 FROM PHONGCHIEU WHERE TenPhong=? COLLATE NOCASE AND MaPhong<>?",name,id).isEmpty(),"Tên phòng đã tồn tại.");
        if(create){
            id=id("R");tx.update("INSERT INTO PHONGCHIEU (MaPhong,TenPhong,MaLoaiPhong,ThoiGianDonPhong,TrangThai) VALUES (?,?,?,?,?)",id,name,type,clean,member(p,"TrangThai",roomStates));
        }else{
            check(!live||(Integer.toString(type).equals(old.get("MaLoaiPhong").toString())&&Integer.toString(clean).equals(old.get("ThoiGianDonPhong").toString())),"Phòng còn suất chiếu chưa kết thúc, không thể đổi loại phòng hoặc thời gian dọn.");
            tx.update("UPDATE PHONGCHIEU SET TenPhong=?,MaLoaiPhong=?,ThoiGianDonPhong=? WHERE MaPhong=?",name,type,clean,id);
        }
        return id;
    }
    private static final String AFFECTED="""
            FROM GHE_SUATCHIEU h JOIN SUATCHIEU s ON s.MaSuat=h.MaSuat
            LEFT JOIN GIAODICH g ON g.MaGiaoDich=h.MaGiaoDich
            LEFT JOIN THANHTOAN t ON t.MaGiaoDich=h.MaGiaoDich
            WHERE s.MaPhong=? AND s.TrangThai<>'Đã hủy'
            """;
    String seat(Tx tx,String op,String id,Map<String,String> p){
        boolean create=op.equals("seat-new"),classify=op.equals("seat-type");
        Map<String,Object> old=create?Map.of():one(tx.query("SELECT * FROM GHE WHERE MaGhe=?",id));
        String room=create?id:old.get("MaPhong").toString();
        String row=classify?old.get("HangGhe").toString():required(p,"HangGhe",2).toUpperCase(Locale.ROOT);
        int number=classify?((Number)old.get("SoGhe")).intValue():number(p,"SoGhe",1);
        String state=classify?old.get("TrangThai").toString():member(p,"TrangThai",seatStates);
        int type=create||classify?reference(tx,p,"MaLoaiGhe","LOAIGHE"):((Number)old.get("MaLoaiGhe")).intValue();
        check(row.matches("[A-Z]{1,2}"),"Hàng ghế phải gồm 1–2 chữ A–Z.");
        check(tx.query("SELECT 1 FROM GHE WHERE MaPhong=? AND HangGhe=? AND SoGhe=? AND MaGhe<>?",room,row,number,create?-1:id).isEmpty(),"Vị trí ghế đã tồn tại.");
        boolean structural=create||!row.equals(old.get("HangGhe"))||!String.valueOf(number).equals(old.get("SoGhe").toString())||!String.valueOf(type).equals(old.get("MaLoaiGhe").toString());
        if(!create&&!structural&&state.equals(old.get("TrangThai")))return room;
        String now=now();
        check(tx.query("SELECT 1\n"+AFFECTED+"""
                 AND (?=1 OR h.MaGhe=?)
                 AND ((s.ThoiGianTraPhong>? AND (h.TrangThai='Đã bán' OR g.TrangThai IN ('Đang thanh toán','Đã thanh toán')
                       OR EXISTS (SELECT 1 FROM VE v WHERE v.MaSuat=s.MaSuat AND v.MaGhe=h.MaGhe) OR t.MaThanhToan IS NOT NULL))
                      OR g.TrangThai='Đang thanh toán'
                      OR ((g.TrangThai='Đã thanh toán' OR t.MaThanhToan IS NOT NULL)
                          AND NOT EXISTS (SELECT 1 FROM VE v WHERE v.MaSuat=s.MaSuat AND v.MaGhe=h.MaGhe)))
                 LIMIT 1
                """,room,structural?1:0,id,now).isEmpty(),"Sơ đồ ghế có vé bán hoặc giao dịch thanh toán liên quan, không thể thay đổi.");
        String saleState=state.equals(seatStates[0])?"Trống":"Không khả dụng";
        if(create){
            tx.update("INSERT INTO GHE (MaPhong,HangGhe,SoGhe,MaLoaiGhe,TrangThai) VALUES (?,?,?,?,?)",room,row,number,type,state);
            Object seat=one(tx.query("SELECT last_insert_rowid() id")).get("id");
            tx.update("INSERT INTO GHE_SUATCHIEU (MaSuat,MaGhe,TrangThai) SELECT MaSuat,?,? FROM SUATCHIEU WHERE MaPhong=? AND TrangThai<>'Đã hủy' AND ThoiGianTraPhong>?",seat,saleState,room,now);return room;
        }
        release(tx,tx.query("SELECT DISTINCT h.MaGiaoDich "+AFFECTED+" AND s.ThoiGianTraPhong>? AND h.MaGhe=? AND h.TrangThai='Đang giữ'",room,now,id));
        tx.update("UPDATE GHE SET HangGhe=?,SoGhe=?,MaLoaiGhe=?,TrangThai=? WHERE MaGhe=?",row,number,type,state,id);
        tx.update("UPDATE GHE_SUATCHIEU SET TrangThai=?,MaGia=NULL,GiaVeChot=NULL WHERE MaGhe=? AND MaGiaoDich IS NULL AND TrangThai<>'Đã bán' AND MaSuat IN (SELECT MaSuat FROM SUATCHIEU WHERE MaPhong=? AND TrangThai<>'Đã hủy' AND ThoiGianTraPhong>?)",saleState,id,room,now);return room;
    }
}
