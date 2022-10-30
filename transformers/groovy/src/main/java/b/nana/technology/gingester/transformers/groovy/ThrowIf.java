package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformers.Throw;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Names(1)
@Passthrough
public final class ThrowIf extends SimpleScriptTransformer {

    private final Template messageTemplate;

    public ThrowIf(Parameters parameters) {
        super(parameters.script);
        messageTemplate = Context.newTemplate(parameters.message);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Throw.FlowException {

        Object result = getResult(context, in);
        boolean asBoolean;

        try {
            asBoolean = (boolean) result;
        } catch (ClassCastException e) {
            throw new IllegalStateException("ThrowIf script did not return a boolean but returned \"" + result + "\"");
        }

        if (asBoolean)
            throw new Throw.FlowException(messageTemplate.render(context));

        out.accept(context, in);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isArray, array -> am((ArrayNode) array, "message", "script"));
            }
        }

        public TemplateParameters message;
        public TemplateParameters script;
    }
}
