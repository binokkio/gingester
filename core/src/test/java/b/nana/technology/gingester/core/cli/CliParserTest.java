package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliParserTest {

    @Test
    void testTransformer() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-t Stash"));
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testClosedCommentBlock() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-t Stash ++ -t Fetch -foo -bar ++ -t Passthrough"));
        assertEquals(2, gingester.getTransformers().size());
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
        assertEquals("Passthrough", gingester.getTransformers().get(1).getName().orElseThrow());
    }

    @Test
    void testUnclosedCommentBlock() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-t Stash ++ -t Fetch"));
        assertEquals(1, gingester.getTransformers().size());
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testTransformerWithId() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-t StashId:Stash"));
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
        assertEquals("StashId", gingester.getTransformers().get(0).getId().orElseThrow());
    }

    @Test
    void testTerminalTransformer() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-t Stash --"));
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
        assertEquals(Optional.of(Collections.emptyList()), gingester.getTransformers().get(0).getLinks());
    }

    @Test
    void testSyncToTransformer() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-stt Stash"));
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
        assertEquals(Collections.singletonList("__seed__"), gingester.getTransformers().get(0).getSyncs().orElseThrow());
    }

    @Test
    void testSyncFromTransformer() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-sft Stash -stt Fetch"));
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
        assertEquals(Optional.empty(), gingester.getTransformers().get(0).getSyncs());
        assertEquals("Fetch", gingester.getTransformers().get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("Stash"), gingester.getTransformers().get(1).getSyncs().orElseThrow());
    }

    @Test
    void testSyncFromTransformerWithId() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-sft StashId:Stash -stt Fetch"));
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
        assertEquals("StashId", gingester.getTransformers().get(0).getId().orElseThrow());
        assertEquals(Optional.empty(), gingester.getTransformers().get(0).getSyncs());
        assertEquals("Fetch", gingester.getTransformers().get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("StashId"), gingester.getTransformers().get(1).getSyncs().orElseThrow());
    }

    @Test
    void testLinks() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-t Stash -l A B C"));
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
        assertEquals(Arrays.asList("A", "B", "C"), gingester.getTransformers().get(0).getLinks().orElseThrow());
    }

    @Test
    void testFetch() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-f"));
        assertEquals("Fetch", gingester.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testStash() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-s"));
        assertEquals("Stash", gingester.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testSwap() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-w"));
        assertEquals("Swap", gingester.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testExceptionLinking() {
        Gingester gingester = CliParser.parse(CliSplitter.split("-e Stash -t Monkey 1 -- -t Stash"));
        assertEquals(2, gingester.getTransformers().size());
        assertEquals("Monkey", gingester.getTransformers().get(0).getName().orElseThrow());
        assertEquals("Stash", gingester.getTransformers().get(1).getName().orElseThrow());
//        assertEquals(Collections.singletonList("Stash"), configuration.excepts);  TODO
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

    @Test
    void testSyncUsesIds() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester("" +
                "-t Generate one " +
                "-s s1 " +
                "-t Generate two " +
                "-s s2 " +
                "-t Generate three " +
                "-sft Stash s3 " +
                "-stt OnFinish " +
                "-f s2")
                .attach(result::set)
                .run();

        assertEquals("two", result.get());
    }
}