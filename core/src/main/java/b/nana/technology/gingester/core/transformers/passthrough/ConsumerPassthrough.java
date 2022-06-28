package b.nana.technology.gingester.core.transformers.passthrough;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.function.Consumer;

@Passthrough
public final class ConsumerPassthrough<T> implements Transformer<T, T> {

    private final Consumer<T> consumer;

    public ConsumerPassthrough(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void transform(Context context, T in, Receiver<T> out) {
        consumer.accept(in);
        out.accept(context, in);
    }
}
