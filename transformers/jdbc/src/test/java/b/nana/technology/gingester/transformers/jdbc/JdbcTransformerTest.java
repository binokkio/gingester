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

        AtomicReference<Map<String, Map<String, ?>>> result = new AtomicReference<>();
        Gingester reader = new Gingester();
        reader.cli("-t JdbcDql \"{url:'jdbc:sqlite:" + tempFile + "',dql:'SELECT * FROM test'}\"");
        reader.add(result::set);
        reader.run();

        Map<String, ?> container = result.get().get("test");
        assertEquals(123, container.get("a"));
        assertEquals("Hello, World!", container.get("b"));
        assertEquals(true, container.get("c"));

        Files.delete(tempFile);
    }

    @Test
    void testValueAndColumnNameInterpretation() {

        AtomicReference<Map<String, Map<String, ?>>> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-t JsonCreate \"{a:123,b:true,c:'Hello, World!'}\" " +
                "-s in " +
                "-t JdbcDml \"{" +
                "   ddl:'CREATE TABLE test (a INTEGER, b BOOLEAN, c TEXT)'," +
                "   dml:{statement:'INSERT INTO test VALUES (?, ?, ?)',parameters:['in.a','in.b','in.c']}" +
                "}\" " +
                "-t JdbcDql 'SELECT *, a * 2 as a2, a * 3 as \"test.a3\" FROM test'");

        gingester.add(result::set);

        gingester.run();

        assertEquals(246, result.get().get("__calculated__").get("a2"));
        assertEquals(123, result.get().get("test").get("a"));
        assertEquals(true, result.get().get("test").get("b"));
        assertEquals("Hello, World!", result.get().get("test").get("c"));
        assertEquals(369, result.get().get("test").get("a3"));
    }
}