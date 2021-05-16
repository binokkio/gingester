package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.transformers.base.common.ToJsonBase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ToJson extends ToJsonBase<String> {

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected InputStream toInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }
}
