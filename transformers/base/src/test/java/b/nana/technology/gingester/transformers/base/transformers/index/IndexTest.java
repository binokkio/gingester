package b.nana.technology.gingester.transformers.base.transformers.index;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexTest {

    @Test
    void test() {

        ArrayDeque<String> results = new ArrayDeque<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-t A:String.Create '{count:3,template:\"Hello, World\"}' " +
                "-t B:String.Create 'Hello, World ${description}!' " +
                "-s " +
                "-t String.Create ${A.description} " +
                "-t Index '{preserveInsertOrder:true}' " +
                "-t Index.Stream " +
                "-s part1 " +
                "-t String.Create '\"0\"' " +
                "-t Lookup " +
                "-s part2 " +
                "-t String.Create '${part1} - ${part2}'");

        gingester.add(results::add);

        gingester.run();

        assertEquals(3, results.size());
        assertEquals("Hello, World 0! - Hello, World 0!", results.remove());
        assertEquals("Hello, World 1! - Hello, World 0!", results.remove());
        assertEquals("Hello, World 2! - Hello, World 0!", results.remove());
    }
}
