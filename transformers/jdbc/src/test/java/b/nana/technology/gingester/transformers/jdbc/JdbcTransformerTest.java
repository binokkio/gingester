package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.GingesterConfiguration;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcTransformerTest {

    @Test
    void test() throws IOException {

        Path tempFile = Files.createTempFile("gingester-", ".sqlite3");

        GingesterConfiguration configuration = GingesterConfiguration.fromJson(getClass().getResourceAsStream("/test.gin.json"));
        ((ObjectNode) configuration.transformers.get(2).getParameters().orElseThrow()).put("url", "jdbc:sqlite:" + tempFile);

        Gingester writer = new Gingester();
        configuration.applyTo(writer);
        writer.run();

        AtomicReference<Map<String, Object>> result = new AtomicReference<>();
        Gingester reader = new Gingester();
        reader.cli("-t Jdbc.Dql \"{url:'jdbc:sqlite:" + tempFile + "',dql:'SELECT * FROM test'}\"");
        reader.add(result::set);
        reader.run();

        Map<String, Object> container = (Map<String, Object>) result.get().get("test");
        assertEquals(123, container.get("a"));
        assertEquals("Hello, World!", container.get("b"));
        assertEquals(true, container.get("c"));

        Files.delete(tempFile);
    }
}