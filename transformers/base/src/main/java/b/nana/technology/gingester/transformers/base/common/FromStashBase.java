package b.nana.technology.gingester.transformers.base.common;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.transformers.base.transformers.Stash;

public abstract class FromStashBase<T> extends Transformer<Void, T> {

    private final String key;
    private final Class<T> stashedClass;

    public FromStashBase(Stash.Parameters parameters, Class<T> stashedClass) {
        super(parameters);
        this.key = parameters.key;
        this.stashedClass = stashedClass;
    }

    @Override
    protected final void transform(Context context, Void input) throws Exception {
        Object object = context.getDetail(key).orElseThrow(() -> new IllegalStateException("Nothing stashed under " + key));
        emit(context, stashedClass.cast(object));
    }
}
