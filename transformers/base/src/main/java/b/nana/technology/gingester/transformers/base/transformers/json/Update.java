package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.cli.CliParser;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public final class Update implements Transformer<JsonNode, JsonNode> {

    private final TemplateMapper<JsonNode> jsonTemplate;

    public Update(Parameters parameters) {
        jsonTemplate = Context.newTemplateMapper(parameters.json, CliParser.OBJECT_MAPPER::readTree);
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws JsonMappingException {
        out.accept(context, CliParser.OBJECT_MAPPER.updateValue(in, jsonTemplate.render(context, in)));
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "json" })
    public static class Parameters {
        public TemplateParameters json;
    }
}
