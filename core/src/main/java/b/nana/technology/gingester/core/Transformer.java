package b.nana.technology.gingester.core;

import java.util.function.BiConsumer;

public interface Transformer<I, O> {

    default void open() throws Exception {}

    default void prepare(Context context) throws Exception {}

    void transform(Context context, I input, BiConsumer<Context, O> output) throws Exception;

    default void finish(Context context) throws Exception {}

    default void close() throws Exception {}
}
