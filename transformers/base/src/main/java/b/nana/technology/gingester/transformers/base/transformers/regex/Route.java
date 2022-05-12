package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.OutputFetcher;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Route implements Transformer<String, Object>, OutputFetcher {

    private final FetchKey fetch;
    private final LinkedHashMap<Pattern, String> routes;

    public Route(Parameters parameters) {
        fetch = parameters.fetch;
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
    public FetchKey getOutputStashName() {
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
                out.accept(
                        context.stash("route", route.getKey().pattern()),
                        context.fetch(fetch).findFirst().orElseThrow(() -> new NoSuchElementException("RegexRoute empty fetch")),
                        route.getValue()
                );
                break;  // TODO parameterize
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

        public FetchKey fetch = new FetchKey(1);
        public LinkedHashMap<String, String> routes;
    }
}
