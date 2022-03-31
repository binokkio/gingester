package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Route implements Transformer<String, String> {

    public final LinkedHashMap<Pattern, String> routes;

    public Route(Parameters parameters) {
        routes = parameters.routes.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(Pattern.compile(e.getKey()), e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> { throw new IllegalStateException("Collision in RegexRoute routes"); },
                        LinkedHashMap::new
                ));
    }

    @Override
    public void setup(SetupControls controls) {
        controls.links(new ArrayList<>(routes.values()));
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws Exception {
        for (Map.Entry<Pattern, String> route : routes.entrySet()) {
            if (route.getKey().matcher(in).find()) {
                out.accept(context, in, route.getValue());
                break;
            }
        }
    }

    public static class Parameters {

        public LinkedHashMap<String, String> routes;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(LinkedHashMap<String, String> routes) {
            this.routes = routes;
        }
    }
}
