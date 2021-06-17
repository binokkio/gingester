package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.transformers.base.common.inputstream.Splitter;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Split extends Transformer<InputStream, InputStream> {

    private final byte[] delimiter;

    public Split(Parameters parameters) {
        super(parameters);
        delimiter = parameters.delimiter.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void setup(Setup setup) {
        setup.requireDownstreamSync();
    }

    @Override
    protected void transform(Context context, InputStream input) throws Exception {
        Splitter splitter = new Splitter(input, delimiter);
        Optional<InputStream> split;
        long counter = 0;
        while ((split = splitter.getNextInputStream()).isPresent()) {
            emit(context.extend(this).description(++counter), split.get());
        }
    }

    public static class Parameters {

        public String delimiter;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String delimiter) {
            this.delimiter = delimiter;
        }
    }

}
