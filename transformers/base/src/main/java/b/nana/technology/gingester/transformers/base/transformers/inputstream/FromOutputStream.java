package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@Pure
public final class FromOutputStream implements Transformer<OutputStreamWrapper, InputStream> {

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingAsync();
    }

    @Override
    public void transform(Context context, OutputStreamWrapper in, Receiver<InputStream> out) throws Exception {
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(65536);
        pipedOutputStream.connect(pipedInputStream);
        in.wrap(pipedOutputStream);
        out.accept(context, pipedInputStream);
    }
}
