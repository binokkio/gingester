package b.nana.technology.gingester.core.transformers.passthrough;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.function.BiConsumer;

@Passthrough
public final class BiConsumerPassthrough<T> implements Transformer<T, T> {

    private final BiConsumer<Context, T> biConsumer;

    public BiConsumerPassthrough(BiConsumer<Context, T> biConsumer) {
        this.biConsumer = biConsumer;
    }

    @Override
    public void transform(Context context, T in, Receiver<T> out) {
        biConsumer.accept(context, in);
        out.accept(context, in);
    }
}
