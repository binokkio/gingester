package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilterTest {

    @Test
    void testFilterIn() {

        Gingester gingester = new Gingester().cli("" +
                "-t Repeat 11 " +
                "-t StringCreate 'Hello, World ${description}!' " +
                "-t RegexFilterIn '.*1.*'");

        ArrayDeque<String> result = new ArrayDeque<>();
        gingester.attach(result::add);

        gingester.run();

        assertEquals(List.of("Hello, World 1!", "Hello, World 10!"), List.copyOf(result));
    }

    @Test
    void testFilterOut() {

        Gingester gingester = new Gingester().cli("" +
                "-t Repeat 11 " +
                "-t StringCreate 'Hello, World ${description}!' " +
                "-t RegexFilterOut '.*1.*'");

        ArrayDeque<String> result = new ArrayDeque<>();
        gingester.attach(result::add);

        gingester.run();

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

        Gingester gingester = new Gingester().cli("" +
                "-t Repeat 11 " +
                "-t StringCreate 'Hello, World ${description}!' " +
                "-s " +
                "-t StringCreate '\"1\"' " +
                "-s needle " +
                "-f " +
                "-t RegexFilterIn '.*${needle}.*'");

        ArrayDeque<String> result = new ArrayDeque<>();
        gingester.attach(result::add);

        gingester.run();

        assertEquals(List.of("Hello, World 1!", "Hello, World 10!"), List.copyOf(result));
    }
}