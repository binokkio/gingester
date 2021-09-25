package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.configuration.GingesterConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void testTransformer() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-t Hello"));
        assertEquals("Hello", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testBreak() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-t Hello -b -t World"));
        assertEquals(1, configuration.transformers.size());
        assertEquals("Hello", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testTransformerWithId() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-t HelloId:Hello"));
        assertEquals("Hello", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals("HelloId", configuration.transformers.get(0).getId().orElseThrow());
    }

    @Test
    void testTerminalTransformer() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-t Hello --"));
        assertEquals("Hello", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Optional.of(Collections.emptyList()), configuration.transformers.get(0).getLinks());
    }

    @Test
    void testSyncToTransformer() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-stt Hello"));
        assertEquals("Hello", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Collections.singletonList("__seed__"), configuration.transformers.get(0).getSyncs().orElseThrow());
    }

    @Test
    void testSyncFromTransformer() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-sft Hello -stt World"));
        assertEquals("Hello", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Optional.empty(), configuration.transformers.get(0).getSyncs());
        assertEquals("World", configuration.transformers.get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("Hello"), configuration.transformers.get(1).getSyncs().orElseThrow());
    }

    @Test
    void testSyncFromTransformerWithId() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-sft HelloId:Hello -stt World"));
        assertEquals("Hello", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals("HelloId", configuration.transformers.get(0).getId().orElseThrow());
        assertEquals(Optional.empty(), configuration.transformers.get(0).getSyncs());
        assertEquals("World", configuration.transformers.get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("HelloId"), configuration.transformers.get(1).getSyncs().orElseThrow());
    }

    @Test
    void testLinks() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-t Hello -l A B C"));
        assertEquals("Hello", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Arrays.asList("A", "B", "C"), configuration.transformers.get(0).getLinks().orElseThrow());
    }

    @Test
    void testFetch() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-f"));
        assertEquals("Fetch", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testStash() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-s"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testSwap() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-w"));
        assertEquals("Swap", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testJsonParameters() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-t Hello {\"target\":\"World!\"}"));
        JsonNode parameters = configuration.transformers.get(0).getParameters().orElseThrow();
        assertTrue(parameters instanceof ObjectNode);
        assertEquals("{\"target\":\"World!\"}", parameters.toString());
    }

    @Test
    void testStringParameters() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-t Hello World!"));
        JsonNode parameters = configuration.transformers.get(0).getParameters().orElseThrow();
        assertTrue(parameters instanceof TextNode);
        assertEquals("World!", parameters.asText());
    }

    @Test
    void testExceptionLinking() {
        GingesterConfiguration configuration = Main.parseArgs(CliParser.parse("-e Stash -t Throw -- -t Stash"));
        assertEquals(2, configuration.transformers.size());
        assertEquals("Throw", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Collections.singletonList("Stash"), configuration.transformers.get(0).getExcepts().orElseThrow());
        assertEquals("Stash", configuration.transformers.get(1).getName().orElseThrow());
    }
}