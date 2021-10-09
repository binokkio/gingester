package b.nana.technology.gingester.transformers.base.common.string;

import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class CharsetTransformer<I, O> implements Transformer<I, O> {

    private final Charset charset;

    public CharsetTransformer(Parameters parameters) {
        charset = switchCharsetParameter(parameters.charset);
    }

    private Charset switchCharsetParameter(String charsetParameter) {
        switch (charsetParameter) {
            case "ISO‑8859‑1": return StandardCharsets.ISO_8859_1;
            case "US-ASCII": return StandardCharsets.US_ASCII;
            case "UTF-16": return StandardCharsets.UTF_16;
            case "UTF-16BE": return StandardCharsets.UTF_16BE;
            case "UTF-16LE": return StandardCharsets.UTF_16LE;
            case "UTF-8": return StandardCharsets.UTF_8;
            default: throw new IllegalArgumentException("No case for " + charsetParameter);
        }
    }

    protected Charset getCharset() {
        return charset;
    }

    public static class Parameters {
        public String charset = "UTF-8";
    }
}
