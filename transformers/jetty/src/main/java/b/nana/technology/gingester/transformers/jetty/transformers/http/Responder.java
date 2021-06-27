package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Transformer;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Responder extends Transformer<Object, Void> {

    private final ContextMap<AtomicBoolean> states = new ContextMap<>();

    @Override
    protected void prepare(Context context) {
        states.put(context, new AtomicBoolean());
    }

    @Override
    protected void transform(Context context, Object input) throws IOException {

        HttpServletResponse response = (HttpServletResponse) context.fetch("response")
                .orElseThrow(() -> new NoSuchElementException("Nothing to respond to"));

        AtomicBoolean state = states.require(context);
        if (state.get()) {
            throw new IllegalStateException("Context already responded");
        }

        state.set(true);

        if (input != null) {
            context.fetch("mimeType").map(o -> (String) o).ifPresent(mimeType ->
                    response.addHeader("Content-Type", mimeType));
            if (input instanceof InputStream) {
                ((InputStream) input).transferTo(response.getOutputStream());
            } else if (input instanceof byte[]) {
                response.getOutputStream().write((byte[]) input);
            } else {
                response.getOutputStream().write(input.toString().getBytes(StandardCharsets.UTF_8));
            }
        } else {
            Throwable exception = (Throwable) context.fetch("exception").orElseThrow();  // TODO
            response.setStatus(409);
            response.addHeader("Content-Type", "text/plain; charset=UTF-8");
            response.getOutputStream().write(
                    (exception.getClass().getSimpleName() + ": " + exception.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void finish(Context context) {
        states.requireRemove(context);
    }
}
