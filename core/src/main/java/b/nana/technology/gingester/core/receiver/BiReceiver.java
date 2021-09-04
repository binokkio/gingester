package b.nana.technology.gingester.core.receiver;

import b.nana.technology.gingester.core.context.Context;

/**
 * Ignores target and passes given context and output through to `accept`.
 *
 * Useful for testing.
 */
public interface BiReceiver<T> extends Receiver<T> {

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
