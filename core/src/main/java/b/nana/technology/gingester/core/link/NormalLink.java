package b.nana.technology.gingester.core.link;

import b.nana.technology.gingester.core.Transformer;

public final class NormalLink<T> extends BaseLink<T, T> {
    public NormalLink(Transformer<?, T> from, Transformer<? super T, ?> to) {
        super(from, to);
    }
}
