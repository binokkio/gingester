package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SplitTest {

    @Test
    void testMaxSplits() {

        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef a,b,c " +
                "-t InputStreamSplit , 1 " +
                "-t OrdinalRoute A B " +
                "-t A:InputStreamToString -- " +
                "-t B:InputStreamToString --")
                .attach(a::set, "A")
                .attach(b::set, "B")
                .run();

        assertEquals("a", a.get());
        assertEquals("b,c", b.get());
    }
}
