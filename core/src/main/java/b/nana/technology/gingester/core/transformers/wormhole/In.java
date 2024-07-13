package b.nana.technology.gingester.core.transformers.wormhole;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.function.Consumer;

@Passthrough
public final class In implements Transformer<Object, Object> {

    private final FetchKey fetchWormhole = new FetchKey(Out.STASH_KEY);

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        @SuppressWarnings("unchecked")
        Consumer<Object> wormhole = (Consumer<Object>) context.require(fetchWormhole);
        wormhole.accept(in);
        out.accept(context, in);
    }
}
