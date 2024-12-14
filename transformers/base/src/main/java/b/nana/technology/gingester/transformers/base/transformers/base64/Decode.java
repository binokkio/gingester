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

        if (parameters.readBufferSize % 4 != 0)
            throw new IllegalArgumentException("readBufferSize must be a multiple of 4");

        switch (parameters.style) {
            case "basic" -> decoder = Base64.getDecoder();
            case "url" -> decoder = Base64.getUrlDecoder();
            case "mime" -> decoder = Base64.getMimeDecoder();
            default -> throw new IllegalArgumentException("Unknown style: \"" + parameters.style + "\"");
        }

        bufferPairs = ThreadLocal.withInitial(() ->
                new BufferPair(parameters.readBufferSize));
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<OutputStreamWrapper> out) throws Exception {

        try (OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper()) {

            out.accept(context, outputStreamWrapper);

            BufferPair buffers = bufferPairs.get();

            int offset = 0;
            int read;
            while ((read = in.read(buffers.readBuffer, offset, buffers.readBuffer.length - offset)) != -1) {
                offset += read;
                if (offset == buffers.readBuffer.length) {
                    int write = decoder.decode(buffers.readBuffer, buffers.writeBuffer);
                    outputStreamWrapper.write(buffers.writeBuffer, 0, write);
                    offset = 0;
                }
            }

            if (offset > 0 ) {
                byte[] remaining = Arrays.copyOf(buffers.readBuffer, offset);
                int write = decoder.decode(remaining, buffers.writeBuffer);
                outputStreamWrapper.write(buffers.writeBuffer, 0, write);
            }
        }
    }

    public static class Parameters {

        public int readBufferSize = 4000;
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
            writeBuffer = new byte[readBufferSize];
        }
    }
}
