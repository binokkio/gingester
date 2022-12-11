package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.transformers.base.common.string.CharsetTransformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

@Names(1)
@Example(example = "1000", description = "Yield the first 1000 characters, stash complete inputstream as `stash`")
public final class PeekString extends CharsetTransformer<InputStream, String> {

    private final int bufferSize;
    private final String stashName;
    private final ThreadLocal<char[]> buffers;

    public PeekString(Parameters parameters) {
        super(parameters);
        bufferSize = parameters.length;
        stashName = parameters.stash;
        buffers = ThreadLocal.withInitial(() -> new char[bufferSize]);
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.ofOrdinal(stashName, "__input__");
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<String> out) throws Exception {

        BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
        bufferedInputStream.mark(bufferSize * 4);

        Reader reader = new InputStreamReader(bufferedInputStream, getCharset());

        char[] buffer = buffers.get();
        int total = 0;
        int read;
        while ((total != buffer.length && (read = reader.read(buffer, total, buffer.length - total)) != -1)) {
            total += read;
        }

        String string = new String(buffer, 0, total);

        bufferedInputStream.reset();

        out.accept(context.stash(stashName, bufferedInputStream), string);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters extends CharsetTransformer.Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, length -> o("length", length));
                rule(JsonNode::isArray, args -> o("length", args.get(0), "stash", args.get(1)));
            }
        }

        public int length;
        public String stash = "stash";
    }
}
