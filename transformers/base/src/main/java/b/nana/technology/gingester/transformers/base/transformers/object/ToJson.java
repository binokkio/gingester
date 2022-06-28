package b.nana.technology.gingester.transformers.base.transformers.object;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Collections;
import java.util.List;

public final class ToJson implements Transformer<Object, JsonNode> {

    private final ObjectMapper objectMapper;

    public ToJson(Parameters parameters) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        parameters.enable.forEach(objectMapper::enable);
        parameters.disable.forEach(objectMapper::disable);
    }

    @Override
    public void transform(Context context, Object in, Receiver<JsonNode> out) {
        out.accept(context, objectMapper.valueToTree(in));
    }

    public static class Parameters {
        public List<SerializationFeature> enable = Collections.emptyList();
        public List<SerializationFeature> disable = Collections.emptyList();
    }
}
