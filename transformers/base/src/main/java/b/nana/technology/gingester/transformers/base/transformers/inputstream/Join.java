package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Names(1)
public final class Join implements Transformer<InputStream, InputStream> {

    private final ContextMap<PipedOutputStream> contextMap = new ContextMap<>();
    private final byte[] delimiter;

    public Join(Parameters parameters) {
        delimiter = parameters.delimiter.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(Collections.singletonList("__seed__"));
        controls.requireOutgoingAsync();
    }

    @Override
    public void prepare(Context context, Receiver<InputStream> out) throws Exception {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        pipedOutputStream.connect(pipedInputStream);
        contextMap.put(context, pipedOutputStream);
        out.accept(context, pipedInputStream);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        contextMap.act(context, pipedOutputStream -> {
            in.transferTo(pipedOutputStream);
            if (delimiter.length > 0) pipedOutputStream.write(delimiter);
        });
    }

    @Override
    public void finish(Context context, Receiver<InputStream> out) throws Exception {
        contextMap.remove(context).close();
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
