package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.RouteBase;

public class Route<T> extends RouteBase<T> {

    private final String[] stash;

    public Route(Parameters parameters) {
        super(parameters);
        stash = parameters.stash != null ? parameters.stash.split("\\.") : null;
    }

    @Override
    protected void transform(Context context, T input) throws Exception {

        String string = stash != null ?
                context.fetch(stash).orElseThrow().toString() :
                input.toString();

        emit(
                context,
                input,
                getRoute(string).orElseGet(
                        () -> getDefaultRoute()
                                .orElseThrow(() ->  new IllegalStateException("No route for " + string)
        )));
    }

    public static class Parameters extends RouteBase.Parameters {
        public String stash;
    }
}
