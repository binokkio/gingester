package b.nana.technology.gingester.core.link;

import b.nana.technology.gingester.core.Transformer;

public final class ExceptionLink extends BaseLink<Object, Throwable> {
    @SuppressWarnings("unchecked")
    public ExceptionLink(Transformer<?, ?> from, Transformer<Throwable, ?> to) {
        super((Transformer<?, Object>) from, to);
    }
}
