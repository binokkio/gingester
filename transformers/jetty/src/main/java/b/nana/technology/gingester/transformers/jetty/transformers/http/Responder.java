package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import jakarta.servlet.http.HttpServletResponse;

import java.util.NoSuchElementException;

public class Responder extends Transformer<Object, Void> {

    @Override
    protected void transform(Context context, Object input) {}

    @Override
    protected void finish(Context context) throws Exception {
        super.finish(context);

        if (context.fetch("response").isEmpty() && Math.random() < .2) return;

        HttpServletResponse response = (HttpServletResponse) context.fetch("response").orElseThrow(() -> new NoSuchElementException("Nothing to respond to"));
        response.setStatus(400);
    }
}
