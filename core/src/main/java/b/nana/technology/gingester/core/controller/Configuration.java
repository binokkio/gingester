package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class Configuration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String id;
    private String transformer;
    private JsonNode parameters;
    private Boolean async;
    private List<String> links;
    private List<String> syncs;
    private List<String> excepts;
    private Integer maxQueueSize;
    private Integer maxWorkers;
    private Integer maxBatchSize;
    private Boolean report;

    @JsonIgnore
    private Transformer<?, ?> instance;


    @JsonCreator
    public Configuration() {}

    @JsonCreator
    public Configuration(String transformer) {
        this.transformer = transformer;
    }


    public String getId() {
        return id;
    }

    public Configuration id(String id) {
        this.id = id;
        return this;
    }


    public String getTransformer() {
        return transformer;
    }

    public Configuration transformer(String transformer) {
        this.transformer = transformer;
        return this;
    }

    public Configuration transformer(Transformer<?, ?> transformer) {
        this.transformer = TransformerFactory.getUniqueName(transformer);
        this.instance = transformer;
        return this;
    }

    public <T> Configuration transformer(Consumer<T> consumer) {
        transformer = "Consumer";
        instance = ((Transformer<T, T>) (context, in, out) -> {
            consumer.accept(in);
            out.accept(context, in);
        });
        return this;
    }


    public JsonNode getParameters() {
        return parameters;
    }

    public Configuration parameters(Object parameters) {
        this.parameters = OBJECT_MAPPER.valueToTree(parameters);
        return this;
    }


    public boolean getAsync() {
        return async != null && async;
    }

    public Configuration async(boolean async) {
        this.async = async;
        return this;
    }


    public List<String> getLinks() {
        return links == null ? Collections.singletonList("__maybe_next__") : links;
    }

    public Configuration links(List<String> links) {
        this.links = links;
        return this;
    }


    public List<String> getSyncs() {
        return syncs == null ? Collections.emptyList() : syncs;
    }

    public Configuration syncs(List<String> syncs) {
        this.syncs = syncs;
        return this;
    }


    public List<String> getExcepts() {
        return excepts == null ? Collections.emptyList() : excepts;
    }

    public Configuration excepts(List<String> excepts) {
        this.excepts = excepts;
        return this;
    }


    public int getMaxQueueSize() {
        return maxQueueSize == null ? 100 : maxQueueSize;
    }

    public Configuration maxQueueSize(Integer maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }


    public int getMaxWorkers() {
        return maxWorkers == null ? 4 : maxWorkers;
    }

    public Configuration maxWorkers(Integer maxWorkers) {
        this.maxWorkers = maxWorkers;
        return this;
    }


    public int getMaxBatchSize() {
        return maxBatchSize == null ? 50000 : maxBatchSize;
    }

    public Configuration maxBatchSize(Integer maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
        return this;
    }


    public Boolean report() {
        return report;
    }

    public Configuration report(boolean report) {
        this.report = report;
        return this;
    }


    public Optional<Transformer<?, ?>> getInstance() {
        return Optional.ofNullable(instance);
    }
}
