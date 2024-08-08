package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.cli.CliParser;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Example(example = "@ '{message: \"Hello, ${target}!\"}'", description = "Define a JSON object containing a message greeting `target` from stash")
public final class Def implements Transformer<Object, JsonNode> {

    private final TemplateMapper<JsonNode> jsonTemplate;

    public Def(Parameters parameters) {
        jsonTemplate = Context.newTemplateMapper(parameters.json, CliParser.OBJECT_MAPPER::readTree);
    }

    @Override
    public void transform(Context context, Object in, Receiver<JsonNode> out) {
        if (jsonTemplate.isInvariant()) {
            out.accept(context, jsonTemplate.requireInvariant().deepCopy());
        } else {
            out.accept(context, jsonTemplate.render(context, in));
        }
    }

    // TODO USING -l at the start of a flow causes unexpected issues
    // TODO USING -l at the start of a flow causes unexpected issues
    // TODO USING -l at the start of a flow causes unexpected issues
    // TODO USING -l at the start of a flow causes unexpected issues
    // TODO USING -l at the start of a flow causes unexpected issues
    // TODO USING -l at the start of a flow causes unexpected issues
    // TODO USING -l at the start of a flow causes unexpected issues
    // TODO USING -l at the start of a flow causes unexpected issues
    // TODO USING -l at the start of a flow causes unexpected issues

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"json"})
    public static class Parameters {
        public TemplateParameters json;
    }
}
