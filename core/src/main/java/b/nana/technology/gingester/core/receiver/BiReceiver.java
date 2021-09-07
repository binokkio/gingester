package b.nana.technology.gingester.core.receiver;

import b.nana.technology.gingester.core.controller.Context;

/**
 * Ignores target and passes given context and output through to `accept`.
 *
 * Useful for testing.
 */
public interface BiReceiver<T> extends Receiver<T> {

    @Override
    default void accept(Context.Builder contextBuilder, T output) {
        accept(contextBuilder.build(null), output);
    }

    @Override
    default void accept(Context context, T output, String target) {
        accept(context, output);
    }

    @Override
    default void accept(Context.Builder contextBuilder, T output, String targetId) {
        accept(contextBuilder.build(null), output);
    }

    @Override
    default Context build(Context.Builder contextBuilder) {
        return contextBuilder.build(null);
    }
}
