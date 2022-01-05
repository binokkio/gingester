package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Names(1)
public final class Join implements Transformer<InputStream, OutputStreamWrapper> {

    private final ContextMap<OutputStream> contextMap = new ContextMap<>();
    private final byte[] delimiter;

    public Join(Parameters parameters) {
        delimiter = parameters.delimiter.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(Collections.singletonList("__seed__"));
        controls.requireOutgoingSync();
    }

    @Override
    public void prepare(Context context, Receiver<OutputStreamWrapper> out) throws Exception {
        OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
        contextMap.put(context, outputStreamWrapper);
        out.accept(context, outputStreamWrapper);
    }

    @Override
    public void beforeBatch(Context context) {
        contextMap.lock(context);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<OutputStreamWrapper> out) throws Exception {
        OutputStream outputStream = contextMap.getLocked();
        in.transferTo(outputStream);
        if (delimiter.length > 0) outputStream.write(delimiter);
    }

    @Override
    public void afterBatch(Context context) {
        contextMap.unlock();
    }

    @Override
    public void finish(Context context, Receiver<OutputStreamWrapper> out) throws Exception {
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
