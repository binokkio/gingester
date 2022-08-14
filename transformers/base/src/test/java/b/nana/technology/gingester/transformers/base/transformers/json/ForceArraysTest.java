package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ForceArraysTest {

    @Test
    void test() {

        ArrayDeque<JsonNode> results = new ArrayDeque<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t ResourceOpen /data/xml/arrays-issue.xml " +
                "-t XmlToJson " +
                "-t JsonPath $.record[*] " +
                "-t JsonForceArrays [\"$.container.list.item\"]");

        flowBuilder.attach(results::add);

        flowBuilder.run();

        assertEquals(results.size(), 3);
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
    }

    @Test
    void testForceArraysOnRoot() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t JsonDef {hello:123} " +
                "-t JsonForceArrays $");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        flowBuilder.attach(result::set);
        flowBuilder.run();

        assertEquals(123, result.get().get(0).get("hello").intValue());
    }
}