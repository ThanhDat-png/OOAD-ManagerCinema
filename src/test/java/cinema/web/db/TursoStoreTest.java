package cinema.web.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TursoStoreTest {
    @Test void pipelineCarriesNewBatonAndRoundTripsBinaryAndIntegerTypes() throws IOException {
        ObjectMapper json = new ObjectMapper();
        List<JsonNode> bodies = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        String url = "http://127.0.0.1:"+server.getAddress().getPort();
        server.createContext("/v2/pipeline", exchange -> {
            try (exchange) {
                JsonNode request = json.readTree(exchange.getRequestBody());
                bodies.add(request);
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                var response = json.createObjectNode().put("base_url",url);
                var results = response.putArray("results");
                for (var op : request.get("requests")) {
                    var result = results.addObject().put("type","ok").putObject("response");
                    if (op.path("type").asText().equals("close")) {
                        result.put("type","close");
                        response.putNull("baton");
                    } else {
                        response.put("baton","b"+bodies.size());
                        var statement = result.put("type","execute").putObject("result");
                        statement.put("affected_row_count",1);
                        var cols = statement.putArray("cols");
                        var rows = statement.putArray("rows");
                        if (op.path("stmt").path("sql").asText().startsWith("SELECT")) {
                            for (String name : List.of("Id","Data","Empty")) cols.addObject().put("name",name);
                            var row = rows.addArray();
                            row.addObject().put("type","integer").put("value","9007199254740993");
                            row.addObject().put("type","blob").put("base64","AP8=");
                            row.addObject().put("type","null");
                        }
                    }
                }
                byte[] bytes = json.writeValueAsBytes(response);
                exchange.sendResponseHeaders(200,bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try {
            TursoStore store = new TursoStore(url,"test-token");
            try (var stream = store.open()) {
                stream.update("BEGIN IMMEDIATE");
                stream.update("INSERT INTO test VALUES (?,?,?)",null,9007199254740993L,new byte[]{0,(byte)255});
                Map<String,Object> row = stream.query("SELECT * FROM test").get(0);
                assertEquals(9007199254740993L,row.get("Id"));
                assertArrayEquals(new byte[]{0,(byte)255},(byte[])row.get("Data"));
                assertNull(row.get("Empty"));
                stream.update("COMMIT");
            }
            assertEquals("Bearer test-token",authorization.get());
            assertEquals(4,bodies.size());
            assertEquals("b1",bodies.get(1).path("baton").textValue());
            assertEquals("b2",bodies.get(2).path("baton").textValue());
            assertEquals("b3",bodies.get(3).path("baton").textValue());
            assertEquals("close",bodies.get(3).at("/requests/1/type").textValue());
            assertEquals("null",bodies.get(1).at("/requests/0/stmt/args/0/type").textValue());
            assertEquals("9007199254740993",bodies.get(1).at("/requests/0/stmt/args/1/value").textValue());
            assertEquals("AP8=",bodies.get(1).at("/requests/0/stmt/args/2/base64").textValue());
            assertEquals(1,store.update("UPDATE test SET y=0"));
            assertEquals("close",bodies.get(4).at("/requests/2/type").textValue());
        } finally {
            server.stop(0);
        }
    }
}
