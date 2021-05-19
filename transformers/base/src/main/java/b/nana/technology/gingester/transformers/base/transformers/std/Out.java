package b.nana.technology.gingester.transformers.base.transformers.std;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

public class Out extends Transformer<String, Void> {

    private static final int INDENT = 2;

    private final boolean decorate;

    public Out() {
        this(new Parameters());
    }

    public Out(Parameters parameters) {
        super(parameters);
        decorate = parameters.decorate;
    }

    @Override
    protected void transform(Context context, String input) throws JsonProcessingException {
        if (decorate) {
            Map<String, Object> details = context.getDetails();
            String description = context.getDescription();
            System.out.print(
                    "---- " + description + " ----\n" +
                    (details.isEmpty() ? "" : prettyPrint(context.getDetails(), 0) + '\n') +
                    input + '\n' +
                    "-".repeat(description.length() + 10) + "\n\n"
            );
        } else {
            System.out.println(input);
        }
    }

    private String prettyPrint(Object object, int indentation) {

        if (object instanceof Map) {

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("{\n");

            ((Map<?, ?>) object).forEach(
                    (key, value) -> stringBuilder
                            .append(" ".repeat(indentation + INDENT))
                            .append(key)
                            .append('=')
                            .append(prettyPrint(value, indentation + INDENT))
            );

            stringBuilder
                    .append(" ".repeat(indentation))
                    .append("}\n");

            return stringBuilder.toString();

        } else {
            return object.toString() + '\n';
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
