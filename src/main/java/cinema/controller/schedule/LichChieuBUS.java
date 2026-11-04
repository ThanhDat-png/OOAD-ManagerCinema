package cinema.controller.schedule;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.*;
import cinema.model.*;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class LichChieuBUS {
    private final PhienDangNhap session;
    private final SuatChieuDAO dao;
    private final Set<LocalDate> holidays;

    public LichChieuBUS(PhienDangNhap session, SuatChieuDAO dao) {
        this(session,dao,loadHolidays());
    }

    public LichChieuBUS(PhienDangNhap session, SuatChieuDAO dao, Set<LocalDate> holidays) {
        if(session==null || session.vaiTro()==null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        this.session=session; this.dao=dao; this.holidays=Set.copyOf(holidays);
    }

    private void requireAdmin() {
        if(session.vaiTro()!=VaiTro.ADMIN) throw new IllegalArgumentException("Bạn không có quyền quản lý lịch chiếu");
    }

    public List<SuatChieu> list(LocalDate date,String room) throws SQLException {
        requireAdmin(); return dao.list(date,room);
    }
    public SuatChieu editable(int id) throws SQLException { requireAdmin(); return dao.editable(id); }
    public SuatChieu detail(int id) throws SQLException { requireAdmin(); return dao.find(id); }
    public List<Phim> films() throws SQLException { requireAdmin(); return new PhimDAO().search("",null,null); }
    public List<PhongChieu> rooms() throws SQLException { requireAdmin(); return new PhongChieuDAO().all(); }

    public List<LoaiThoiDiem> periods() throws SQLException {
        requireAdmin(); return new LoaiThoiDiemDAO().all();
    }

    public SuatChieu create(String film,String room,LocalDateTime start) throws SQLException {
        requireAdmin(); requireId(film); requireId(room); requireTime(start);
        return dao.create(film,room,start.withNano(0),holidays);
    }

    public SuatChieu changeFilm(int id,String film) throws SQLException {
        requireAdmin(); requireId(film); return dao.modify(id,old -> old.withFilm(film),holidays);
    }

    public SuatChieu changeRoom(int id,String room) throws SQLException {
        requireAdmin(); requireId(room); return dao.modify(id,old -> old.withRoom(room),holidays);
    }

    public SuatChieu changeDate(int id,LocalDate date) throws SQLException {
        requireAdmin(); if(date==null) throw new IllegalArgumentException("Vui lòng chọn ngày chiếu");
        requireTime(date.atStartOfDay());
        return dao.modify(id,old -> old.withStart(LocalDateTime.of(date,old.thoiGianBatDau().toLocalTime())),holidays);
    }

    public SuatChieu changeTime(int id,LocalTime time) throws SQLException {
        requireAdmin(); if(time==null) throw new IllegalArgumentException("Vui lòng nhập giờ chiếu");
        return dao.modify(id,old -> old.withStart(LocalDateTime.of(old.thoiGianBatDau().toLocalDate(),time.withNano(0))),holidays);
    }

    public void cancel(int id) throws SQLException { requireAdmin(); dao.cancel(id); }

    private void requireId(String value) {
        if(value==null || value.isBlank()) throw new IllegalArgumentException("Vui lòng chọn đầy đủ phim và phòng chiếu");
    }
    private void requireTime(LocalDateTime value) {
        if(value==null || value.getYear()<1000 || value.getYear()>9999) throw new IllegalArgumentException("Ngày giờ chiếu không hợp lệ");
    }

    private static Set<LocalDate> loadHolidays() {
        Path path=Path.of("config","calendar.properties");
        if(!Files.exists(path)) return Set.of();
        Properties config=new Properties();
        try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) {
            config.load(reader); Set<LocalDate> result=new HashSet<>();
            for(String value:config.getProperty("holidays","").split(","))
                if(!value.isBlank()) result.add(LocalDate.parse(value.trim()));
            return result;
        } catch(IOException|DateTimeParseException e) {
            throw new IllegalArgumentException("Cấu hình ngày lễ không hợp lệ. Dùng yyyy-MM-dd, phân tách bằng dấu phẩy",e);
        }
    }
}
