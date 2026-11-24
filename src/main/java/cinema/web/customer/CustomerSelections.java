package cinema.web.customer;

import cinema.web.db.WebDb;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
class CustomerSelections {
    static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String VISIBLE = "p.TrangThai IN ('Sắp chiếu','Đang chiếu')";
    private static final String FILMS = """
            SELECT p.*, (p.Poster=p.MaPhim) AS SeedArtwork, (SELECT group_concat(t.TenTheLoai, ', ')
            FROM PHIM_THELOAI pt JOIN THELOAI t ON t.MaTheLoai=pt.MaTheLoai
            WHERE pt.MaPhim=p.MaPhim) AS TheLoai
            FROM PHIM p WHERE
            """;
    private static final String SHOWS = """
            SELECT s.*, p.TenPhim,p.ThoiLuong,p.DinhDangChieu,p.GioiHanTuoi,r.TenPhong,
                   strftime('%d/%m · %H:%M',s.ThoiGianBatDau) AS Lich
            FROM SUATCHIEU s JOIN PHIM p ON p.MaPhim=s.MaPhim
            JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong
            WHERE s.TrangThai<>'Đã hủy' AND s.ThoiGianBatDau>?
            AND r.TrangThai='Đang hoạt động' AND
            """;
    private static final String SEATS = """
            SELECT g.MaGhe,g.HangGhe,g.SoGhe,g.HangGhe || g.SoGhe AS Cho,l.TenLoaiGhe,b.GiaVe,
                   CASE WHEN g.TrangThai='Sẵn sàng sử dụng' AND b.GiaVe IS NOT NULL
                    AND (h.TrangThai='Trống' OR (h.TrangThai='Đang giữ'
                    AND gd.TrangThai='Chờ thanh toán' AND h.ThoiGianHetHanGiu<=?)) THEN 1 ELSE 0 END AS Available
            FROM GHE_SUATCHIEU h JOIN GHE g ON g.MaGhe=h.MaGhe
            JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe
            JOIN SUATCHIEU s ON s.MaSuat=h.MaSuat AND s.MaPhong=g.MaPhong
            JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong
            LEFT JOIN BANGGIAVE b ON b.MaLoaiGhe=g.MaLoaiGhe AND b.MaLoaiPhong=r.MaLoaiPhong
                AND b.MaLoaiThoiDiem=s.MaLoaiThoiDiem
            LEFT JOIN GIAODICH gd ON gd.MaGiaoDich=h.MaGiaoDich
            WHERE h.MaSuat=? ORDER BY g.HangGhe,g.SoGhe
            """;
    private final WebDb db;

    CustomerSelections(WebDb db) {
        this.db = db;
    }

    private static String timeNow() {
        return LocalDateTime.now(CINEMA_ZONE).format(DB_TIME);
    }

    private static Map<String,Object> first(List<Map<String,Object>> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }

    List<Map<String,Object>> films() {
        return db.query(FILMS + VISIBLE + " ORDER BY p.TrangThai,p.TenPhim");
    }

    Map<String,Object> film(String id) {
        return first(db.query(FILMS + VISIBLE + " AND p.MaPhim=?", id));
    }

    Map<String,Object> poster(String id) {
        return first(db.query("""
                SELECT w.Mime,w.Data FROM WEB_POSTER w JOIN PHIM p ON p.Poster=w.Id
                WHERE p.MaPhim=? AND
                """ + VISIBLE, id));
    }

    List<Map<String,Object>> shows(String film) {
        return db.query(SHOWS + VISIBLE + " AND p.MaPhim=? ORDER BY s.ThoiGianBatDau,s.MaSuat", timeNow(), film);
    }

    Map<String,Object> show(int show) {
        return first(db.query(SHOWS + VISIBLE + " AND s.MaSuat=?", timeNow(), show));
    }

    List<Map<String,Object>> seats(int show) {
        return db.query(SEATS, timeNow(), show).stream().map(row -> {
            var seat = new LinkedHashMap<>(row);
            seat.put("Available", isAvailable(row));
            return (Map<String,Object>) seat;
        }).toList();
    }

    private static boolean isAvailable(Map<String,Object> seat) {
        return ((Number) seat.get("Available")).intValue() == 1;
    }

    String create(int show, List<Integer> requested) {
        if (requested == null || requested.isEmpty()
                || new HashSet<>(requested).size() != requested.size()) {
            throw new IllegalArgumentException("Vui lòng chọn ghế còn trống.");
        }
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase(Locale.ROOT);
        return db.write(tx -> {
            String now = timeNow();
            var slot = first(tx.query(SHOWS + VISIBLE + " AND s.MaSuat=?", now, show));
            if (slot == null) throw new IllegalArgumentException("Suất chiếu không còn nhận đặt vé.");
            var wanted = new HashSet<>(requested);
            var seats = tx.query(SEATS, now, show).stream()
                    .filter(seat -> wanted.contains(((Number) seat.get("MaGhe")).intValue()))
                    .filter(CustomerSelections::isAvailable).toList();
            if (seats.size() != requested.size()) {
                throw new IllegalArgumentException("Có ghế không còn khả dụng. Vui lòng cập nhật lựa chọn.");
            }
            String showStart = slot.get("ThoiGianBatDau").toString();
            String expiresAt = LocalDateTime.parse(now, DB_TIME).plusMinutes(30).format(DB_TIME);
            if (showStart.compareTo(expiresAt) < 0) expiresAt = showStart;
            tx.update("DELETE FROM WEB_CUSTOMER_SELECTION WHERE MaGiaoDich IS NULL AND ExpiresAt<=?", now);
            tx.update("""
                    INSERT INTO WEB_CUSTOMER_SELECTION (Reference,MaSuat,MaPhim,MaPhong,ShowStart,ExpiresAt)
                    VALUES (?,?,?,?,?,?)
                    """, code, show, slot.get("MaPhim"), slot.get("MaPhong"), showStart, expiresAt);
            for (var seat : seats) {
                tx.update("""
                        INSERT INTO WEB_CUSTOMER_SELECTION_SEAT (Reference,MaGhe,Cho,TenLoaiGhe,GiaVe)
                        VALUES (?,?,?,?,?)
                        """, code, seat.get("MaGhe"), seat.get("Cho"), seat.get("TenLoaiGhe"), seat.get("GiaVe"));
            }
            return code;
        });
    }

    record Selection(String reference, Map<String,Object> show, List<Map<String,Object>> seats, long total,
                     LocalDateTime expiresAt, boolean claimed, boolean paid, boolean showChanged) {}

    Selection find(String code) {
        if (code == null || !code.strip().matches("(?i)[a-f0-9]{24}")) return null;
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        String now = timeNow();
        var slot = first(db.query("""
                SELECT p.MaPhim,p.TenPhim,p.TrangThai AS FilmState,r.TenPhong,r.TrangThai AS RoomState,
                       w.MaSuat,w.MaPhong,w.ShowStart AS ThoiGianBatDau,w.ExpiresAt,w.MaGiaoDich,
                       strftime('%d/%m · %H:%M',w.ShowStart) AS Lich,
                       s.MaPhim AS LiveFilm,s.MaPhong AS LiveRoom,s.ThoiGianBatDau AS LiveStart,
                       s.TrangThai AS LiveShowState,
                       gd.TrangThai AS OrderState,
                       (SELECT max(h.ThoiGianHetHanGiu) FROM GHE_SUATCHIEU h
                        WHERE h.MaGiaoDich=w.MaGiaoDich AND h.TrangThai='Đang giữ') AS HoldEnd
                FROM WEB_CUSTOMER_SELECTION w
                JOIN PHIM p ON p.MaPhim=w.MaPhim
                JOIN PHONGCHIEU r ON r.MaPhong=w.MaPhong
                LEFT JOIN SUATCHIEU s ON s.MaSuat=w.MaSuat
                LEFT JOIN GIAODICH gd ON gd.MaGiaoDich=w.MaGiaoDich
                WHERE w.Reference=?
                  AND (w.ExpiresAt>? OR gd.TrangThai IN ('Chờ thanh toán','Đang thanh toán','Đã thanh toán'))
                """, normalized, now));
        if (slot == null) return null;
        String orderState = slot.get("OrderState") == null ? null : slot.get("OrderState").toString();
        String holdEnd = slot.get("HoldEnd") == null ? null : slot.get("HoldEnd").toString();
        boolean paid = "Đã thanh toán".equals(orderState);
        boolean claimed = paid || "Đang thanh toán".equals(orderState)
                || ("Chờ thanh toán".equals(orderState) && holdEnd != null && holdEnd.compareTo(now) > 0);
        if (!claimed && slot.get("ExpiresAt").toString().compareTo(now) <= 0) return null;
        boolean showChanged = !paid && (
                slot.get("LiveShowState") == null
                || "Đã hủy".equals(slot.get("LiveShowState"))
                || !Objects.equals(slot.get("MaPhim"), slot.get("LiveFilm"))
                || !Objects.equals(slot.get("MaPhong"), slot.get("LiveRoom"))
                || !Objects.equals(slot.get("ThoiGianBatDau"), slot.get("LiveStart"))
                || !"Đang hoạt động".equals(slot.get("RoomState"))
                || !Set.of("Sắp chiếu", "Đang chiếu").contains(slot.get("FilmState"))
        );
        var seats = db.query("""
                SELECT MaGhe,Cho,TenLoaiGhe,GiaVe FROM WEB_CUSTOMER_SELECTION_SEAT
                WHERE Reference=? ORDER BY MaGhe
                """, normalized);
        return new Selection(normalized, slot, seats,
                seats.stream().mapToLong(seat -> ((Number) seat.get("GiaVe")).longValue()).sum(),
                LocalDateTime.parse(slot.get("ExpiresAt").toString(), DB_TIME), claimed, paid, showChanged);
    }
}
