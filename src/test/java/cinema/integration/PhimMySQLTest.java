package cinema.integration;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.film.*;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests", matches="true")
class PhimMySQLTest {
    @TempDir Path temp;
    private final PhimDAO dao = new PhimDAO();
    private PosterStore posters;
    private PhimBUS admin;
    private Phim film;
    private Path image;
    private List<TheLoai> genres;
    private final int fixtureId = 100000 + ThreadLocalRandom.current().nextInt(1_000_000_000);
    private final String room = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
    private boolean scheduleFixture;

    @BeforeEach void setup() throws Exception {
        posters = new PosterStore(temp.resolve("posters"));
        admin = new PhimBUS(new PhienDangNhap(1, "test_admin", VaiTro.ADMIN, null), dao, new TheLoaiDAO(), posters);
        genres = admin.genres(); assertTrue(genres.size() >= 2);
        image = temp.resolve("input.png");
        ImageIO.write(new BufferedImage(40, 60, BufferedImage.TYPE_INT_RGB), "png", image.toFile());
        film = admin.create(draft("Kiểm thử %_ " + room, 100, List.of(genres.get(0), genres.get(1))), image);
    }

    @AfterEach void cleanup() throws Exception {
        if (film == null) return;
        try (Connection c = Database.connect()) {
            for (String table : new String[]{"SUATCHIEU", "PHIM_THELOAI", "PHIM"}) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE MaPhim=?")) {
                    ps.setString(1, film.maPhim()); ps.executeUpdate();
                }
            }
            if (scheduleFixture) {
                execute(c, "DELETE FROM PHONGCHIEU WHERE MaPhong=?", room);
                execute(c, "DELETE FROM LOAIPHONG WHERE MaLoaiPhong=?", fixtureId);
                execute(c, "DELETE FROM LOAITHOIDIEM WHERE MaLoaiThoiDiem=?", fixtureId);
            }
        }
    }

    private Phim draft(String name, int duration, List<TheLoai> selected) {
        return new Phim(null, name, duration, "Đạo diễn thử", LocalDate.of(2026, 11, 1), 13, null, "Đang chiếu", "2D", selected);
    }

    @Test void searchesNameGenreStatusWithoutDuplicateFilms() throws Exception {
        List<Phim> found = admin.search(film.tenPhim(), genres.get(0).maTheLoai(), "Đang chiếu");
        assertEquals(1, found.size()); assertEquals(2, found.get(0).theLoai().size());
        assertTrue(admin.search(film.tenPhim(), null, "Ngừng chiếu").isEmpty());
        assertTrue(admin.search("' OR 1=1 --", null, null).isEmpty());
        assertEquals(film.tenPhim(), admin.detail(film.maPhim()).tenPhim());
        assertTrue(admin.search("", null, null).stream().anyMatch(p -> p.maPhim().equals(film.maPhim())));
    }

    @Test void updatesNameOnlyAndRejectsBlankName() throws Exception {
        admin.rename(film.maPhim(), "Tên mới");
        Phim current = admin.detail(film.maPhim());
        assertEquals("Tên mới", current.tenPhim());
        assertEquals(film.withTheLoai(current.theLoai()).poster(), current.poster());
        assertEquals(film.thoiLuong(), current.thoiLuong()); assertEquals(film.ngayKhoiChieu(), current.ngayKhoiChieu());
        assertEquals(film.theLoai(), current.theLoai());
        assertThrows(IllegalArgumentException.class, () -> admin.rename(film.maPhim(), " "));
        assertEquals("Tên mới", admin.detail(film.maPhim()).tenPhim());
    }

    @Test void updatesGenresAtomicallyAndRestoresOldLinksWhenFkFails() throws Exception {
        admin.updateGenres(film.maPhim(), List.of(genres.get(0)));
        assertEquals(1, admin.detail(film.maPhim()).theLoai().size());
        assertThrows(SQLException.class, () -> admin.updateGenres(film.maPhim(),
                List.of(genres.get(1), new TheLoai(Integer.MAX_VALUE, "missing"))));
        assertEquals(List.of(genres.get(0)), admin.detail(film.maPhim()).theLoai());
        assertThrows(IllegalArgumentException.class, () -> admin.updateGenres(film.maPhim(), List.of()));
        assertEquals(film.tenPhim(), admin.detail(film.maPhim()).tenPhim());
    }

    @Test void copiesNewPosterAndKeepsPreviousValueWhenImageFails() throws Exception {
        admin.updatePoster(film.maPhim(), image);
        String current = admin.detail(film.maPhim()).poster();
        assertNotEquals(film.poster(), current);
        Files.delete(image);
        assertEquals(40, posters.load(current).getWidth());
        Path invalid = temp.resolve("fake.png"); Files.writeString(invalid, "bad");
        assertThrows(IllegalArgumentException.class, () -> admin.updatePoster(film.maPhim(), invalid));
        assertEquals(current, admin.detail(film.maPhim()).poster());
    }

    @Test void failedCreateRollsBackFilmGenresAndImportedPoster() throws Exception {
        long before;
        try (var files = Files.list(temp.resolve("posters"))) { before = files.count(); }
        String name = "rollback_" + room;
        assertThrows(SQLException.class, () -> admin.create(draft(name, 90,
                List.of(genres.get(0), new TheLoai(Integer.MAX_VALUE, "missing"))), image));
        assertTrue(admin.search(name, null, null).isEmpty());
        try (var files = Files.list(temp.resolve("posters"))) { assertEquals(before, files.count()); }
    }

    @Test void validatesCreateBeforePersistingAnything() {
        assertThrows(IllegalArgumentException.class, () -> admin.create(draft("Name", 0, genres), image));
        assertThrows(IllegalArgumentException.class, () -> admin.create(draft(" ", 90, genres), image));
        assertThrows(IllegalArgumentException.class, () -> admin.create(draft("Name", 90, List.of()), image));
    }

    @Test void staffCanSearchAndViewButCannotModify() throws Exception {
        PhimBUS staff = new PhimBUS(new PhienDangNhap(2, "test_staff", VaiTro.NHAN_VIEN, null), dao, new TheLoaiDAO(), posters);
        assertEquals(film.maPhim(), staff.detail(film.maPhim()).maPhim());
        assertFalse(staff.search(film.tenPhim(), null, null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> staff.create(draft("Name", 90, genres), image));
        assertThrows(IllegalArgumentException.class, () -> staff.rename(film.maPhim(), "Name"));
        assertThrows(IllegalArgumentException.class, () -> staff.updateGenres(film.maPhim(), genres));
        assertThrows(IllegalArgumentException.class, () -> staff.updatePoster(film.maPhim(), image));
        assertThrows(IllegalArgumentException.class, () -> staff.stop(film.maPhim()));
    }

    private void prepareSchedule() throws Exception {
        if (scheduleFixture) return;
        try (Connection c = Database.connect()) {
            execute(c, "INSERT INTO LOAIPHONG VALUES (?, ?, '2D')", fixtureId, room);
            execute(c, "INSERT INTO PHONGCHIEU VALUES (?, ?, ?, 15, 'Đang hoạt động')", room, fixtureId, room);
            execute(c, "INSERT INTO LOAITHOIDIEM VALUES (?, ?)", fixtureId, room);
        }
        scheduleFixture = true;
    }

    private void addSchedule(Connection c, int startMinutes, int endMinutes, String state) throws SQLException {
        execute(c, "INSERT INTO SUATCHIEU (MaPhim, MaPhong, MaLoaiThoiDiem, ThoiGianBatDau, ThoiGianTraPhong, TrangThai) "
                + "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE), DATE_ADD(NOW(), INTERVAL ? MINUTE), ?)",
                film.maPhim(), room, fixtureId, startMinutes, endMinutes, state);
    }

    @Test void rejectsStoppingWithOngoingOrFutureShow() throws Exception {
        prepareSchedule();
        try (Connection c = Database.connect()) { addSchedule(c, -30, 90, "Đang chiếu"); addSchedule(c, 180, 300, "Sắp chiếu"); }
        assertEquals("Phim vẫn còn suất chiếu chưa kết thúc", assertThrows(IllegalArgumentException.class,
                () -> admin.stop(film.maPhim())).getMessage());
        assertEquals("Đang chiếu", admin.detail(film.maPhim()).trangThai());
    }

    @Test void allowsStopWithEndedOrCancelledShowsAndPreservesHistory() throws Exception {
        prepareSchedule();
        try (Connection c = Database.connect()) { addSchedule(c, -180, -30, "Đã kết thúc"); addSchedule(c, 60, 180, "Đã hủy"); }
        admin.stop(film.maPhim());
        assertEquals("Ngừng chiếu", admin.detail(film.maPhim()).trangThai());
        assertEquals(2, admin.detail(film.maPhim()).theLoai().size());
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM SUATCHIEU WHERE MaPhim=?")) {
            ps.setString(1, film.maPhim());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); assertEquals(2, rs.getInt(1)); }
            assertThrows(SQLException.class, () -> addSchedule(c, 300, 420, "Sắp chiếu"));
        }
        assertThrows(IllegalArgumentException.class, () -> admin.stop(film.maPhim()));
    }

    @Test void waitsForConcurrentScheduleInsertAndThenRejectsStop() throws Exception {
        prepareSchedule();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            addSchedule(c, 60, 180, "Sắp chiếu");
            Future<?> stop = pool.submit(() -> { admin.stop(film.maPhim()); return null; });
            c.commit();
            ExecutionException failure = assertThrows(ExecutionException.class, () -> stop.get(10, TimeUnit.SECONDS));
            assertInstanceOf(IllegalArgumentException.class, failure.getCause());
            assertEquals("Đang chiếu", admin.detail(film.maPhim()).trangThai());
        } finally { pool.shutdownNow(); }
    }

    private static void execute(Connection c, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) ps.setObject(i + 1, parameters[i]);
            ps.executeUpdate();
        }
    }
}
