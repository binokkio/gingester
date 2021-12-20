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

        Gingester gingester = new Gingester();

        gingester.cli("-t ResourceOpen /b/nana/technology/gingester/transformers/base/transformers/xml/arrays-issue.xml");
        gingester.add("XmlToJson");
        gingester.cli("-t JsonPath $.record[*]");
        gingester.cli("-t JsonForceArrays [\"$.container.list.item\"]");
        gingester.add(results::add);

        gingester.run();

        assertEquals(results.size(), 3);
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
    }

    @Test
    void testForceArraysOnRoot() {

        Gingester gingester = new Gingester();
        gingester.cli("" +
                "-t JsonCreate {hello:123} " +
                "-t JsonForceArrays $");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        gingester.add(result::set);
        gingester.run();

        assertEquals(123, result.get().get(0).get("hello").intValue());
    }
}