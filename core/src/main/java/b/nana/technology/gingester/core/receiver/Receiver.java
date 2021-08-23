package b.nana.technology.gingester.core.receiver;

import b.nana.technology.gingester.core.Context;

public interface Receiver<O> {

    void accept(Context context, O output);

    void accept(Context context, O output, String target);
}
