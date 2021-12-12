package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Respond implements Transformer<InputStream, Void> {

    @Override
    public void transform(Context context, InputStream in, Receiver<Void> out) throws Exception {

        Map<?, ?> response = (Map<?, ?>) context.fetch("http", "response").findFirst()
                .orElseThrow(() -> new IllegalStateException("Context did not come from Http.Server"));

        AtomicBoolean handled = (AtomicBoolean) response.get("handled");

        if (!handled.getAndSet(true)) {
            in.transferTo(((HttpServletResponse) response.get("servlet")).getOutputStream());
        } else {
            throw new IllegalStateException("Already handled");
        }
    }
}
