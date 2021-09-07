package b.nana.technology.gingester.core.receiver;

import b.nana.technology.gingester.core.controller.Context;

public interface Receiver<O> {
    void accept(Context context, O output);
    void accept(Context.Builder contextBuilder, O output);
    void accept(Context context, O output, String targetId);
    void accept(Context.Builder contextBuilder, O output, String targetId);
    Context build(Context.Builder contextBuilder);
}
