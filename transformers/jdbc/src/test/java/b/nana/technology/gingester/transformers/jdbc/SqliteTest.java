package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class SqliteTest {

    private static final String AUTO_INCREMENT_KEYWORD = "AUTOINCREMENT";

    @Test
    void testYieldGeneratedKeys() {

        ArrayDeque<Map<String, Integer>> result = new ArrayDeque<>();

        new FlowBuilder()
                .cli(getClass().getResource("/repeated_insert.cli"), Map.of(
                        "autoIncrementKeyword", AUTO_INCREMENT_KEYWORD
                ))
                .add(result::add)
                .run();

        assertEquals(1, result.remove().get("last_insert_rowid()"));
        assertEquals(2, result.remove().get("last_insert_rowid()"));
        assertEquals(3, result.remove().get("last_insert_rowid()"));
    }

    @Test
    void testYieldGeneratedKeysCanBeDisabled() {

        ArrayDeque<JsonNode> result = new ArrayDeque<>();

        new FlowBuilder()
                .cli(getClass().getResource("/repeated_insert.cli"), Map.of(
                        "autoIncrementKeyword", AUTO_INCREMENT_KEYWORD,
                        "yieldGeneratedKeys", false
                ))
                .add(result::add)
                .run();

        assertEquals(123, result.remove().get("a").intValue());
        assertEquals("Hello, World!", result.remove().get("b").textValue());
        assertTrue(result.remove().get("c").booleanValue());
    }

    @Test
    void testChainedInsert() {

        ArrayDeque<Map<String, Map<String, Object>>> result = new ArrayDeque<>();

        new FlowBuilder()
                .cli(getClass().getResource("/repeated_chained_insert.cli"), Map.of(
                        "autoIncrementKeyword", AUTO_INCREMENT_KEYWORD,
                        "generatedKeyOverride", "last_insert_rowid()"
                ))
                .add(result::add)
                .run();

        assertEquals(1, result.getFirst().get("data").get("last_insert_rowid()"));
        assertEquals(1, result.remove().get("refs").get("last_insert_rowid()"));
        assertEquals(2, result.getFirst().get("data").get("last_insert_rowid()"));
        assertEquals(2, result.remove().get("refs").get("last_insert_rowid()"));
        assertEquals(3, result.getFirst().get("data").get("last_insert_rowid()"));
        assertEquals(3, result.remove().get("refs").get("last_insert_rowid()"));
    }

    @Test
    void testLoadJson_newTable() {

        Queue<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-sft ResourceOpen /records.json " +
                "-t JsonStream $[*] " +
                "-t JdbcLoadJson '{table:\"testing\"}' " +
                "-stt OnFinish " +
                "-t JdbcDql '{dql: \"SELECT * FROM testing\", columnsOnly: true}' " +
                "-t ObjectToString")
                .add(results::add)
                .run();

        assertEquals(2, results.size());
        assertEquals("{hello=world, int=123, float=234.567, boolean=true, bye=null}", results.remove());
        assertEquals("{hello=null, int=234, float=345.0, boolean=false, bye=world}", results.remove());
    }

    @Test
    void testLoadJson_existingTable() {

        Queue<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t JdbcDml '{ddl:\"CREATE TABLE testing (foo TEXT)\",dml:[]}' " +
                "-sft ResourceOpen /records.json " +
                "-t JsonStream $[*] " +
                "-t JdbcLoadJson '{table:\"testing\"}' " +
                "-stt OnFinish " +
                "-t JdbcDql '{dql: \"SELECT * FROM testing\", columnsOnly: true}' " +
                "-t ObjectToString")
                .add(results::add)
                .run();

        assertEquals(2, results.size());
        assertEquals("{foo=null, hello=world, int=123, float=234.567, boolean=true, bye=null}", results.remove());
        assertEquals("{foo=null, hello=null, int=234, float=345.0, boolean=false, bye=world}", results.remove());
    }
}
