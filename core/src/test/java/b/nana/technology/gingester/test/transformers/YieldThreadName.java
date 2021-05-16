package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

public class YieldThreadName extends Transformer<Object, String> {

    private final boolean limitMaxWorkers;

    public YieldThreadName(boolean limitMaxWorkers) {
        this.limitMaxWorkers = limitMaxWorkers;
    }

    @Override
    protected void setup(Setup setup) {
        if (limitMaxWorkers) setup.limitMaxWorkers(1);
    }

    @Override
    protected void transform(Context context, Object input) {
        emit(context, Thread.currentThread().getName());
    }
}
