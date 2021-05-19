package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.transformers.jetty.common.RequestWrapper;

import java.io.InputStream;

public class ToInputStream extends Transformer<RequestWrapper, InputStream> {

    @Override
    protected void setup(Setup setup) {
        setup.syncOutputs();
    }

    @Override
    protected void transform(Context context, RequestWrapper input) throws Exception {
        emit(context, input.request.getInputStream());
    }
}
