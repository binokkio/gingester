package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Responder extends Transformer<Object, Void> {

    private final Map<Context, List<Throwable>> contextExceptions = Collections.synchronizedMap(new HashMap<>());

    @Override
    protected void prepare(Context context) {
        // TODO the list should be synchronized as well if exceptions from async linked transformations bubble up to here..
        Object collision = contextExceptions.put(context, new ArrayList<>());
        if (collision != null) throw new IllegalStateException("Collision");
    }

    @Override
    protected void transform(Context context, Object input) {
        context.fetch("exception").ifPresent(exception -> {
            for (Context parent : context) {
                List<Throwable> collector = contextExceptions.get(parent);
                if (collector != null) {
                    collector.add((Throwable) exception);
                    break;
                }
            }
        });
    }

    @Override
    protected void finish(Context context) throws IOException {

        HttpServletResponse response = (HttpServletResponse) context.fetch("response").orElseThrow(() -> new NoSuchElementException("Nothing to respond to"));

        List<Throwable> exceptions = contextExceptions.get(context);

        if (!exceptions.isEmpty()) {
            response.setStatus(409);
            response.getOutputStream().write(exceptions.stream()
                    .map(t -> t.getClass().getSimpleName() + ": " + t.getMessage())
                    .collect(Collectors.joining("\n", "", "\n")).getBytes(StandardCharsets.UTF_8));
        }
    }
}
