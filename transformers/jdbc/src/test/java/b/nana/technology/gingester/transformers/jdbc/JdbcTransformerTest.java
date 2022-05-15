package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTransformerTest {

    @Test
    void test() throws IOException {

        Path tempFile = Files.createTempFile("gingester-", ".sqlite3");
        new Gingester().cli("-cr /test.cli {url:'jdbc:sqlite:" + tempFile + "'}").run();

        AtomicReference<Map<String, Map<String, ?>>> result = new AtomicReference<>();
        Gingester reader = new Gingester().cli("-t JdbcDql \"{url:'jdbc:sqlite:" + tempFile + "',dql:'SELECT * FROM test'}\"");
        reader.attach(result::set);
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

        Gingester gingester = new Gingester().cli("" +
                "-t JsonCreate \"{a:123,b:true,c:'Hello, World!'}\" " +
                "-s in " +
                "-t JdbcDml \"{" +
                "   ddl:'CREATE TABLE test (a INTEGER, b BOOLEAN, c TEXT)'," +
                "   dml:{statement:'INSERT INTO test VALUES (?, ?, ?)',parameters:['in.a','in.b','in.c']}" +
                "}\" " +
                "-t Repeat 3 " +
                "-t JdbcDql 'SELECT *, a * 2 as a2, a * 3 as \"test.a3\" FROM test'");

        gingester.attach(result::set);

        gingester.run();

        assertEquals(246, result.get().get("__calculated__").get("a2"));
        assertEquals(123, result.get().get("test").get("a"));
        assertEquals(true, result.get().get("test").get("b"));
        assertEquals("Hello, World!", result.get().get("test").get("c"));
        assertEquals(369, result.get().get("test").get("a3"));
    }

    @Test
    void testJdbcTables() {

        Gingester gingester = new Gingester().cli("" +
                "-t JdbcTables \"{ddl:['CREATE TABLE hello (one INT)','CREATE TABLE world (two INT)']}\"");

        ArrayDeque<String> tableNames = new ArrayDeque<>();
        gingester.attach(tableNames::add).run();

        assertEquals(2, tableNames.size());
        assertTrue(tableNames.contains("hello"));
        assertTrue(tableNames.contains("world"));
    }

    @Test
    void testTemplating() throws IOException {

        Path tempFile = Files.createTempFile("gingester-", ".sqlite3");
        new Gingester().cli("-cr /test.cli {url:'jdbc:sqlite:" + tempFile + "'}").run();

        Gingester gingester = new Gingester().cli("" +
                "-t Repeat 3 " +
                "-t JdbcTables {url:'[=url]'} " +
                "-t JdbcDql {url:'[=url]',dql:{template:'/test.sql',is:'RESOURCE'}} " +
                "-t ObjectToJson",
                Map.of("url", "jdbc:sqlite:" + tempFile));

        ArrayDeque<JsonNode> results = new ArrayDeque<>();
        gingester.attach(results::add).run();

        assertEquals(3, results.size());
        assertEquals("{\"test\":{\"a\":123,\"b\":\"Hello, World!\",\"c\":true}}", results.remove().toString());
        assertEquals("{\"test\":{\"a\":123,\"b\":\"Hello, World!\",\"c\":true}}", results.remove().toString());
        assertEquals("{\"test\":{\"a\":123,\"b\":\"Hello, World!\",\"c\":true}}", results.remove().toString());
    }
}