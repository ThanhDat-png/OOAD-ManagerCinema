package cinema.dao;

import cinema.db.Database;
import cinema.model.Phim;
import cinema.model.TheLoai;
import java.sql.*;
import java.util.*;

public class PhimDAO {
    private static final String SELECT = """
            SELECT p.*, t.MaTheLoai, t.TenTheLoai FROM PHIM p
            LEFT JOIN PHIM_THELOAI pt ON pt.MaPhim=p.MaPhim
            LEFT JOIN THELOAI t ON t.MaTheLoai=pt.MaTheLoai
            """;

    public List<Phim> search(String name, Integer genre, String status) throws SQLException {
        String sql = SELECT + """
                WHERE p.TenPhim LIKE ? ESCAPE '='
                AND (? IS NULL OR EXISTS (SELECT 1 FROM PHIM_THELOAI f WHERE f.MaPhim=p.MaPhim AND f.MaTheLoai=?))
                AND (? IS NULL OR p.TrangThai=?) ORDER BY p.TenPhim, p.MaPhim, t.TenTheLoai
                """;
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + name.replace("=", "==").replace("%", "=%").replace("_", "=_") + "%");
            ps.setObject(2, genre, Types.INTEGER); ps.setObject(3, genre, Types.INTEGER);
            ps.setString(4, status); ps.setString(5, status);
            try (ResultSet rs = ps.executeQuery()) { return readAll(rs); }
        }
    }

    public Optional<Phim> find(String id) throws SQLException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                SELECT + " WHERE p.MaPhim=? ORDER BY t.TenTheLoai")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { return readAll(rs).stream().findFirst(); }
        }
    }

    public void insert(Phim film) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO PHIM VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, film.maPhim()); ps.setString(2, film.tenPhim());
                    ps.setInt(3, film.thoiLuong()); ps.setString(4, film.daoDien());
                    ps.setDate(5, java.sql.Date.valueOf(film.ngayKhoiChieu())); ps.setInt(6, film.gioiHanTuoi());
                    ps.setString(7, film.poster()); ps.setString(8, film.trangThai());
                    ps.setString(9, film.dinhDangChieu()); ps.executeUpdate();
                }
                addGenres(c, film.maPhim(), film.theLoai());
                c.commit();
            } catch (SQLException | RuntimeException e) { rollback(c, e); throw e; }
        }
    }

    public void rename(String id, String name) throws SQLException { updateValue("TenPhim", id, name); }
    public void updatePoster(String id, String poster) throws SQLException { updateValue("Poster", id, poster); }

    private void updateValue(String column, String id, String value) throws SQLException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "UPDATE PHIM SET " + column + "=? WHERE MaPhim=?")) {
            ps.setString(1, value); ps.setString(2, id);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không tìm thấy phim");
        }
    }

    public void updateGenres(String id, List<TheLoai> genres) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                lock(c, id);
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM PHIM_THELOAI WHERE MaPhim=?")) {
                    ps.setString(1, id); ps.executeUpdate();
                }
                addGenres(c, id, genres);
                c.commit();
            } catch (SQLException | RuntimeException e) { rollback(c, e); throw e; }
        }
    }

    public void stop(String id) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                if ("Ngừng chiếu".equals(lock(c, id))) throw new IllegalArgumentException("Phim đã ở trạng thái Ngừng chiếu");
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM SUATCHIEU WHERE MaPhim=? AND TrangThai <> 'Đã hủy' AND ThoiGianTraPhong > NOW() LIMIT 1")) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new IllegalArgumentException("Phim vẫn còn suất chiếu chưa kết thúc");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE PHIM SET TrangThai='Ngừng chiếu' WHERE MaPhim=?")) {
                    ps.setString(1, id); ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { rollback(c, e); throw e; }
        }
    }

    private static String lock(Connection c, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT TrangThai FROM PHIM WHERE MaPhim=? FOR UPDATE")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy phim");
                return rs.getString(1);
            }
        }
    }

    private static void addGenres(Connection c, String id, List<TheLoai> genres) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO PHIM_THELOAI VALUES (?, ?)")) {
            for (TheLoai genre : genres) {
                ps.setString(1, id); ps.setInt(2, genre.maTheLoai()); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static List<Phim> readAll(ResultSet rs) throws SQLException {
        Map<String, Phim> films = new LinkedHashMap<>();
        Map<String, List<TheLoai>> genres = new HashMap<>();
        while (rs.next()) {
            String id = rs.getString("MaPhim");
            if (!films.containsKey(id)) {
                java.sql.Date date = rs.getDate("NgayKhoiChieu");
                films.put(id, new Phim(id, rs.getString("TenPhim"), rs.getInt("ThoiLuong"), rs.getString("DaoDien"),
                        date.toLocalDate(), rs.getInt("GioiHanTuoi"), rs.getString("Poster"),
                        rs.getString("TrangThai"), rs.getString("DinhDangChieu"), List.of()));
                genres.put(id, new ArrayList<>());
            }
            if (rs.getObject("MaTheLoai") != null)
                genres.get(id).add(new TheLoai(rs.getInt("MaTheLoai"), rs.getString("TenTheLoai")));
        }
        List<Phim> result = new ArrayList<>();
        films.forEach((id, film) -> result.add(film.withTheLoai(genres.get(id))));
        return result;
    }

    private static void rollback(Connection c, Exception error) {
        try { c.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
    }
}
