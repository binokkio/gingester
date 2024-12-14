package b.nana.technology.gingester.transformers.base.transformers.list;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GetTest {

    @Test
    void testGetFromStart() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t Repeat 5
                -t StringDef 'Hello, ${description}!'
                -t ListCollect
                -t ListGet 1
                """)
                .add(result::set)
                .run();

        assertEquals("Hello, 1!", result.get());
    }

    @Test
    void testGetFromEnd() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t Repeat 5
                -t StringDef 'Hello, ${description}!'
                -t ListCollect
                -t ListGet @ -1
                """)
                .add(result::set)
                .run();

        assertEquals("Hello, 4!", result.get());
    }
}