package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class Gunzip extends Transformer<InputStream, InputStream> {

    @Override
    protected void setup(Setup setup) {
        setup.syncOutputs();
    }

    @Override
    protected void transform(Context context, InputStream input) throws Exception {
        emit(context, new GZIPInputStream(input));
    }
}
