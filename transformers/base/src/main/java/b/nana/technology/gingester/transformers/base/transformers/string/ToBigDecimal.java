package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.math.BigDecimal;

@Pure
public final class ToBigDecimal implements Transformer<String, BigDecimal> {

    @Override
    public void transform(Context context, String in, Receiver<BigDecimal> out) {
        out.accept(context, new BigDecimal(in));
    }
}
