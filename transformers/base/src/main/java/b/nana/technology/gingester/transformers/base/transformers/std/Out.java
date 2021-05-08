package b.nana.technology.gingester.transformers.base.transformers.std;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

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
            System.out.println("---- " + description + " ----");
            System.out.println(input);
            System.out.println("-".repeat(description.length() + 10));
            System.out.println();
        } else {
            System.out.println(input);
        }
    }

    public static class Parameters {
        public boolean decorate;
    }
}
