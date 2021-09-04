package b.nana.technology.gingester.core.receiver;

import b.nana.technology.gingester.core.context.Context;

/**
 * Ignores context and target and passes given output through to `accept`.
 *
 * Useful for testing.
 */
public interface UniReceiver<T> extends Receiver<T> {

    @Override
    default void accept(Context context, T output) {
        accept(output);
    }

    @Override
    default void accept(Context.Builder contextBuilder, T output) {
        accept(output);
    }

    @Override
    default void accept(Context context, T output, String target) {
        accept(output);
    }

    @Override
    default void accept(Context.Builder contextBuilder, T output, String targetId) {
        accept(output);
    }

    void accept(T output);
}
