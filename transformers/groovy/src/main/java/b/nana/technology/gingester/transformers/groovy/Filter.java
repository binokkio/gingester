package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Names(1)
@Passthrough
public final class Filter extends SimpleScriptTransformer {

    public Filter(Parameters parameters) {
        super(parameters.script);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        Object result = getResult(context, in);
        boolean asBoolean;

        try {
            asBoolean = (boolean) result;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Filter script did not return a boolean but returned \"" + result + "\"");
        }

        if (asBoolean)
            out.accept(context, in);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, script -> o("script", script));
            }
        }

        public TemplateParameters script;
    }
}
