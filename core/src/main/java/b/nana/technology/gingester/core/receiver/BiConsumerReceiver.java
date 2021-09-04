package b.nana.technology.gingester.core.receiver;

import b.nana.technology.gingester.core.context.Context;

import java.util.function.BiConsumer;

/**
 * Ignores target and passes given context and output through to `accept`.
 *
 * Useful for testing.
 */
public interface BiConsumerReceiver<T> extends Receiver<T>, BiConsumer<Context, T> {

    @Override
    default void accept(Context context, T output) {
        accept(context, output);
    }

    @Override
    default void accept(Context.Builder contextBuilder, T output) {
        accept(contextBuilder.build(), output);
    }

    @Override
    default void accept(Context context, T output, String target) {
        accept(context, output);
    }

    @Override
    default void accept(Context.Builder contextBuilder, T output, String targetId) {
        accept(contextBuilder.build(), output);
    }
}
