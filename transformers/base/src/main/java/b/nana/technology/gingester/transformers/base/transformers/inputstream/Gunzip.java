package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.SetupControls;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class Gunzip implements Transformer<InputStream, InputStream> {

    @Override
    public void setup(SetupControls controls) {
        controls.requireDownstreamSync = true;
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        out.accept(context, new GZIPInputStream(in));
    }
}
