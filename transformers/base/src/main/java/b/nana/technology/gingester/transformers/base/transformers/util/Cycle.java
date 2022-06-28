package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Names(1)
public final class Cycle implements Transformer<Object, Object> {

    private final List<Object> strings;
    private final AtomicInteger counter = new AtomicInteger();

    public Cycle(Parameters parameters) {
        strings = parameters;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(context, strings.get(counter.getAndIncrement() % strings.size()));
    }

    public static class Parameters extends ArrayList<Object> {

    }
}
