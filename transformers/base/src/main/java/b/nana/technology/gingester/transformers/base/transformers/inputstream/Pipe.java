package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@Deprecated
public final class Pipe implements Transformer<InputStream, InputStream> {

    private final int pipeSize;

    public Pipe(Parameters parameters) {
        pipeSize = parameters.pipeSize;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingAsync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(pipeSize);
        pipedOutputStream.connect(pipedInputStream);
        out.accept(context, pipedInputStream);
        in.transferTo(pipedOutputStream);
        pipedOutputStream.close();
    }

    public static class Parameters {

        public int pipeSize = 65536;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int pipeSize) {
            this.pipeSize = pipeSize;
        }
    }
}
