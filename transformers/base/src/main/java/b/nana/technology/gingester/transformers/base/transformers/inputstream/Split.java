package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.inputstream.Splitter;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Split implements Transformer<InputStream, InputStream> {

    private final byte[] delimiter;

    public Split(Parameters parameters) {
        delimiter = parameters.delimiter.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        Splitter splitter = new Splitter(in, delimiter);
        Optional<InputStream> split;
        long counter = 0;
        while ((split = splitter.getNextInputStream()).isPresent()) {
            out.accept(context.stash("description", counter++), split.get());
        }
    }

    public static class Parameters {

        public String delimiter = "\n";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String delimiter) {
            this.delimiter = delimiter;
        }
    }

}
