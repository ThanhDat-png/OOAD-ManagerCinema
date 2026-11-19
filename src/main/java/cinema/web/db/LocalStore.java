package cinema.web.db;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LocalStore implements WebDb.Store {
    private final String jdbcUrl;

    LocalStore(Path path) {
        Path file = path.toAbsolutePath().normalize();
        try {
            Files.createDirectories(file.getParent());
            try {
                Files.createFile(file);
            } catch (FileAlreadyExistsException ignored) {
                // SQLite opens the same initialized path when startup races another instance.
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create SQLite database path", e);
        }
        jdbcUrl = "jdbc:sqlite:" + file;
    }

    public WebDb.Stream open() {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            try (Statement config = connection.createStatement()) {
                config.execute("PRAGMA busy_timeout = 5000");
                config.execute("PRAGMA foreign_keys = ON");
            } catch (SQLException e) {
                connection.close();
                throw e;
            }
            return new Session(connection);
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    private record Session(Connection connection) implements WebDb.Stream {
        public List<Map<String,Object>> query(String sql, Object... args) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                bind(stmt, args);
                try (ResultSet rows = stmt.executeQuery()) {
                    List<Map<String,Object>> out = new ArrayList<>();
                    int columns = rows.getMetaData().getColumnCount();
                    while (rows.next()) {
                        Map<String,Object> row = new LinkedHashMap<>();
                        for (int i=1; i<=columns; i++)
                            row.put(rows.getMetaData().getColumnLabel(i), rows.getObject(i));
                        out.add(row);
                    }
                    return out;
                }
            } catch (SQLException e) {
                throw failure(e);
            }
        }

        public int update(String sql, Object... args) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                bind(stmt, args);
                return stmt.executeUpdate();
            } catch (SQLException e) {
                throw failure(e);
            }
        }

        public void close() {
            try {
                connection.close();
            } catch (SQLException e) {
                throw failure(e);
            }
        }

        private static void bind(PreparedStatement stmt, Object[] args) throws SQLException {
            for (int i=0; i<args.length; i++) {
                Object value = args[i];
                if (value instanceof byte[] bytes) stmt.setBytes(i+1, bytes);
                else stmt.setObject(i+1, value);
            }
        }
    }

    private static RuntimeException failure(SQLException error) {
        if ((error.getErrorCode() & 0xff) == 19)
            return new IllegalArgumentException("Dữ liệu xung đột với thông tin đã lưu", error);
        return new IllegalStateException("Không thể truy cập cơ sở dữ liệu", error);
    }
}
