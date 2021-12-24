package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.configuration.GingesterConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CliParserTest {

    @Test
    void testTransformer() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-t Stash"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testBreak() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-t Stash -b -t Fetch"));
        assertEquals(1, configuration.transformers.size());
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testTransformerWithId() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-t StashId:Stash"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals("StashId", configuration.transformers.get(0).getId().orElseThrow());
    }

    @Test
    void testTerminalTransformer() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-t Stash --"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Optional.of(Collections.emptyList()), configuration.transformers.get(0).getLinks());
    }

    @Test
    void testSyncToTransformer() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-stt Stash"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Collections.singletonList("__seed__"), configuration.transformers.get(0).getSyncs().orElseThrow());
    }

    @Test
    void testSyncFromTransformer() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-sft Stash -stt Fetch"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Optional.empty(), configuration.transformers.get(0).getSyncs());
        assertEquals("Fetch", configuration.transformers.get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("Stash"), configuration.transformers.get(1).getSyncs().orElseThrow());
    }

    @Test
    void testSyncFromTransformerWithId() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-sft StashId:Stash -stt Fetch"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals("StashId", configuration.transformers.get(0).getId().orElseThrow());
        assertEquals(Optional.empty(), configuration.transformers.get(0).getSyncs());
        assertEquals("Fetch", configuration.transformers.get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("StashId"), configuration.transformers.get(1).getSyncs().orElseThrow());
    }

    @Test
    void testLinks() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-t Stash -l A B C"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals(Arrays.asList("A", "B", "C"), configuration.transformers.get(0).getLinks().orElseThrow());
    }

    @Test
    void testFetch() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-f"));
        assertEquals("Fetch", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testStash() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-s"));
        assertEquals("Stash", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testSwap() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-w"));
        assertEquals("Swap", configuration.transformers.get(0).getName().orElseThrow());
    }

    @Test
    void testExceptionLinking() {
        GingesterConfiguration configuration = CliParser.parse(CliSplitter.split("-e Stash -t Monkey 1 -- -t Stash"));
        assertEquals(2, configuration.transformers.size());
        assertEquals("Monkey", configuration.transformers.get(0).getName().orElseThrow());
        assertEquals("Stash", configuration.transformers.get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("Stash"), configuration.excepts);
    }

    @Test
    void testMissingCliTemplateParameterThrowsVariant1() {
        Exception e = assertThrows(Exception.class, () -> CliParser.parse(CliSplitter.split("-cr hello-target.cli")));
        assertTrue(e.getMessage().startsWith("The following has evaluated to null or missing:"));
    }

    @Test
    void testMissingCliTemplateParameterThrowsVariant2() {
        Exception e = assertThrows(Exception.class, () -> CliParser.parse(CliSplitter.split("-cr hello-target.cli '{}'")));
        assertTrue(e.getMessage().startsWith("The following has evaluated to null or missing:"));
    }
}