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

    private DummyTarget parse(String cli) {
        DummyTarget dummyTarget = new DummyTarget();
        CliParser.parse(dummyTarget, CliSplitter.split(cli));
        return dummyTarget;
    }

    @Test
    void testTransformer() {
        DummyTarget result = parse("-t Stash");
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testClosedCommentBlock() {
        DummyTarget result = parse("-t Stash ++ -t Fetch -foo -bar ++ -t Passthrough");
        assertEquals(2, result.getTransformers().size());
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
        assertEquals("Passthrough", result.getTransformers().get(1).getName().orElseThrow());
    }

    @Test
    void testUnclosedCommentBlock() {
        DummyTarget result = parse("-t Stash ++ -t Fetch");
        assertEquals(1, result.getTransformers().size());
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testTransformerWithId() {
        DummyTarget result = parse("-t StashId:Stash");
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
        assertEquals("StashId", result.getTransformers().get(0).getId().orElseThrow());
    }

    @Test
    void testTerminalTransformer() {
        DummyTarget result = parse("-t Stash --");
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
        assertEquals(Optional.of(Collections.emptyList()), result.getTransformers().get(0).getLinks());
    }

    @Test
    void testSyncToTransformer() {
        DummyTarget result = parse("-stt Stash");
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
        assertEquals(Collections.singletonList("__seed__"), result.getTransformers().get(0).getSyncs().orElseThrow());
    }

    @Test
    void testSyncFromTransformer() {
        DummyTarget result = parse("-sft Stash -stt Fetch");
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
        assertEquals(Optional.empty(), result.getTransformers().get(0).getSyncs());
        assertEquals("Fetch", result.getTransformers().get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("Stash"), result.getTransformers().get(1).getSyncs().orElseThrow());
    }

    @Test
    void testSyncFromTransformerWithId() {
        DummyTarget result = parse("-sft StashId:Stash -stt Fetch");
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
        assertEquals("StashId", result.getTransformers().get(0).getId().orElseThrow());
        assertEquals(Optional.empty(), result.getTransformers().get(0).getSyncs());
        assertEquals("Fetch", result.getTransformers().get(1).getName().orElseThrow());
        assertEquals(Collections.singletonList("StashId"), result.getTransformers().get(1).getSyncs().orElseThrow());
    }

    @Test
    void testLinks() {
        DummyTarget result = parse("-t Stash -l A B C");
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
        assertEquals(Arrays.asList("A", "B", "C"), result.getTransformers().get(0).getLinks().orElseThrow());
    }

    @Test
    void testFetch() {
        DummyTarget result = parse("-f");
        assertEquals("Fetch", result.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testStash() {
        DummyTarget result = parse("-s");
        assertEquals("Stash", result.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testSwap() {
        DummyTarget result = parse("-w");
        assertEquals("Swap", result.getTransformers().get(0).getName().orElseThrow());
    }

    @Test
    void testExceptionLinking() {
        DummyTarget result = parse("-e Stash -t Monkey 1 -- -t Stash");
        assertEquals(2, result.getTransformers().size());
        assertEquals("Monkey", result.getTransformers().get(0).getName().orElseThrow());
        assertEquals("Stash", result.getTransformers().get(1).getName().orElseThrow());
//        assertEquals(Collections.singletonList("Stash"), configuration.excepts);  TODO
    }

    @Test
    void testMissingCliTemplateParameterThrowsVariant1() {
        Exception e = assertThrows(Exception.class, () -> parse("-cr hello-target.cli"));
        assertTrue(e.getMessage().startsWith("The following has evaluated to null or missing:"));
    }

    @Test
    void testMissingCliTemplateParameterThrowsVariant2() {
        Exception e = assertThrows(Exception.class, () -> parse("-cr hello-target.cli '{}'"));
        assertTrue(e.getMessage().startsWith("The following has evaluated to null or missing:"));
    }

    @Test
    void testSyncUsesIds() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
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