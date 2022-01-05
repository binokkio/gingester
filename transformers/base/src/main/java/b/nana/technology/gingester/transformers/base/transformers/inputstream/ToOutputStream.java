package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;

import java.io.InputStream;

@Pure
public final class ToOutputStream implements Transformer<InputStream, OutputStreamWrapper> {

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<OutputStreamWrapper> out) throws Exception {
        OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
        out.accept(context, outputStreamWrapper);
        in.transferTo(outputStreamWrapper);  // don't forget about the ByteArrayInputStream implementation when considering replacing this
        outputStreamWrapper.close();
    }
}
