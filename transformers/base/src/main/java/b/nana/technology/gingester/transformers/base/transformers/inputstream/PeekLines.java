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
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Names(1)
@Example(example = "1", description = "Yield the first line, stash complete inputstream as `stash`")
public final class PeekLines extends CharsetTransformer<InputStream, String> {

    private final int numLines;
    private final int bufferSize;
    private final String stashName;

    public PeekLines(Parameters parameters) {
        super(parameters);
        numLines = parameters.lines;
        bufferSize = parameters.bufferSize;
        stashName = parameters.stash;
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.ofOrdinal(stashName, "__input__");
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<String> out) throws Exception {

        BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
        bufferedInputStream.mark(bufferSize * 4);

        BufferedReader reader = new BufferedReader(new InputStreamReader(bufferedInputStream, getCharset()));

        StringBuilder lines = new StringBuilder();
        String line;
        for (int i = 0; i < numLines && (line = reader.readLine()) != null; i++) {
            lines.append(line);
            lines.append('\n');
        }

        // drop trailing newline
        if (lines.length() > 0)
            lines.deleteCharAt(lines.length() - 1);

        bufferedInputStream.reset();

        out.accept(context.stash(stashName, bufferedInputStream), lines.toString());
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters extends CharsetTransformer.Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, lines -> o("lines", lines));
                rule(JsonNode::isArray, args -> {
                    ObjectNode result = o("lines", args.get(0));
                    if (args.get(1).isInt()) {
                        result.set("bufferSize", args.get(1));
                        if (args.has(2)) result.set("stash", args.get(2));
                    } else {
                        result.set("stash", args.get(1));
                    }
                    return result;
                });
            }
        }

        public int lines;
        public int bufferSize = 8192;
        public String stash = "stash";
    }
}
