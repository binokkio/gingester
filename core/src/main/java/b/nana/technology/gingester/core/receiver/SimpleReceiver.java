package b.nana.technology.gingester.core.receiver;

import b.nana.technology.gingester.core.Context;

import java.util.function.Consumer;

/**
 * Ignores context and target and passes given output through to `accept`.
 *
 * Useful for testing.
 */
public interface SimpleReceiver<T> extends Receiver<T>, Consumer<T> {

    @Override
    default void accept(Context context, T output) {
        accept(output);
    }

    @Override
    default void accept(Context context, T output, String target) {
        accept(output);
    }
}
