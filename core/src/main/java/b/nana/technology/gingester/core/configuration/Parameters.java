package b.nana.technology.gingester.core.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class Parameters {

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


    @JsonCreator
    public Parameters() {}

    @JsonCreator
    public Parameters(String transformer) {
        this.transformer = transformer;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getTransformer() {
        return transformer;
    }

    public void setTransformer(String transformer) {
        this.transformer = transformer;
    }


    public JsonNode getParameters() {
        return parameters;
    }

    public void setParameters(Object parameters) {
        this.parameters = OBJECT_MAPPER.valueToTree(parameters);
    }


    public boolean getAsync() {
        return async != null && async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }


    public List<String> getLinks() {
        return links == null ? Collections.singletonList("__maybe_next__") : links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }


    public List<String> getSyncs() {
        return syncs == null ? Collections.emptyList() : syncs;
    }

    public void setSyncs(List<String> syncs) {
        this.syncs = syncs;
    }


    public List<String> getExcepts() {
        return excepts == null ? Collections.emptyList() : excepts;
    }

    public void setExcepts(List<String> excepts) {
        this.excepts = excepts;
    }


    public int getMaxQueueSize() {
        return maxQueueSize == null ? 100 : maxQueueSize;
    }

    public void setMaxQueueSize(Integer maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }


    public int getMaxWorkers() {
        return maxWorkers == null ? 1 : maxWorkers;
    }

    public void setMaxWorkers(Integer maxWorkers) {
        this.maxWorkers = maxWorkers;
    }


    public int getMaxBatchSize() {
        return maxBatchSize == null ? 65536 : maxBatchSize;
    }

    public void setMaxBatchSize(Integer maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }
}
