package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.OutputFetcher;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Route implements Transformer<String, Object>, OutputFetcher {

    private final String[] fetch;
    private final LinkedHashMap<Pattern, String> routes;

    public Route(Parameters parameters) {
        fetch = Fetch.parseStashName(parameters.fetch);
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
    public String[] getOutputStashName() {
        return fetch;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.links(new ArrayList<>(routes.values()));
    }

    @Override
    public void transform(Context context, String in, Receiver<Object> out) throws Exception {
        for (Map.Entry<Pattern, String> route : routes.entrySet()) {
            if (route.getKey().matcher(in).find()) {
                // TODO throw if fetch is empty
                context.fetch(fetch)
                        .findFirst()
                        .ifPresent(o -> out.accept(context, o, route.getValue()));
                break;
            }
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(json -> !json.path("routes").isObject(), routes -> o("routes", routes));
            }
        }

        public String fetch = "";
        public LinkedHashMap<String, String> routes;
    }
}
