package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.transformers.base.common.ToPathBase;

import java.io.InputStream;

public class ToPath extends ToPathBase<InputStream> {

    public ToPath(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected InputStream toInputStream(InputStream input) {
        return input;
    }
}
