package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteTest {

    @Test
    void test() throws Exception {

        LinkedHashMap<String, String> routes = new LinkedHashMap<>();
        routes.put("h.?llo", "world");
        routes.put(".*", "catch-all");

        Route.Parameters parameters = new Route.Parameters();
        parameters.key = "${foo}";
        parameters.routes = routes;

        Context context = new Context.Builder().stash("foo", "hello").build();

        AtomicReference<String> result = new AtomicReference<>();

        Route route = new Route(parameters);
        route.transform(context, "irrelevant", new Receiver<>() {

            @Override
            public void accept(Context context, Object output) {
                throw new IllegalStateException("Should not be called");
            }

            @Override
            public void accept(Context.Builder contextBuilder, Object output) {
                throw new IllegalStateException("Should not be called");
            }

            @Override
            public void accept(Context context, Object output, String targetId) {
                result.set(targetId);
            }

            @Override
            public void accept(Context.Builder contextBuilder, Object output, String targetId) {
                throw new IllegalStateException("Should not be called");
            }
        });

        assertEquals("world", result.get());
    }
}