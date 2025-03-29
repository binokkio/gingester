package b.nana.technology.gingester.transformers.base.transformers.list;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

class CollectAndStreamTest {

    @Test
    void test() {

        ArrayDeque<String> result = new ArrayDeque<>();

        new FlowBuilder().cli("""
                -t Repeat 9 \
                -t Cycle A B C \
                -s letter \
                -t StringDef '${letter}-${Repeat.description}' \
                -t ListCollect '{type:"array"}'\
                -t ListStream""")
                .add(result::add)
                .run();

        assertEquals("A-0", result.remove());
        assertEquals("B-1", result.remove());
        assertEquals("C-2", result.remove());

        assertEquals("A-3", result.remove());
        assertEquals("B-4", result.remove());
        assertEquals("C-5", result.remove());

        assertEquals("A-6", result.remove());
        assertEquals("B-7", result.remove());
        assertEquals("C-8", result.remove());
    }
}