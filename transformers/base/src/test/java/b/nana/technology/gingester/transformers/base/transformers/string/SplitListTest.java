package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitListTest {

    @Test
    void testSimple() {

        AtomicReference<List<String>> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                        -t StringDef one$two$three
                        -t StringSplitList $""")
                .add(result::set)
                .run();

        assertEquals("one", result.get().get(0));
        assertEquals("two", result.get().get(1));
        assertEquals("three", result.get().get(2));
    }

    @Test
    void testLimit() {

        AtomicReference<List<String>> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                        -t StringDef 'one, two, three'
                        -t StringSplitList ', ' 2""")
                .add(result::set)
                .run();

        assertEquals("one", result.get().get(0));
        assertEquals("two, three", result.get().get(1));
    }
}