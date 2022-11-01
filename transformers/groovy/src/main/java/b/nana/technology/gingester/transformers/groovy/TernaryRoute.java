package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;

@Names(1)
@Passthrough
public final class TernaryRoute extends SimpleScriptTransformer {

    private final String thenRoute;
    private final String otherwiseRoute;

    public TernaryRoute(Parameters parameters) {
        super(parameters.test);
        thenRoute = parameters.then;
        otherwiseRoute = parameters.otherwise;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.links(List.of(thenRoute, otherwiseRoute));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        Object result = getResult(context, in);

        try {
            boolean asBoolean = (boolean) result;
            if (asBoolean)
                out.accept(context, in, thenRoute);
            else
                out.accept(context, in, otherwiseRoute);
        } catch (ClassCastException e) {
            throw new IllegalStateException("TernaryRoute script did not return a boolean but returned \"" + result + "\"");
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isArray, array -> am((ArrayNode) array, "test", "then", "otherwise"));
            }
        }

        public TemplateParameters test;
        public String then;
        public String otherwise;
    }
}
