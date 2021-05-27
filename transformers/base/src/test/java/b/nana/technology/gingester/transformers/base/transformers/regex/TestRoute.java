package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.transformers.string.Generate;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestRoute {

    @Test
    void testRoute() {

        Route.Parameters routeParameters = new Route.Parameters();
        routeParameters.pattern = "\\.(.+?)$";
        routeParameters.group = 1;
        routeParameters.routes = Map.of(
                "csv", "CsvHandler",
                "json", "JsonHandler"
        );
        Route route = new Route(routeParameters);

        Generate csvHandler = new Generate(new Generate.Parameters("CSV handler triggered"));
        Generate jsonHandler = new Generate(new Generate.Parameters("JSON handler triggered"));

        AtomicReference<String> csvHandlerResult = new AtomicReference<>();
        AtomicReference<String> jsonHandlerResult = new AtomicReference<>();

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(route, "test.json");

        gBuilder.name("CsvHandler", csvHandler);
        gBuilder.link(route, csvHandler);
        gBuilder.link(csvHandler, csvHandlerResult::set);

        gBuilder.name("JsonHandler", jsonHandler);
        gBuilder.link(route, jsonHandler);
        gBuilder.link(jsonHandler, jsonHandlerResult::set);

        gBuilder.build().run();

        assertNull(csvHandlerResult.get());
        assertEquals("JSON handler triggered", jsonHandlerResult.get());
    }
}
