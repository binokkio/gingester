package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Move extends Transformer<Path, Path> {

    private final Context.StringFormat destinationFormat;

    public Move(Parameters parameters) {
        super(parameters);
        destinationFormat = new Context.StringFormat(parameters.destination);
    }

    @Override
    protected void transform(Context context, Path input) throws Exception {
        String destinationString = destinationFormat.format(context);
        Path destination = Paths.get(destinationString);
        Files.move(input, destination);
        emit(context.extend(this).description(destinationString), destination);
    }

    public static class Parameters {

        public String destination;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String destination) {
            this.destination = destination;
        }
    }
}
