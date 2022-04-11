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

@Names(1)
public final class Join implements Transformer<InputStream, OutputStreamWrapper> {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final byte[] delimiter;

    public Join(Parameters parameters) {
        delimiter = parameters.delimiter.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void prepare(Context context, Receiver<OutputStreamWrapper> out) throws Exception {
        OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
        contextMap.put(context, new State(outputStreamWrapper));
        out.accept(context, outputStreamWrapper);
    }

    @Override
    public void beforeBatch(Context context) {
        contextMap.lock(context);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<OutputStreamWrapper> out) throws Exception {
        State state = contextMap.getLocked();
        if (delimiter.length > 0) {
            if (state.anythingWritten) {
                state.outputStream.write(delimiter);
            } else {
                state.anythingWritten = true;
            }
        }
        in.transferTo(state.outputStream);
    }

    @Override
    public void afterBatch(Context context) {
        contextMap.unlock();
    }

    @Override
    public void finish(Context context, Receiver<OutputStreamWrapper> out) throws Exception {
        contextMap.remove(context).outputStream.close();
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

    private static class State {

        private final OutputStream outputStream;
        private boolean anythingWritten;

        private State(OutputStream outputStream) {
            this.outputStream = outputStream;
        }
    }
}
