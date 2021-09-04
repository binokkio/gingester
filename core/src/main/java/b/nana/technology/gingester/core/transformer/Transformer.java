package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.controller.SetupControls;
import b.nana.technology.gingester.core.receiver.Receiver;

public interface Transformer<I, O> {

    default void setup(SetupControls controls) {}

    default void open() throws Exception {}

    default void prepare(Context context) throws Exception {}

    void transform(Context context, I in, Receiver<O> out) throws Exception;

    default void finish(Context context, Receiver<O> out) throws Exception {}

    default void close() throws Exception {}
}
