package b.nana.technology.gingester.transformers.base.transformers.base64;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;

public final class Decode implements Transformer<InputStream, OutputStreamWrapper> {

    private final Base64.Decoder decoder;
    private final ThreadLocal<BufferPair> bufferPairs;

    public Decode(Parameters parameters) {

        if (parameters.style.equals("basic")) {
            decoder = Base64.getDecoder();
        } else if (parameters.style.equals("url")) {
            decoder = Base64.getUrlDecoder();
        } else if (parameters.style.equals("mime")) {
            decoder = Base64.getMimeDecoder();
        } else {
            throw new IllegalArgumentException("Unknown style: \"" + parameters.style + "\"");
        }

        bufferPairs = ThreadLocal.withInitial(() ->
                new BufferPair(parameters.readBufferSize));
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<OutputStreamWrapper> out) throws Exception {

        try (OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper()) {

            out.accept(context, outputStreamWrapper);

            BufferPair buffers = bufferPairs.get();

            int readLength;
            while ((readLength = in.read(buffers.readBuffer)) == buffers.readBuffer.length) {
                int writeLength = decoder.decode(buffers.readBuffer, buffers.writeBuffer);
                outputStreamWrapper.write(buffers.writeBuffer, 0, writeLength);
            }

            if (readLength > 0 ) {
                byte[] remaining = Arrays.copyOf(buffers.readBuffer, readLength);
                int writeLength = decoder.decode(remaining, buffers.writeBuffer);
                outputStreamWrapper.write(buffers.writeBuffer, 0, writeLength);
            }
        }
    }

    public static class Parameters {

        public int readBufferSize = 8192;
        public String style = "basic";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String style) {
            this.style = style;
        }

        @JsonCreator
        public Parameters(int style) {
            this.style = Integer.toString(style);
        }
    }

    private static class BufferPair {

        byte[] readBuffer;
        byte[] writeBuffer;

        BufferPair(int readBufferSize) {
            readBuffer = new byte[readBufferSize];
            writeBuffer = new byte[readBufferSize * 2];
        }
    }
}
