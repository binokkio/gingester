package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.transformers.base.common.ToPathBase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ToPath extends ToPathBase<byte[]> {

    public ToPath(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected InputStream toInputStream(byte[] input) {
        return new ByteArrayInputStream(input);
    }
}