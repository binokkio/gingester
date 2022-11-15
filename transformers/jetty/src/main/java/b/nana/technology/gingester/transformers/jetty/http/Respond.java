package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.InputStream;

public final class Respond implements Transformer<InputStream, String> {

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");

    @Override
    public void transform(Context context, InputStream in, Receiver<String> out) throws Exception {

        Server.ResponseWrapper response = (Server.ResponseWrapper) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        response.respond(servlet -> in.transferTo(servlet.getOutputStream()));

        out.accept(context, "http respond signal");
    }
}
