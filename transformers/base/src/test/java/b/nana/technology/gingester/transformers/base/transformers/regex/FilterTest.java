package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest {

    @Test
    void testFilterIn() {

        ArrayDeque<String> result = new ArrayDeque<>();

        new FlowBuilder().cli("""
                -t Repeat 11
                -t StringDef 'Hello, World ${description}!'
                -t RegexFilterIn '.*1.*'
                """)
                .add(result::add)
                .run();

        assertEquals(List.of("Hello, World 1!", "Hello, World 10!"), List.copyOf(result));
    }

    @Test
    void testFilterOut() {

        ArrayDeque<String> result = new ArrayDeque<>();

        new FlowBuilder().cli("""
                -t Repeat 11
                -t StringDef 'Hello, World ${description}!'
                -t RegexFilterOut '.*1.*'
                """)
                .add(result::add)
                .run();

        assertEquals(
                List.of(
                        "Hello, World 0!",
                        "Hello, World 2!",
                        "Hello, World 3!",
                        "Hello, World 4!",
                        "Hello, World 5!",
                        "Hello, World 6!",
                        "Hello, World 7!",
                        "Hello, World 8!",
                        "Hello, World 9!"
                ),
                List.copyOf(result)
            );
    }

    @Test
    void testFilterInWithTemplatedRegex() {

        ArrayDeque<String> result = new ArrayDeque<>();

        new FlowBuilder().cli("""
                -t Repeat 11
                -t StringDef 'Hello, World ${description}!'
                -s
                -t StringDef '"1"'
                -s needle
                -f
                -t RegexFilterIn '.*${needle}.*'
                """)
                .add(result::add)
                .run();

        assertEquals(List.of("Hello, World 1!", "Hello, World 10!"), List.copyOf(result));
    }

    @Test
    void testFilterInWithTemplatedInput() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t Repeat 11
                -t StringDef 'Hello, World ${description}!'
                -s input
                -f Repeat.description
                -t RegexFilterIn '${input}' 'World 1'
                -t InputStreamJoin ,
                -t InputStreamToString
                """)
                .add(result::set)
                .run();

        assertEquals("1,10", result.get());
    }
}