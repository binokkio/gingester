package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.PrefixInputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.util.Arrays;

@Names(1)
@Example(example = "1000", description = "Yield the first 1000 bytes, stash complete inputstream at `stash`")
public final class PeekBytes implements Transformer<InputStream, byte[]> {

    private final int bufferSize;
    private final boolean reuseBuffer;
    private final String stashName;
    private final ThreadLocal<byte[]> buffers;

    public PeekBytes(Parameters parameters) {
        bufferSize = parameters.bytes;
        reuseBuffer = parameters.reuseBuffer;
        stashName = parameters.stash;
        buffers = !reuseBuffer ? null : ThreadLocal.withInitial(() -> new byte[bufferSize]);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<byte[]> out) throws Exception {

        byte[] buffer = reuseBuffer ? buffers.get() : new byte[bufferSize];

        int total = 0;
        int read;
        while ((total != buffer.length && (read = in.read(buffer, total, buffer.length - total)) != -1)) {
            total += read;
        }

        if (total < buffer.length)
            buffer = Arrays.copyOfRange(buffer, 0, total);

        PrefixInputStream prefixInputStream = new PrefixInputStream(in);
        prefixInputStream.prefix(buffer);  // risky, buffer might get modified downstream

        out.accept(context.stash(stashName, prefixInputStream), buffer);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, bytes -> o("bytes", bytes));
                rule(JsonNode::isArray, args -> {
                    ObjectNode result = o("bytes", args.get(0));
                    if (args.get(1).isBoolean()) {
                        result.set("reuseBuffer", args.get(1));
                        if (args.has(2)) result.set("stash", args.get(2));
                    } else {
                        result.set("stash", args.get(1));
                    }
                    return result;
                });
            }
        }

        public int bytes;
        public boolean reuseBuffer = false;
        public String stash = "stash";
    }
}
