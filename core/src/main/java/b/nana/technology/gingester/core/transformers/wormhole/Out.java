package b.nana.technology.gingester.core.transformers.wormhole;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Passthrough
public final class Out implements Transformer<Object, Object> {

    static final String STASH_KEY = "wormhole";

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        AtomicBoolean wormholeClosed = new AtomicBoolean();

        // create the capsule and wormhole
        Object[] capsule = new Object[1];
        Consumer<Object> wormhole = traveler -> {

            if (wormholeClosed.get())
                throw new IllegalStateException("Wormhole closed");

            if (capsule[0] != null)  // we could use an unbounded capsule instead
                throw new IllegalStateException("Wormhole capsule already filled");

            capsule[0] = traveler;
        };

        // add wormhole to context and passthrough `in`
        out.accept(
                context.stash(STASH_KEY, wormhole),
                in
        );

        // keep checking and yielding travelers from the capsule
        while (capsule[0] != null) {
            Object next = capsule[0];
            capsule[0] = null;
            out.accept(
                    context.stash(STASH_KEY, wormhole),
                    next
            );
        }

        wormholeClosed.set(true);
    }
}
