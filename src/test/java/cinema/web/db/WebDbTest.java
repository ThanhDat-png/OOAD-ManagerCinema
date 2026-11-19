package cinema.web.db;

import cinema.controller.auth.PasswordHasher;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

final class WebDbTest {
    @TempDir Path directory;

    @Test void persistentDemoAccountsSeatMapsAndBoundParameters() {
        MockEnvironment env = env(true);
        WebDb db = new WebDb(env);
        Map<String,Object> admin = db.query("SELECT TenDangNhap, MatKhauHash FROM TAIKHOAN WHERE MaTK=?", 1).get(0);
        assertEquals("admin_demo", admin.get("TenDangNhap"));
        assertTrue(PasswordHasher.verify("Cinema@123".toCharArray(), (String) admin.get("MatKhauHash")));
        assertFalse(db.query("SELECT 1 FROM SUATCHIEU WHERE ThoiGianBatDau>datetime('now','+7 hours')").isEmpty());
        assertFalse(db.query("SELECT 1 FROM GHE_SUATCHIEU WHERE TrangThai='Trống'").isEmpty());
        assertTrue(db.query("PRAGMA foreign_key_check").isEmpty());
        assertEquals(3, db.query("SELECT MaPhim FROM PHIM JOIN WEB_POSTER ON WEB_POSTER.Id=PHIM.Poster WHERE length(Data)>0").size());
        byte[] poster = {0, (byte) 250, 6};
        db.update("INSERT INTO WEB_POSTER VALUES (?,?,?)","p","image/png",poster);
        assertArrayEquals(poster,(byte[])db.query("SELECT Data FROM WEB_POSTER").get(0).get("Data"));
        db.update("UPDATE NHANVIEN SET HoTen=? WHERE MaNV=?", "O'Brien", "NV001");
        WebDb restarted = new WebDb(env);
        assertEquals("O'Brien", restarted.query("SELECT HoTen FROM NHANVIEN WHERE MaNV='NV001'").get(0).get("HoTen"));
    }

    @Test void atomicRollbackAndConcurrentMigration() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            var tasks = java.util.stream.IntStream.range(0,2).mapToObj(i -> pool.submit(() -> {
                start.await();
                return new WebDb(env(false));
            })).toList();
            start.countDown();
            WebDb db = tasks.get(0).get(2, TimeUnit.MINUTES);
            tasks.get(1).get(2, TimeUnit.MINUTES);
            assertEquals(1, db.query("SELECT Name FROM WEB_MIGRATION").size());
            assertTrue(db.query("SELECT MaTK FROM TAIKHOAN").isEmpty());
            assertThrows(IllegalArgumentException.class, () -> db.write(tx -> {
                tx.update("INSERT INTO NHANVIEN (MaNV,HoTen) VALUES ('x','test')");
                tx.update("INSERT INTO TAIKHOAN (MaVaiTro,TenDangNhap,MatKhauHash) VALUES (999,'x','x')");
                return null;
            }));
            assertEquals(List.of(), db.query("SELECT MaNV FROM NHANVIEN"));
        } finally {
            pool.shutdownNow();
        }
    }

    private MockEnvironment env(boolean demo) {
        return new MockEnvironment().withProperty("CINEMA_SQLITE_PATH",directory.resolve("cinema.sqlite").toString())
                .withProperty("CINEMA_DEMO_DATA",Boolean.toString(demo));
    }
}
