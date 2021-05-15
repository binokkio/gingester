package b.nana.technology.gingester.transformers.base.transformers.std;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Out extends Transformer<String, Void> {

    private final boolean decorate;

    public Out() {
        decorate = false;
    }

    public Out(Parameters parameters) {
        super(parameters);
        decorate = parameters.decorate;
    }

    @Override
    protected void transform(Context context, String input) {
        if (decorate) {
            String description = context.getDescription();
            System.out.print(
                    "---- " + description + " ----\n" +
                    input + '\n' +
                    "-".repeat(description.length() + 10) +
                    "\n\n"
            );
        } else {
            System.out.println(input);
        }
    }

    public static class Parameters {

        public boolean decorate;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean decorate) {
            this.decorate = decorate;
        }
    }
}
