package b.nana.technology.gingester.transformers.base.common;

import b.nana.technology.gingester.core.Passthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class RouteBase<T> extends Passthrough<T> {

    private final Map<String, String> routes;
    private final String defaultRoute;

    public RouteBase(Parameters parameters) {
        super(parameters);
        routes = parameters.routes;
        defaultRoute = parameters.defaultRoute;
    }

    @Override
    public List<String> getLinks() {
        List<String> links = new ArrayList<>(routes.values());
        if (defaultRoute != null) links.add(defaultRoute);
        return links;
    }

    protected final Optional<String> getRoute(String key) {
        return Optional.ofNullable(routes.get(key));
    }

    protected final Optional<String> getDefaultRoute() {
        return Optional.ofNullable(defaultRoute);
    }

    public static class Parameters {
        public Map<String, String> routes;
        public String defaultRoute;
    }
}
