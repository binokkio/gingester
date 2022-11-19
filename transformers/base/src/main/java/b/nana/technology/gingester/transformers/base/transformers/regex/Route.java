package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Route implements Transformer<Object, Object> {

    private final Template inputTemplate;
    private final LinkedHashMap<Pattern, String> routes;
    private final FetchKey fetch;

    public Route(Parameters parameters) {
        inputTemplate = parameters.input != null ? Context.newTemplate(parameters.input) : null;
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
    public Class<?> getInputType() {
        return inputTemplate == null ? String.class : Object.class;
    }

    @Override
    public Object getOutputType() {
        if (fetch == null)
            throw new IllegalStateException("getOutputType() called while fetch == null");
        return fetch;
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.of(
                "route", String.class,
                "match", Matcher.class
        );
    }

    @Override
    public boolean isPassthrough() {
        return fetch == null;  // TODO return an Input sentinel from getOutputType instead
    }

    @Override
    public void setup(SetupControls controls) {
        controls.links(new ArrayList<>(routes.values()));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        String input = inputTemplate == null ? (String) in : inputTemplate.render(context, in);
        for (Map.Entry<Pattern, String> route : routes.entrySet()) {
            Pattern pattern = route.getKey();
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                out.accept(
                        context.stash(Map.of("route", pattern.pattern(), "match", matcher)),
                        fetch != null ? context.require(fetch) : in,
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
                rule(JsonNode::isArray, array ->
                        array.get(0).isObject() ?
                                am((ArrayNode) array, "routes", "fetch") :
                                am((ArrayNode) array, "input", "routes", "fetch")
                );
                rule(json -> !json.path("routes").isObject(), routes -> o("routes", routes, "fetch", "^"));
            }
        }

        public TemplateParameters input;
        public LinkedHashMap<String, String> routes;
        public FetchKey fetch;
    }
}
