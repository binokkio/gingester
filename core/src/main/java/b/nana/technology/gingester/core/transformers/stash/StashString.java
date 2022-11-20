package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Names(1)
@Passthrough
public final class StashString implements Transformer<Object, Object>  {

    private final String stash;
    private final Template template;

    public StashString(Parameters parameters) {
        stash = parameters.stash;
        template = Context.newTemplate(parameters.template);
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.ofOrdinal(stash, String.class);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws InterruptedException {
        out.accept(context.stash(stash, template.render(context, in)), in);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, template -> o("stash", "stash", "template", template));
                rule(JsonNode::isArray, array -> o("stash", array.get(0), "template", array.get(1)));
            }
        }

        public String stash;
        public TemplateParameters template;
    }
}
