package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ThrottleTest {

    @Test
    void test() {

        AtomicReference<Long> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Repeat 2000 " +
                "-t Throttle 1000 " +
                "-t CountCollect")
                .add(result::set)
                .run();

        assertEquals(2000, result.get());
    }
}