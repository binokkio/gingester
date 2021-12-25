package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceArraysTest {

    @Test
    void test() {

        ArrayDeque<JsonNode> results = new ArrayDeque<>();

        Gingester gingester = new Gingester("" +
                "-t ResourceOpen /data/xml/arrays-issue.xml " +
                "-t XmlToJson " +
                "-t JsonPath $.record[*] " +
                "-t JsonForceArrays [\"$.container.list.item\"]");

        gingester.attach(results::add);

        gingester.run();

        assertEquals(results.size(), 3);
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
    }

    @Test
    void testForceArraysOnRoot() {

        Gingester gingester = new Gingester("" +
                "-t JsonCreate {hello:123} " +
                "-t JsonForceArrays $");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        gingester.attach(result::set);
        gingester.run();

        assertEquals(123, result.get().get(0).get("hello").intValue());
    }
}