package cinema.web.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hrana's pipeline protocol. Each interactive transaction owns its baton and routing URL. */
final class TursoStore implements WebDb.Store {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String,String> CLOSE = Map.of("type", "close");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private final URI base;
    private final String token;

    TursoStore(String databaseUrl, String token) {
        base = URI.create(databaseUrl.replaceFirst("^(libsql|turso)://", "https://"));
        String host = base.getHost();
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host);
        if (host == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                || (!"https".equals(base.getScheme()) && !(local && "http".equals(base.getScheme()))))
            throw new IllegalArgumentException("Invalid Turso database URL");
        this.token = token;
    }

    public WebDb.Stream open() { return new Session(); }

    public List<Map<String,Object>> query(String sql, Object... args) {
        return rows(new Session().execute(sql, args, true));
    }

    public int update(String sql, Object... args) {
        return new Session().execute(sql, args, true).path("affected_row_count").asInt();
    }

    private final class Session implements WebDb.Stream {
        private URI endpoint = base.resolve("/v2/pipeline");
        private String baton;
        private boolean closed;

        public List<Map<String,Object>> query(String sql, Object... args) {
            return rows(execute(sql, args, false));
        }

        public int update(String sql, Object... args) {
            boolean finish = sql.equals("COMMIT") || sql.equals("ROLLBACK");
            return execute(sql, args, finish).path("affected_row_count").asInt();
        }

        JsonNode execute(String sql, Object[] args, boolean finish) {
            List<Object> requests = new ArrayList<>();
            if (baton == null) requests.add(statement("PRAGMA foreign_keys = ON", new Object[0]));
            int resultIndex = requests.size();
            requests.add(statement(sql, args));
            if (finish) requests.add(CLOSE);
            return send(requests, finish).get(resultIndex).path("response").path("result");
        }

        // Schema/data scripts are one round trip so they fit the hosted transaction window.
        public void batch(List<String> statements) {
            List<Object> requests = new ArrayList<>();
            statements.forEach(sql -> requests.add(statement(sql, new Object[0])));
            send(requests, false);
        }

        private JsonNode send(List<Object> requests, boolean finish) {
            if (closed) throw new IllegalStateException("Database stream is closed");
            Map<String,Object> body = new LinkedHashMap<>();
            if (baton != null) body.put("baton", baton);
            body.put("requests", requests);
            try {
                HttpRequest req = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(12))
                        .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(JSON.writeValueAsBytes(body))).build();
                HttpResponse<byte[]> response = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200)
                    throw new IllegalStateException("Turso database returned HTTP " + response.statusCode());
                JsonNode message = JSON.readTree(response.body());
                baton = message.path("baton").asText(null);
                closed = finish || baton == null;
                if (!finish && baton == null) throw new IllegalStateException("Turso did not return an open stream");
                if (message.path("base_url").isTextual())
                    endpoint = routingUrl(message.get("base_url").textValue());
                JsonNode results = message.path("results");
                if (!results.isArray() || results.size() != requests.size())
                    throw new IllegalStateException("Invalid Turso pipeline response");
                for (JsonNode result : results) {
                    if ("ok".equals(result.path("type").asText())) continue;
                    String code = result.path("error").path("code").asText("UNKNOWN");
                    if (code.startsWith("SQLITE_CONSTRAINT"))
                        throw new IllegalArgumentException("Dữ liệu xung đột với thông tin đã lưu");
                    throw new IllegalStateException("Turso database error " + code);
                }
                return results;
            } catch (IOException e) {
                throw new IllegalStateException("Không thể kết nối Turso", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Turso request interrupted", e);
            }
        }

        public void close() {
            if (!closed && baton != null) send(List.of(CLOSE), true);
        }
    }

    private URI routingUrl(String value) {
        URI route = URI.create(value);
        String host = route.getHost();
        boolean same = base.getHost().equalsIgnoreCase(host) && base.getPort() == route.getPort();
        boolean turso = base.getHost().endsWith(".turso.io") && host != null && host.endsWith(".turso.io");
        if ((!same && !turso) || !base.getScheme().equals(route.getScheme()) || route.getUserInfo() != null)
            throw new IllegalStateException("Invalid Turso routing URL");
        return route.resolve("/v2/pipeline");
    }

    private static Map<String,Object> statement(String sql, Object[] args) {
        return Map.of("type","execute","stmt",Map.of("sql",sql,"args",Arrays.stream(args).map(TursoStore::encode).toList()));
    }

    private static Map<String,Object> encode(Object value) {
        if (value == null) return Map.of("type","null");
        if (value instanceof byte[] bytes)
            return Map.of("type","blob","base64",Base64.getEncoder().encodeToString(bytes));
        if (value instanceof Boolean bool) return Map.of("type","integer","value",bool ? "1" : "0");
        if (value instanceof Float || value instanceof Double)
            return Map.of("type","float","value",value);
        if (value instanceof BigDecimal amount)
            return Map.of("type","integer","value",Long.toString(amount.longValueExact()));
        if (value instanceof BigInteger integer)
            return Map.of("type","integer","value",Long.toString(integer.longValueExact()));
        if (value instanceof Number number) return Map.of("type","integer","value",Long.toString(number.longValue()));
        return Map.of("type","text","value",value.toString());
    }

    private static List<Map<String,Object>> rows(JsonNode result) {
        List<Map<String,Object>> output = new ArrayList<>();
        JsonNode cols = result.path("cols");
        for (JsonNode row : result.path("rows")) {
            Map<String,Object> mapped = new LinkedHashMap<>();
            for (int i=0; i<cols.size(); i++) {
                JsonNode cell = row.get(i);
                Object value = switch (cell.path("type").asText()) {
                    case "null" -> null;
                    case "integer" -> Long.parseLong(cell.get("value").asText());
                    case "float" -> cell.get("value").doubleValue();
                    case "blob" -> Base64.getDecoder().decode(cell.get("base64").asText());
                    case "text" -> cell.get("value").textValue();
                    default -> throw new IllegalStateException("Unsupported Turso row value");
                };
                mapped.put(cols.get(i).get("name").asText(), value);
            }
            output.add(mapped);
        }
        return output;
    }
}
