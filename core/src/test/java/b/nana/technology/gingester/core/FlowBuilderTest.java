package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.transformers.primitive.LongDef;
import b.nana.technology.gingester.core.transformers.stash.Fetch;
import b.nana.technology.gingester.core.transformers.stash.Stash;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FlowBuilderTest {

    @Test
    void test() {

        AtomicReference<Long> result = new AtomicReference<>();

        new FlowBuilder()

                .node()
                .transformer(new LongDef(new LongDef.Parameters(10L)))
                .add()

                .node()
                .transformer(new Stash(new Stash.Parameters("hello")))
                .add()

                .node()
                .transformer(new Fetch(new Fetch.Parameters("hello")))
                .add()

                .add(result::set)

                .run();

        assertEquals(10, result.get());
    }
}