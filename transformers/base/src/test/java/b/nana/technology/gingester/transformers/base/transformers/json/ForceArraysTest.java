package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceArraysTest {

    @Test
    void test() {

        ArrayDeque<JsonNode> results = new ArrayDeque<>();

        Gingester gingester = new Gingester();

        gingester.cli("-t Resource.Open /b/nana/technology/gingester/transformers/base/transformers/xml/arrays-issue.xml");
        gingester.add("Xml.ToJson");
        gingester.cli("-t Json.Path $.record[*]");
        gingester.cli("-t Json.ForceArrays [\"$.container.list.item\"]");
        gingester.add(results::add);

        gingester.run();

        assertEquals(results.size(), 3);
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
    }
}