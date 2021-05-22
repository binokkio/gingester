package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class Configuration {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    public static Configuration fromJson(InputStream inputStream) throws IOException {
        return OBJECT_MAPPER.readValue(inputStream, Configuration.class);
    }

    static Configuration fromGingester(Gingester gingester) {
        Configuration configuration = new Configuration();
        for (Transformer<?, ?> transformer : gingester.getTransformers()) {
            TransformerConfiguration transformerConfiguration = new TransformerConfiguration();
            transformerConfiguration.transformer = Provider.name(transformer);
            gingester.getName(transformer)
                    .filter(name -> !name.equals(transformerConfiguration.transformer))
                    .ifPresent(name -> transformerConfiguration.id = name);
            if (transformer.parameters != null) {
                transformerConfiguration.parameters = OBJECT_MAPPER.valueToTree(transformer.parameters);
            }
            transformer.outputs.stream()
                    .map(link -> link.to)
                    .map(t -> gingester.getName(t).orElseGet(() -> Provider.name(t)))
                    .forEach(transformerConfiguration.links::add);
            transformer.syncs.stream()
                    .map(t -> gingester.getName(t).orElseGet(() -> Provider.name(t)))
                    .forEach(transformerConfiguration.syncs::add);
            configuration.transformers.add(transformerConfiguration);
        }
        return configuration;
    }

    public Integer maxWorkers;
    public List<HostConfiguration> hosts = new ArrayList<>();
    public List<TransformerConfiguration> transformers = new ArrayList<>();

    public String toJson() {
        try {
            return OBJECT_WRITER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String hash() {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(toJson().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);  // base64 vs base16 is not relevant here
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Gingester build() {

        Gingester gingester = new Gingester();

        for (TransformerConfiguration transformerConfiguration : transformers) {
            String name = transformerConfiguration.id != null ? transformerConfiguration.id : transformerConfiguration.transformer;
            Transformer<?, ?> transformer = Provider.instance(transformerConfiguration.transformer, transformerConfiguration.parameters);
            transformer.apply(transformerConfiguration);
            gingester.name(name, transformer);
        }

        for (TransformerConfiguration transformerConfiguration : transformers) {
            String fromName = transformerConfiguration.id != null ? transformerConfiguration.id : transformerConfiguration.transformer;
            for (String toName : transformerConfiguration.links) {
                gingester.link(fromName, toName);
            }
        }

        for (TransformerConfiguration transformerConfiguration : transformers) {
            String fromName = transformerConfiguration.id != null ? transformerConfiguration.id : transformerConfiguration.transformer;
            for (String toName : transformerConfiguration.syncs) {
                gingester.sync(fromName, toName);
            }
        }

        return gingester;
    }

    static class HostConfiguration {
        public String id;
        public String name;
        public int port;
    }

    static class TransformerConfiguration {
        public String id;
        public String transformer;
        public Integer maxWorkers;
        public JsonNode parameters;
        public List<String> hosts = new ArrayList<>();
        public List<String> links = new ArrayList<>();
        public List<String> syncs = new ArrayList<>();
    }
}
