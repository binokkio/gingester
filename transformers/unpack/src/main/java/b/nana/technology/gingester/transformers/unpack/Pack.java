package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import b.nana.technology.gingester.transformers.unpack.packers.Packer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@Names(1)
public final class Pack implements Transformer<byte[], OutputStreamWrapper> {

    private final ContextMap<Packer> contextMap = new ContextMap<>();

    private final Packer.Type type;
    private final Template entryNameTemplate;

    public Pack(Parameters parameters) {
        type = parameters.type;
        entryNameTemplate = Context.newTemplate(parameters.entry);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void prepare(Context context, Receiver<OutputStreamWrapper> out) {
        OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
        out.accept(context, outputStreamWrapper);
        Packer packer = type.create(outputStreamWrapper);
        contextMap.put(context, packer);
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<OutputStreamWrapper> out) throws Exception {
        String entryName = entryNameTemplate.render(context, in);
        contextMap.act(context, archiver -> archiver.add(entryName, in));
    }

    @Override
    public void finish(Context context, Receiver<OutputStreamWrapper> out) throws IOException {
        contextMap.remove(context).close();
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, entry -> o("entry", entry));
                rule(JsonNode::isArray, array -> o("type", array.get(0), "entry", array.get(1)));
            }
        }

        public Packer.Type type = Packer.Type.TAR;
        public TemplateParameters entry;
    }
}
