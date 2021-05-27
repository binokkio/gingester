package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.TimeBase;

import java.time.temporal.TemporalAccessor;

public class ToTime extends TimeBase<String, TemporalAccessor> {

    public ToTime(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, String input) throws Exception {
        emit(context, getFormatter().parse(input));
    }
}
