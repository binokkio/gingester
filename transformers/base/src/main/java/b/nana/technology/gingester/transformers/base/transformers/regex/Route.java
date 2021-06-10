package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.RouteBase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route<T> extends RouteBase<T> {

    private final String[] stash;
    private final Pattern pattern;
    private final int group;

    public Route(Parameters parameters) {
        super(parameters);
        stash = parameters.stash != null ? parameters.stash.split("\\.") : null;
        pattern = Pattern.compile(parameters.pattern);
        group = parameters.group;
    }

    @Override
    protected void transform(Context context, T input) throws Exception {

        String string = stash != null ?
                context.fetch(stash).orElseThrow().toString() :
                input.toString();

        Matcher matcher = pattern.matcher(string);
        if (!matcher.find()) {
            emit(context, input, getDefaultRoute().orElseThrow(() -> new IllegalStateException("Pattern not found")));
        } else {
            emit(context, input,
                    getRoute(matcher.group(group)).orElseGet(
                            () -> getDefaultRoute()
                                    .orElseThrow(() -> new IllegalStateException("No route for " + string))));
        }
    }

    public static class Parameters extends RouteBase.Parameters {
        public String stash;
        public String pattern;
        public int group;
    }
}
