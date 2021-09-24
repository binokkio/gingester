package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceArraysTest {

    @Test
    void test() throws Exception {

        ArrayDeque<JsonNode> results = new ArrayDeque<>();

        Gingester gingester = new Gingester();

        gingester.configure(c -> c
                .transformer("Resource.Open")
                .parameters("/b/nana/technology/gingester/transformers/base/transformers/xml/arrays-issue.xml"));

        gingester.add("Xml.ToJson");

        gingester.configure(c -> c
                .transformer("Json.Path")
                .parameters("$.record[*]"));

        gingester.configure(c -> c
                .transformer("Json.ForceArrays")
                .parameters(Collections.singletonList("$.container.list.item")));

        gingester.add(results::add);

        gingester.run();

        assertEquals(results.size(), 3);
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
        assertTrue(results.remove().get("container").get("list").get("item").isArray());
    }
}