package cinema.web.db;

import cinema.controller.auth.PasswordHasher;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** SQLite SQL and bound parameters shared by the local and hosted web runtimes. */
@Component
public final class WebDb {
    public interface Tx {
        List<Map<String,Object>> query(String sql, Object... args);
        int update(String sql, Object... args);
    }

    interface Stream extends Tx, AutoCloseable {
        default void batch(List<String> statements) {
            statements.forEach(this::update);
        }
        @Override void close();
    }

    interface Store {
        Stream open();
        default List<Map<String,Object>> query(String sql, Object... args) {
            try (Stream stream = open()) { return stream.query(sql, args); }
        }
        default int update(String sql, Object... args) {
            try (Stream stream = open()) { return stream.update(sql, args); }
        }
    }

    private final Store store;

    public WebDb(Environment env) {
        String url = env.getProperty("TURSO_DATABASE_URL", "").trim();
        String token = env.getProperty("TURSO_AUTH_TOKEN", "").trim();
        boolean remote = !url.isEmpty();
        if (remote != !token.isEmpty())
            throw new IllegalStateException("TURSO_DATABASE_URL and TURSO_AUTH_TOKEN must be set together");
        if (!remote && env.containsProperty("RENDER"))
            throw new IllegalStateException("A deployed web instance requires a durable Turso database");
        store = remote ? new TursoStore(url, token)
                : new LocalStore(Path.of(env.getProperty("CINEMA_SQLITE_PATH", "data/cinema-web.sqlite")));
        boolean demo = env.getProperty("CINEMA_DEMO_DATA", Boolean.class, !remote);
        initialize(env, demo, remote);
    }

    public List<Map<String,Object>> query(String sql, Object... args) {
        return store.query(sql, args);
    }

    public int update(String sql, Object... args) {
        return store.update(sql, args);
    }

    public <T> T write(Function<Tx,T> work) {
        try (Stream session = store.open()) {
            session.update("BEGIN IMMEDIATE");
            try {
                T result = work.apply(session);
                session.update("COMMIT");
                return result;
            } catch (RuntimeException | Error failure) {
                try {
                    session.update("ROLLBACK");
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    private void initialize(Environment env, boolean demo, boolean remote) {
        update("CREATE TABLE IF NOT EXISTS WEB_MIGRATION (Name TEXT NOT NULL PRIMARY KEY)");
        apply("001-schema", tx -> tx.batch(statements("001-schema")));
        if (!demo || alreadyApplied("demo-v1")) return;
        String admin = env.getProperty("CINEMA_ADMIN_PASSWORD", remote ? "" : "Cinema@123");
        String staff = env.getProperty("CINEMA_STAFF_PASSWORD", remote ? "" : "Cinema@123");
        if (remote && (admin.isBlank() || staff.isBlank() || admin.equals(staff)
                || admin.equals("Cinema@123") || staff.equals("Cinema@123")))
            throw new IllegalStateException("Remote demo requires distinct CINEMA_ADMIN_PASSWORD and CINEMA_STAFF_PASSWORD");
        // PBKDF2 takes place before BEGIN, outside the hosted transaction deadline.
        String adminHash = hash(admin);
        String staffHash = hash(staff);
        List<DemoPoster> posters = demoPosters();
        apply("demo-v1", tx -> {
            tx.batch(statements("demo"));
            tx.update("""
                    INSERT INTO TAIKHOAN (MaTK,MaVaiTro,MaNV,TenDangNhap,MatKhauHash,TrangThai) VALUES
                    (1,1,'NV001','admin_demo',?,'Đang hoạt động'),
                    (2,2,'NV002','staff_demo',?,'Đang hoạt động'),
                    (3,2,'NV003','locked_demo',?,'Bị khóa'),
                    (4,2,'NV004','inactive_demo',?,'Ngừng hoạt động')
                    """, adminHash, staffHash, staffHash, staffHash);
            if (!posters.isEmpty()) {
                String values = String.join(",", java.util.Collections.nCopies(posters.size(), "(?,'image/jpeg',?)"));
                Object[] args = posters.stream().flatMap(p -> java.util.stream.Stream.of(p.film(), p.bytes())).toArray();
                tx.update("INSERT INTO WEB_POSTER (Id,Mime,Data) VALUES " + values, args);
                tx.update("UPDATE PHIM SET Poster=MaPhim WHERE MaPhim IN (SELECT Id FROM WEB_POSTER)");
            }
        });
    }

    private record DemoPoster(String film, byte[] bytes) {}

    private static List<DemoPoster> demoPosters() {
        var posters = new java.util.ArrayList<DemoPoster>();
        for (String film : List.of("PH001", "PH002", "PH003")) {
            try (InputStream in = WebDb.class.getResourceAsStream("/demo-posters/" + film + ".jpg")) {
                if (in != null) posters.add(new DemoPoster(film, in.readAllBytes()));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read demo poster: " + film, e);
            }
        }
        return posters;
    }

    private boolean alreadyApplied(String name) {
        return !query("SELECT 1 FROM WEB_MIGRATION WHERE Name=?", name).isEmpty();
    }

    private void apply(String name, java.util.function.Consumer<Stream> migration) {
        if (alreadyApplied(name)) return;
        write(tx -> {
            if (tx.update("INSERT INTO WEB_MIGRATION (Name) VALUES (?) ON CONFLICT DO NOTHING", name) == 1)
                migration.accept((Stream) tx);
            return null;
        });
    }

    private static String hash(String password) {
        char[] chars = password.toCharArray();
        try {
            return PasswordHasher.hash(chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    // Empty lines delimit statements. This also supports semicolons inside SQLite triggers.
    private static List<String> statements(String resource) {
        try (InputStream in = WebDb.class.getResourceAsStream("/webdb/" + resource + ".sql")) {
            if (in == null) throw new IllegalStateException("Missing database migration: " + resource);
            return Arrays.asList(new String(in.readAllBytes(), StandardCharsets.UTF_8).strip().split("\\R\\s*\\R"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read migration: " + resource, e);
        }
    }
}
