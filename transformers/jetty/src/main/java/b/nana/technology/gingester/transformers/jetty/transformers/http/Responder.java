package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Responder extends Transformer<Object, Void> {

    private final Map<Context, AtomicBoolean> states = Collections.synchronizedMap(new HashMap<>());

    @Override
    protected void prepare(Context context) {
        Object collision = states.put(context, new AtomicBoolean());
        if (collision != null) throw new IllegalStateException("Collision");
    }

    @Override
    protected void transform(Context context, Object input) throws IOException {

        HttpServletResponse response = (HttpServletResponse) context.fetch("response")
                .orElseThrow(() -> new NoSuchElementException("Nothing to respond to"));

        AtomicBoolean state = context.stream()
                .map(states::get)
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Context not known"));

        if (state.get()) {
            throw new IllegalStateException("Context already responded");
        }

        state.set(true);

        if (input != null) {
            context.fetch("mimeType").map(o -> (String) o).ifPresent(mimeType ->
                    response.addHeader("Content-Type", mimeType));
            if (input instanceof InputStream) {
                InputStream inputStream = (InputStream) input;
                OutputStream outputStream = response.getOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
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
        states.remove(context);
    }
}
