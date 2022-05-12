package b.nana.technology.gingester.transformers.base.transformers.std;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.stream.Collectors;

@Passthrough
public final class Out implements Transformer<Object, Object> {

    private final FetchKey fetchDescription = new FetchKey("description");
    private final boolean decorate;

    public Out(Parameters parameters) {
        decorate = parameters.decorate;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        if (decorate) {
            String description = context.fetchReverse(fetchDescription)
                    .map(Object::toString)
                    .collect(Collectors.joining(" :: "));
            String prettyStash = context.prettyStash();
            System.out.print(
                    "---- " + description + " ----\n" +
                    (prettyStash.isEmpty() ? "" : prettyStash + '\n') +
                    in + '\n' +
                    "-".repeat(description.length() + 10) + "\n\n"
            );
        } else {
            System.out.println(in);
        }
        out.accept(context, in);
    }

    public static class Parameters {

        public boolean decorate = true;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean decorate) {
            this.decorate = decorate;
        }
    }
}
