package b.nana.technology.gingester.transformers.base.transformers.base64;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public final class Encode implements Transformer<InputStream, OutputStreamWrapper> {

    private final Base64.Encoder encoder;
    private final ThreadLocal<BufferPair> bufferPairs;

    public Encode(Parameters parameters) {

        if (parameters.readBufferSize % 3 != 0)
            throw new IllegalArgumentException("readBufferSize must be a multiple of 3");

        if (parameters.style.equals("basic")) {
            encoder = Base64.getEncoder();
        } else if (parameters.style.equals("url")) {
            encoder = Base64.getUrlEncoder();
        } else {
            try {
                int lineLength = Integer.parseInt(parameters.style);
                encoder = Base64.getMimeEncoder(lineLength, "\r\n".getBytes(StandardCharsets.UTF_8));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unknown style: \"" + parameters.style + "\"");
            }
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
                    int write = encoder.encode(buffers.readBuffer, buffers.writeBuffer);
                    outputStreamWrapper.write(buffers.writeBuffer, 0, write);
                    offset = 0;
                }
            }

            if (offset > 0 ) {
                byte[] remaining = Arrays.copyOf(buffers.readBuffer, offset);
                int write = encoder.encode(remaining, buffers.writeBuffer);
                outputStreamWrapper.write(buffers.writeBuffer, 0, write);
            }
        }
    }

    public static class Parameters {

        public int readBufferSize = 3000;
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
            writeBuffer = new byte[readBufferSize * 2];  // allow for 3/4 encoding ratio and newlines overhead
        }
    }
}
