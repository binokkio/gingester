package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Passthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route<T> extends Passthrough<T> {

    private final String[] stash;
    private final Pattern pattern;
    private final int group;
    private final Map<String, String> routes;
    private final String defaultRoute;

    public Route(Parameters parameters) {
        super(parameters);
        stash = parameters.stash != null ? parameters.stash.split("\\.") : null;
        pattern = Pattern.compile(parameters.pattern);
        group = parameters.group;
        routes = parameters.routes;
        defaultRoute = parameters.defaultRoute;
    }

    @Override
    public List<String> getLinks() {
        List<String> links = new ArrayList<>(routes.values());
        if (defaultRoute != null) links.add(defaultRoute);
        return links;
    }

    @Override
    protected void transform(Context context, T input) throws Exception {

        String string = stash != null ?
                context.fetch(stash).orElseThrow().toString() :
                input.toString();

        Matcher matcher = pattern.matcher(string);
        if (!matcher.find()) {
            if (defaultRoute == null) throw new IllegalStateException("Pattern not found");
            emit(context, input, defaultRoute);
            return;
        }

        String route = routes.get(matcher.group(group));
        if (route == null) {
            if (defaultRoute == null) throw new IllegalStateException("No route for " + matcher.group(group));
            emit(context, input, defaultRoute);
            return;
        }

        emit(context, input, route);
    }

    public static class Parameters {
        public String stash;
        public String pattern;
        public int group;
        public Map<String, String> routes;
        public String defaultRoute;
    }
}
