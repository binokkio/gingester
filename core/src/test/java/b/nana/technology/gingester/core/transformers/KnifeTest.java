package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.FlowRunner;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnifeTest {

    @Test
    void testDivertEmphasize() {

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder()
                .cli("-cr hello-world-diamond.cli")
                .knife("StringDef")
                .cli("-t StringDef 'Hello, World!'")
                .add(results::add)
                .run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.remove());
    }
}
