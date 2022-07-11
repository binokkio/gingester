package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.stream.Collectors;

@Names(1)
@Passthrough
public final class Probe implements Transformer<Object, Object> {

    private final FetchKey fetchDescription = new FetchKey("description");
    private final boolean decorate;

    public Probe(Parameters parameters) {
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
