package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RouteTest {

    @Test
    void test() {

        LinkedHashMap<String, String> routes = new LinkedHashMap<>();
        routes.put("h.?llo", "world");
        routes.put(".*", "catch-all");

        Route.Parameters parameters = new Route.Parameters();
        parameters.fetch = new FetchKey("value");
        parameters.routes = routes;

        Context context = Context.newTestContext().stash("value", "routed value").buildForTesting();

        AtomicReference<String> target = new AtomicReference<>();
        AtomicReference<Object> routedValue = new AtomicReference<>();

        Route route = new Route(parameters);
        route.transform(context, "hello", new Receiver<>() {

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
                throw new IllegalStateException("Should not be called");
            }

            @Override
            public void accept(Context.Builder contextBuilder, Object output, String targetId) {
                target.set(targetId);
                routedValue.set(output);
            }
        });

        assertEquals("world", target.get());
        assertEquals("routed value", routedValue.get());
    }
    
    @Test
    void testInFlow() {

        Gingester gingester = new Gingester().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-s " +
                "-t RegexRoute \"{'.ello.*': 'Hello', '.*': 'Other'}\" " +
                "-t Other:Passthrough -- " +
                "-t Hello:Passthrough");
        
        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);
        gingester.run();
        
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testRoutedValueIsFetched() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t JsonDef '{hello:\"world\"}' " +
                "-s record " +
                "-t JsonToString " +
                "-t RegexRoute '{\".\":\"Target\"}' " +
                "-t Target:Passthrough")
                .attach(result::set)
                .run();

        assertEquals("world", result.get().get("hello").asText());
    }

    @Test
    void testRoutedValueCanBeBridged() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t JsonDef '{hello:\"world\"}' " +
                "-s record " +
                "-t JsonToString " +
                "-t RegexRoute '{\".\":\"Target\"}' " +
                "-t Target:StringAppend '!'")
                .attach(result::set)
                .run();

        assertEquals("{\"hello\":\"world\"}!", result.get());
    }
}