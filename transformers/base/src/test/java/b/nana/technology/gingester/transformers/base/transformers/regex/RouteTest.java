package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Gingester;
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
        parameters.routes = routes;

        Context context = Context.newTestContext();

        AtomicReference<String> result = new AtomicReference<>();

        Route route = new Route(parameters);
        route.transform(context, "hello", new Receiver<>() {

            @Override
            public void accept(Context context, String output) {
                throw new IllegalStateException("Should not be called");
            }

            @Override
            public void accept(Context.Builder contextBuilder, String output) {
                throw new IllegalStateException("Should not be called");
            }

            @Override
            public void accept(Context context, String output, String targetId) {
                result.set(targetId);
            }

            @Override
            public void accept(Context.Builder contextBuilder, String output, String targetId) {
                throw new IllegalStateException("Should not be called");
            }
        });

        assertEquals("world", result.get());
    }
    
    @Test
    void testInFlow() {

        Gingester gingester = new Gingester("" +
                "-t StringCreate 'Hello, World!' " +
                "-t RegexRoute \"{'.ello.*': 'Hello', '.*': 'Other'}\" " +
                "-t Other:Passthrough -- " +
                "-t Hello:Passthrough");
        
        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);
        gingester.run();
        
        assertEquals("Hello, World!", result.get());
    }
}