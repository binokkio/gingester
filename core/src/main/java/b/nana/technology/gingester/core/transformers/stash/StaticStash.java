package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Map;

@Names(1)
@Passthrough
public final class StaticStash implements Transformer<Object, Object> {

    private final Map<String, Object> stash;

    public StaticStash(Map<String, Object> stash) {
        this.stash = stash;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(context.stash(stash), in);
    }
}
