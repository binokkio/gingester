package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.link.BaseLink;
import b.nana.technology.gingester.core.link.ExceptionLink;
import b.nana.technology.gingester.core.link.NormalLink;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Configuration {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer(new Printer());

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
            transformer.getId()
                    .filter(name -> !name.equals(transformerConfiguration.transformer))
                    .ifPresent(name -> transformerConfiguration.id = name);

            if (transformer.report != Transformer.DEFAULT_REPORT_VALUE) transformerConfiguration.report = transformer.report;
            if (transformer.maxWorkers != Transformer.DEFAULT_MAX_WORKERS) transformerConfiguration.maxWorkers = transformer.maxWorkers;
            if (transformer.maxBatchSize != Transformer.DEFAULT_MAX_BATCH_SIZE) transformerConfiguration.maxBatchSize = transformer.maxBatchSize;

            if (transformer.parameters != null) {
                // TODO if equal to newly constructed parameters without parameters ignore
                transformerConfiguration.parameters = OBJECT_MAPPER.valueToTree(transformer.parameters);
            }

            List<LinkConfiguration> links = transformer.outgoing.stream()
                    .filter(link -> !link.isImplied())
                    .map(LinkConfiguration::new)
                    .collect(Collectors.toList());
            if (!links.isEmpty()) transformerConfiguration.links = links;

            List<String> syncs = transformer.syncs.stream()
                    .map(t -> t.getId().orElseGet(() -> Provider.name(t)))
                    .collect(Collectors.toList());
            if (!syncs.isEmpty()) transformerConfiguration.syncs = syncs;

            List<LinkConfiguration> excepts = transformer.excepts.stream()
                    .filter(link -> !link.isImplied())
                    .map(LinkConfiguration::new)
                    .collect(Collectors.toList());
            if (!excepts.isEmpty()) transformerConfiguration.excepts = excepts;

            configuration.transformers.add(transformerConfiguration);
        }

        return configuration;
    }

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
        Builder gBuilder = new Builder();
        appendToBuilder(gBuilder);
        return gBuilder;
    }

    void appendToBuilder(Builder gBuilder) {

        gBuilder.report(report);

        Transformer<?, ?> autoLinkNext = null;
        for (TransformerConfiguration transformerConfiguration : transformers) {
            Transformer<?, ?> transformer = Provider.instance(transformerConfiguration.transformer, transformerConfiguration.parameters);
            transformer.apply(transformerConfiguration);
            if (transformerConfiguration.id != null) gBuilder.id(transformerConfiguration.id, transformer);
            else gBuilder.add(transformer);
            if (autoLinkNext != null) {
                gBuilder.linkUnchecked(autoLinkNext, transformer).markImplied();
                autoLinkNext = null;
            }
            if (transformerConfiguration.links == null && transformer.getLinks().isEmpty()) {
                autoLinkNext = transformer;
            }
        }

        for (TransformerConfiguration transformerConfiguration : transformers) {
            String transformerName = transformerConfiguration.id != null ? transformerConfiguration.id : transformerConfiguration.transformer;
            if (transformerConfiguration.links != null) {
                for (LinkConfiguration linkConfiguration : transformerConfiguration.links) {
                    NormalLink<?> link = gBuilder.link(transformerName, linkConfiguration.to);
                    if (linkConfiguration.async != null) {
                        if (linkConfiguration.async) link.async();
                        else link.sync();
                    }
                }
            }
            if (transformerConfiguration.excepts != null) {
                for (LinkConfiguration linkConfiguration : transformerConfiguration.excepts) {
                    ExceptionLink link = gBuilder.except(transformerName, linkConfiguration.to);
                    if (linkConfiguration.async != null) {
                        if (linkConfiguration.async) link.async();
                        else link.sync();
                    }
                }
            }
        }

        for (TransformerConfiguration transformerConfiguration : transformers) {
            String fromName = transformerConfiguration.id != null ? transformerConfiguration.id : transformerConfiguration.transformer;
            if (transformerConfiguration.syncs != null) {
                for (String toName : transformerConfiguration.syncs) {
                    gBuilder.sync(fromName, toName);
                }
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
        public Boolean report;
        public Integer maxWorkers;
        public Integer maxBatchSize;
        public JsonNode parameters;
        public List<String> hosts;
        public List<LinkConfiguration> links;
        public List<String> syncs;
        public List<LinkConfiguration> excepts;

        @JsonCreator
        public TransformerConfiguration() {}

        @JsonCreator
        public TransformerConfiguration(String transformer) {
            this.transformer = transformer;
        }

        @JsonValue
        public JsonNode getJsonValue() {
            if (Stream.of(id, report, maxWorkers, maxBatchSize, parameters, hosts, links, syncs, excepts).allMatch(Objects::isNull)) {
                return JsonNodeFactory.instance.textNode(transformer);
            } else {
                // TODO find a less cumbersome solution
                ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                if (id != null) objectNode.put("id", id);
                if (transformer != null) objectNode.put("transformer", transformer);
                if (report != null) objectNode.put("report", report);
                if (maxWorkers != null) objectNode.put("maxWorkers", maxWorkers);
                if (maxBatchSize != null) objectNode.put("maxBatchSize", maxBatchSize);
                if (parameters != null) objectNode.set("parameters", parameters);
                if (hosts != null) objectNode.set("hosts", JsonNodeFactory.instance.pojoNode(hosts));
                if (links != null) objectNode.set("links", JsonNodeFactory.instance.pojoNode(links));
                if (syncs != null) objectNode.set("syncs", JsonNodeFactory.instance.pojoNode(syncs));
                if (excepts != null) objectNode.set("excepts", JsonNodeFactory.instance.pojoNode(excepts));
                return objectNode;
            }
        }
    }

    static class LinkConfiguration {

        public String to;
        public Boolean async;

        @JsonCreator
        public LinkConfiguration() {}

        @JsonCreator
        public LinkConfiguration(String to) {
            this.to = to;
        }

        public LinkConfiguration(BaseLink<?, ?> link) {
            to = link.to.getId().orElseThrow();
            if (link.isSyncModeExplicit()) {
                async = !link.isSync();
            }
        }

        @JsonValue
        public JsonNode getJsonValue() {
            if (async == null) {
                return JsonNodeFactory.instance.textNode(to);
            } else {
                // TODO find a less cumbersome solution
                ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                if (to != null) objectNode.put("to", to);
                if (async != null) objectNode.put("async", async);
                return objectNode;
            }
        }
    }

    private static class Printer extends DefaultPrettyPrinter {

        Printer() {
            _objectFieldValueSeparatorWithSpaces = ": ";
            _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
            return new Printer();
        }
    }
}
