package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

public final class Append implements Transformer<String, String> {

    private final Context.Template append;

    public Append(Parameters parameters) {
        append = Context.newTemplate(requireNonNull(parameters.append));
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        out.accept(context, in + append.render(context));
    }

    public static class Parameters {

        public String append;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String append) {
            this.append = append;
        }
    }
}
