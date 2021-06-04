package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.util.Map;

public class Exceptions extends Transformer<Throwable, Void> {

    @Override
    protected void transform(Context context, Throwable input) {
        emit(context.extend(this).stash(Map.of("exception", input)), null);
    }
}
