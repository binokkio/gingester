package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.Splitter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Description("Split InputStream on given delimiter")
@Example(example = "", description = "Split InputStream on default newline delimiter")
@Example(example = "DELIM", description = "Split InputStream on \"DELIM\"")
@Example(example = "1", description = "Split InputStream at most once on default newline delimiter")
@Example(example = "DELIM 2", description = "Split InputStream at most twice on \"DELIM\"")
public final class Split implements Transformer<InputStream, InputStream> {

    private final TemplateMapper<byte[]> delimiterTemplate;
    private final long maxSplits;

    public Split(Parameters parameters) {
        delimiterTemplate = Context.newTemplateMapper(parameters.delimiter, s -> s.getBytes(StandardCharsets.UTF_8));
        maxSplits = parameters.maxSplits;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        byte[] delimiter = delimiterTemplate.render(context);
        Splitter splitter = new Splitter(in, delimiter);
        Optional<InputStream> split;
        long counter = 0;
        while ((split = splitter.getNextInputStream()).isPresent()) {
            out.accept(context.stash("description", counter++), split.get());
            if (counter == maxSplits) {
                out.accept(context.stash("description", "remaining"), splitter.getRemaining());
                break;
            }
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, delimiter -> o("delimiter", delimiter));
                rule(JsonNode::isNumber, maxSplits -> o("maxSplits", maxSplits));
                rule(JsonNode::isArray, array -> o("delimiter", array.get(0), "maxSplits", array.get(1)));
            }
        }

        public TemplateParameters delimiter = new TemplateParameters("\n");
        public long maxSplits;
    }
}
