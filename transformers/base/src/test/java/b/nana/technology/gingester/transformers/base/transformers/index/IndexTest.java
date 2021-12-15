package b.nana.technology.gingester.transformers.base.transformers.index;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexTest {

    @Test
    void test() {

        ArrayDeque<String> results = new ArrayDeque<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-t A:StringCreate '\"Hello, World\"' -t Repeat 3 " +
                "-t B:StringCreate 'Hello, World ${description}!' " +
                "-s " +
                "-t StringCreate ${Repeat.description} " +
                "-t Index '{preserveInsertOrder:true}' " +
                "-t IndexStream " +
                "-s part1 " +
                "-t StringCreate '\"0\"' " +
                "-t Lookup " +
                "-s part2 " +
                "-t StringCreate '${part1} - ${part2}'");

        gingester.add(results::add);

        gingester.run();

        assertEquals(3, results.size());
        assertEquals("Hello, World 0! - Hello, World 0!", results.remove());
        assertEquals("Hello, World 1! - Hello, World 0!", results.remove());
        assertEquals("Hello, World 2! - Hello, World 0!", results.remove());
    }
}
