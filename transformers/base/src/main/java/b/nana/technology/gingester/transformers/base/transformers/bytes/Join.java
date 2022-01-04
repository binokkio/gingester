package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.Pype;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Names(1)
public final class Join implements Transformer<byte[], InputStream> {

    private final ContextMap<Pype> contextMap = new ContextMap<>();
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
        Pype pype = new Pype(delimiter);
        contextMap.put(context, pype);
        out.accept(context, pype);
    }

    @Override
    public void beforeBatch(Context context) {
        contextMap.lock(context);
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<InputStream> out) throws Exception {
        contextMap.getLocked().add(in);
    }

    @Override
    public void afterBatch(Context context) {
        contextMap.unlock();
    }

    @Override
    public void finish(Context context, Receiver<InputStream> out) throws Exception {
        contextMap.remove(context).addCloseSentinel();
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
