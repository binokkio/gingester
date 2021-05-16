package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.transformers.base.common.ToJsonBase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ToJson extends ToJsonBase<byte[]> {

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected InputStream toInputStream(byte[] input) {
        return new ByteArrayInputStream(input);
    }
}
