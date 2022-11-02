package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GroupTest {

    @Test
    public void testOrdinalGroup() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t RegexFind '(\\\\w+), (\\\\w+)' " +
                "-t RegexGroup 2 ")
                .add(result::set)
                .run();

        assertEquals("World", result.get());
    }

    @Test
    public void testNamedGroup() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t RegexFind '(\\\\w+), (?<target>\\\\w+)' " +
                "-t RegexGroup target ")
                .add(result::set)
                .run();

        assertEquals("World", result.get());
    }
}