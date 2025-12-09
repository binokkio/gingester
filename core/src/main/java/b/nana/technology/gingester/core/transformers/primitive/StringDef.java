package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Names(1)
@Description("Create a string, optionally using Freemarker templating")
@Example(example = "'Hello, World!'", description = "Create a simple string")
@Example(example = "'Hello, ${target}!'", description = "Create a string greeting a `target` stashed by an upstream transformer")
@Example(example = "'{template: \"path/to/template.txt\", is: \"FILE\"}'", description = "Read template from file", test = false)
@Example(example = "'{template: \"path/to/template.txt\", is: \"RESOURCE\"}'", description = "Read template from Java resource", test = false)
public final class StringDef implements Transformer<Object, String> {

    private final Template template;

    public StringDef(Parameters parameters) {
        template = Context.newTemplate(parameters.template);
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws InterruptedException {
        out.accept(context, template.render(context, in));
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, template -> o("template", template));
                rule(JsonNode::isObject, object -> {
                    if (object.path("template").isTextual()) {
                        return o("template", object);
                    } else {
                        return object;
                    }
                });
            }
        }

        public TemplateParameters template;
    }
}
