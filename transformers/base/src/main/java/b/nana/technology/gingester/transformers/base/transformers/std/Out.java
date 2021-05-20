package b.nana.technology.gingester.transformers.base.transformers.std;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

public class Out extends Transformer<Object, Void> {

    private final boolean decorate;

    public Out() {
        this(new Parameters());
    }

    public Out(Parameters parameters) {
        super(parameters);
        decorate = parameters.decorate;
    }

    @Override
    protected void transform(Context context, Object input) throws JsonProcessingException {
        if (decorate) {
            String description = context.getDescription();
            String prettyDetails = context.prettyDetails();
            System.out.print(
                    "---- " + description + " ----\n" +
                    (prettyDetails.isEmpty() ? "" : prettyDetails + '\n') +
                    input + '\n' +
                    "-".repeat(description.length() + 10) + "\n\n"
            );
        } else {
            System.out.println(input);
        }
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
