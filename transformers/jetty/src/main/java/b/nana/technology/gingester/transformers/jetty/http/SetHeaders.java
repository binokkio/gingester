package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;

@Passthrough
public final class SetHeaders implements Transformer<Object, Object> {

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");

    private final Map<String, String> headers;

    public SetHeaders(Parameters parameters) {
        headers = parameters;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        Server.ResponseWrapper response = (Server.ResponseWrapper) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        headers.forEach(response::addHeader);

        out.accept(context, in);
    }

    public static class Parameters extends HashMap<String, String> {

    }
}
