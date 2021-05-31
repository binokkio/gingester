package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.transformers.base.common.ToJsonBase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ToJson extends ToJsonBase<InputStream> {

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected InputStream toInputStream(InputStream input) {
        return input;
    }
}
