package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;

public final class Respond implements Transformer<InputStream, Void> {

    private final ContextMap<State> states = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Void> out) {
        State state = new State();
        state.response = (HttpServletResponse) context.fetch("http", "response", "servlet").findFirst().orElseThrow(
                () -> new IllegalStateException("Context did not contain HttpServletResponse, check syncs"));
        states.put(context, state);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<Void> out) throws Exception {
        states.act(context, state -> {

            if (state.responded) throw new IllegalStateException("Already responded");
            state.responded = true;

            HttpServletResponse response = state.response;

            context.fetch("exception").findAny().ifPresentOrElse(
                    exception -> {
                        response.setStatus(409);
                        response.addHeader("Content-Type", "text/plain; charset=UTF-8");
                    },
                    () -> context.fetch("mimeType").findFirst().ifPresent(mimeType ->
                            response.addHeader("Content-Type", (String) mimeType))
            );

            in.transferTo(response.getOutputStream());
        });
    }

    @Override
    public void finish(Context context, Receiver<Void> out) {
        states.remove(context);
    }

    private static final class State {
        private HttpServletResponse response;
        private boolean responded;
    }
}