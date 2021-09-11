package b.nana.technology.gingester.core.main;

import b.nana.technology.gingester.core.configuration.Configuration;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void testTransformer() {
        Configuration configuration = Main.parseArgs(createArgs("-t Hello"));
        assertEquals("Hello", configuration.transformers.get(0).getTransformer());
        assertEquals(Collections.singletonList("__maybe_next__"), configuration.transformers.get(0).getLinks());
    }

    @Test
    void testBreak() {
        Configuration configuration = Main.parseArgs(createArgs("-t Hello -b -t World"));
        assertEquals(1, configuration.transformers.size());
        assertEquals("Hello", configuration.transformers.get(0).getTransformer());
    }

    @Test
    void testTransformerWithId() {
        Configuration configuration = Main.parseArgs(createArgs("-t HelloId:Hello"));
        assertEquals("Hello", configuration.transformers.get(0).getTransformer());
        assertEquals("HelloId", configuration.transformers.get(0).getId());
        assertEquals(Collections.singletonList("__maybe_next__"), configuration.transformers.get(0).getLinks());
    }

    @Test
    void testTerminalTransformer() {
        Configuration configuration = Main.parseArgs(createArgs("-t Hello --"));
        assertEquals("Hello", configuration.transformers.get(0).getTransformer());
        assertEquals(Collections.emptyList(), configuration.transformers.get(0).getLinks());
    }

    @Test
    void testSyncToTransformer() {
        Configuration configuration = Main.parseArgs(createArgs("-stt Hello"));
        assertEquals("Hello", configuration.transformers.get(0).getTransformer());
        assertEquals(Collections.singletonList("__seed__"), configuration.transformers.get(0).getSyncs());
    }

    @Test
    void testSyncFromTransformer() {
        Configuration configuration = Main.parseArgs(createArgs("-sft Hello -stt World"));
        assertEquals("Hello", configuration.transformers.get(0).getTransformer());
        assertEquals(Collections.emptyList(), configuration.transformers.get(0).getSyncs());
        assertEquals("World", configuration.transformers.get(1).getTransformer());
        assertEquals(Collections.singletonList("Hello"), configuration.transformers.get(1).getSyncs());
    }

    @Test
    void testSyncFromTransformerWithId() {
        Configuration configuration = Main.parseArgs(createArgs("-sft HelloId:Hello -stt World"));
        assertEquals("Hello", configuration.transformers.get(0).getTransformer());
        assertEquals("HelloId", configuration.transformers.get(0).getId());
        assertEquals(Collections.emptyList(), configuration.transformers.get(0).getSyncs());
        assertEquals("World", configuration.transformers.get(1).getTransformer());
        assertEquals(Collections.singletonList("HelloId"), configuration.transformers.get(1).getSyncs());
    }

    @Test
    void testLinks() {
        Configuration configuration = Main.parseArgs(createArgs("-t Hello -l A B C"));
        assertEquals("Hello", configuration.transformers.get(0).getTransformer());
        assertEquals(Arrays.asList("A", "B", "C"), configuration.transformers.get(0).getLinks());
    }

    @Test
    void testFetch() {
        Configuration configuration = Main.parseArgs(createArgs("-f"));
        assertEquals("Fetch", configuration.transformers.get(0).getTransformer());
    }

    @Test
    void testStash() {
        Configuration configuration = Main.parseArgs(createArgs("-s"));
        assertEquals("Stash", configuration.transformers.get(0).getTransformer());
    }

    @Test
    void testSwap() {
        Configuration configuration = Main.parseArgs(createArgs("-w"));
        assertEquals("Swap", configuration.transformers.get(0).getTransformer());
    }

    @Test
    void testJsonParameters() {
        Configuration configuration = Main.parseArgs(createArgs("-t Hello {\"target\":\"World!\"}"));
        assertTrue(configuration.transformers.get(0).getParameters() instanceof ObjectNode);
        assertEquals("{\"target\":\"World!\"}", configuration.transformers.get(0).getParameters().toString());
    }

    @Test
    void testStringParameters() {
        Configuration configuration = Main.parseArgs(createArgs("-t Hello World!"));
        assertTrue(configuration.transformers.get(0).getParameters() instanceof TextNode);
        assertEquals("World!", configuration.transformers.get(0).getParameters().asText());
    }

    private static String[] createArgs(String cli) {
        return cli.split(" ");
    }
}