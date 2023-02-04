package b.nana.technology.gingester.transformers.base.transformers.yaml;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Names(1)
public final class JsonToYaml implements Transformer<JsonNode, String> {

    private final ObjectMapper objectMapper;

    public JsonToYaml(Parameters parameters) {
        YAMLFactory yamlFactory = new YAMLFactory();
        parameters.features.forEach(yamlFactory::enable);
        objectMapper = new ObjectMapper(yamlFactory);
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<String> out) throws IOException {
        out.accept(context, objectMapper.writeValueAsString(in));
    }

    public static class Parameters {

        public List<YAMLGenerator.Feature> features = Collections.emptyList();

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(List<YAMLGenerator.Feature> features) {
            this.features = features;
        }
    }
}
