package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public final class Configuration {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    public static Configuration fromJson(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "Configuration.fromJson called with null InputStream");
        return OBJECT_MAPPER.readValue(inputStream, Configuration.class);
    }

    static Configuration fromGingester(Gingester gingester) {
        Configuration configuration = new Configuration();
        configuration.report = gingester.report;
        for (Transformer<?, ?> transformer : gingester.getTransformers()) {
            TransformerConfiguration transformerConfiguration = new TransformerConfiguration();
            transformerConfiguration.transformer = Provider.name(transformer);
            transformer.getName()
                    .filter(name -> !name.equals(transformerConfiguration.transformer))
                    .ifPresent(name -> transformerConfiguration.id = name);
            if (transformer.parameters != null) {
                transformerConfiguration.parameters = OBJECT_MAPPER.valueToTree(transformer.parameters);
            }
            transformer.outputs.stream()
                    .map(LinkConfiguration::new)
                    .forEach(transformerConfiguration.links::add);
            transformer.syncs.stream()
                    .map(t -> t.getName().orElseGet(() -> Provider.name(t)))
                    .forEach(transformerConfiguration.syncs::add);
            configuration.transformers.add(transformerConfiguration);
        }
        return configuration;
    }

    public Integer maxWorkers;
    public boolean report = true;
    public List<HostConfiguration> hosts = new ArrayList<>();
    public List<TransformerConfiguration> transformers = new ArrayList<>();

    public String toJson() {
        try {
            return OBJECT_WRITER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Gingester.Builder toBuilder() {
        Gingester.Builder gBuilder = new Gingester.Builder();
        appendToBuilder(gBuilder);
        return gBuilder;
    }

    void appendToBuilder(Gingester.Builder gBuilder) {

        gBuilder.report(report);

        for (TransformerConfiguration transformerConfiguration : transformers) {
            Transformer<?, ?> transformer = Provider.instance(transformerConfiguration.transformer, transformerConfiguration.parameters);
            transformer.apply(transformerConfiguration);
            if (transformerConfiguration.id != null) gBuilder.name(transformerConfiguration.id, transformer);
            else gBuilder.add(transformer);
        }

        for (TransformerConfiguration transformerConfiguration : transformers) {
            String fromName = transformerConfiguration.id != null ? transformerConfiguration.id : transformerConfiguration.transformer;
            for (LinkConfiguration linkConfiguration : transformerConfiguration.links) {
                Link<?> link = gBuilder.link(fromName, linkConfiguration.to);
                if (linkConfiguration.sync) link.sync();
            }
        }

        for (TransformerConfiguration transformerConfiguration : transformers) {
            String fromName = transformerConfiguration.id != null ? transformerConfiguration.id : transformerConfiguration.transformer;
            for (String toName : transformerConfiguration.syncs) {
                gBuilder.sync(fromName, toName);
            }
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
        public List<LinkConfiguration> links = new ArrayList<>();
        public List<String> syncs = new ArrayList<>();
    }

    static class LinkConfiguration {

        public String to;
        public boolean sync;

        @JsonCreator
        public LinkConfiguration() {

        }

        @JsonCreator
        public LinkConfiguration(String to) {
            this.to = to;
        }

        public LinkConfiguration(Link<?> link) {
            to = link.to.getName().orElseThrow();
            sync = link.isSyncModeExplicit();
        }

        @JsonValue
        public JsonNode getJsonValue() {
            if (sync) {
                ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                objectNode.put("to", to);
                objectNode.put("sync", sync);
                return objectNode;
            } else {
                return JsonNodeFactory.instance.textNode(to);
            }
        }
    }
}
