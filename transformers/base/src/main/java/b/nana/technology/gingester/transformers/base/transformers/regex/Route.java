package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route extends Transformer<String, Void> {

    private final Pattern pattern;
    private final int group;
    private final Map<String, String> routes;
    private final String defaultRoute;
    private final Map<String, Integer> directions = new HashMap<>();
    private Integer defaultDirection;

    public Route(Parameters parameters) {
        super(parameters);
        pattern = Pattern.compile(parameters.pattern);
        group = parameters.group;
        routes = parameters.routes;
        defaultRoute = parameters.defaultRoute;
    }

    @Override
    protected void setup(Setup setup) {

        routes.forEach((key, value) ->
                directions.put(key, setup.getDirection(value)));

        if (defaultRoute != null) {
            defaultDirection = setup.getDirection(defaultRoute);
        }
    }

    @Override
    protected void transform(Context context, String input) throws Exception {

        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            if (defaultDirection == null) throw new IllegalStateException("Pattern not found");
            emit(context, null, defaultDirection);
            return;
        }

        Integer direction = directions.get(matcher.group(group));
        if (direction == null) {
            if (defaultDirection == null) throw new IllegalStateException("No route for " + matcher.group(group));
            emit(context, null, defaultDirection);
            return;
        }

        emit(context, null, direction);
    }

    public static class Parameters {
        public String pattern;
        public int group;
        public Map<String, String> routes;
        public String defaultRoute;
    }
}
