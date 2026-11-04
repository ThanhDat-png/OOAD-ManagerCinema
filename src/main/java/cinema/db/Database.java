package cinema.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private Database() { }

    public static Connection connect() throws SQLException {
        Properties config = new Properties();
        Path path = Path.of("config", "database.properties");
        try (InputStream input = Files.newInputStream(path)) {
            config.load(input);
        } catch (IOException e) {
            throw new SQLException("Chưa đọc được cấu hình kết nối CSDL", e);
        }
        String url = config.getProperty("db.url");
        String user = config.getProperty("db.user");
        if (url == null || user == null || !url.startsWith("jdbc:mysql://")) {
            throw new SQLException("Cấu hình MySQL không hợp lệ");
        }
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(url, user, config.getProperty("db.password", ""));
    }
}
