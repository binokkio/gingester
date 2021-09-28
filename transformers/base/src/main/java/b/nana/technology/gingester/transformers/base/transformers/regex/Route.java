package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Route implements Transformer<Object, Object> {

    public final Context.Template keyTemplate;
    public final LinkedHashMap<Pattern, String> routes;

    public Route(Parameters parameters) {
        keyTemplate = Context.newTemplate(parameters.key);
        routes = parameters.routes.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(Pattern.compile(e.getKey()), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        String key = keyTemplate.render(context);
        for (Map.Entry<Pattern, String> route : routes.entrySet()) {
            if (route.getKey().matcher(key).matches()) {
                out.accept(context, in, route.getValue());
                break;
            }
        }
    }

    public static class Parameters {
        public String key;
        public LinkedHashMap<String, String> routes;
    }
}
