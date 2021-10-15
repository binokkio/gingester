package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.servlet.http.HttpServletResponse;

public final class SetStatus implements Transformer<Object, Object> {

    private final int status;

    public SetStatus(Parameters parameters) {
        status = parameters.status;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        HttpServletResponse response = (HttpServletResponse) context.fetch("http", "response", "servlet").findFirst().orElseThrow(
                () -> new IllegalStateException("Context did not contain HttpServletResponse"));

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
