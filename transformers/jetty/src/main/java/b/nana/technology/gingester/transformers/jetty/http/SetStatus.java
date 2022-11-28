package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Passthrough
public final class SetStatus implements Transformer<Object, Object> {

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");

    private final int status;

    public SetStatus(Parameters parameters) {
        status = parameters.status;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        HttpResponse response = (HttpResponse) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        response.setStatus(status);

        out.accept(context, in);
    }

    public static class Parameters {

        public int status;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int status) {
            this.status = status;
        }
    }
}
