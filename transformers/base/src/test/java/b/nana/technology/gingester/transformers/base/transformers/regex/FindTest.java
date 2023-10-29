package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FindTest {

    @Test
    public void testStashNamedGroups() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t RegexFind '(?<one>\\\\w+), (?<two>\\\\w+)' " +
                "-t StringDef '${one}+${two}'")
                .add(result::set)
                .run();

        assertEquals("Hello+World", result.get());
    }
}