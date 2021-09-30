package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Responder implements Transformer<Object, Void> {

    private final ContextMap<AtomicBoolean> states = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Void> out) {
        states.put(context, new AtomicBoolean());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Void> out) throws Exception {

        HttpServletResponse response = (HttpServletResponse) context.fetch("response")
                .findFirst().orElseThrow(() -> new NoSuchElementException("Nothing to respond to"));

        AtomicBoolean state = states.get(context);
        if (state.get()) {
            throw new IllegalStateException("Context already responded");
        }

        state.set(true);

        if (in != null) {
            context.fetch("mimeType").map(o -> (String) o).findFirst().ifPresent(mimeType ->
                    response.addHeader("Content-Type", mimeType));
            if (in instanceof InputStream) {
                ((InputStream) in).transferTo(response.getOutputStream());
            } else if (in instanceof byte[]) {
                response.getOutputStream().write((byte[]) in);
            } else {
                response.getOutputStream().write(in.toString().getBytes(StandardCharsets.UTF_8));
            }
        } else {
            Throwable exception = (Throwable) context.fetch("exception").findFirst().orElseThrow();  // TODO
            response.setStatus(409);
            response.addHeader("Content-Type", "text/plain; charset=UTF-8");
            response.getOutputStream().write(
                    (exception.getClass().getSimpleName() + ": " + exception.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void finish(Context context, Receiver<Void> out) {
        states.remove(context);
    }
}
