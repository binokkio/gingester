package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class CycleTest {

    @Test
    void test() throws Exception {

        Cycle.Parameters parameters = new Cycle.Parameters();
        parameters.add("foo");
        parameters.add("bar");
        parameters.add("baz");

        Cycle cycle = new Cycle(parameters);

        Context context = Context.newTestContext();
        Queue<Object> results = new ArrayDeque<>();

        cycle.transform(context, context, (UniReceiver<Object>) results::add);
        cycle.transform(context, context, (UniReceiver<Object>) results::add);
        cycle.transform(context, context, (UniReceiver<Object>) results::add);
        cycle.transform(context, context, (UniReceiver<Object>) results::add);

        assertEquals("foo", results.remove());
        assertEquals("bar", results.remove());
        assertEquals("baz", results.remove());
        assertEquals("foo", results.remove());
    }
}