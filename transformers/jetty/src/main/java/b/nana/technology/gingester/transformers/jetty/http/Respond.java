package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.InputStream;

public final class Respond implements Transformer<InputStream, Void> {

    @Override
    public void transform(Context context, InputStream in, Receiver<Void> out) throws Exception {

        Server.ResponseWrapper response = (Server.ResponseWrapper) context.fetch("http", "response").findFirst()
                .orElseThrow(() -> new IllegalStateException("Context did not come from Http.Server"));

        response.respond(servlet -> in.transferTo(servlet.getOutputStream()));
    }
}
