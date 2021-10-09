package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class TransformerConfiguration extends BaseConfiguration<TransformerConfiguration> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String id;
    private String transformer;
    private JsonNode parameters;
    private Boolean report;

    @JsonIgnore
    private Transformer<?, ?> instance;



    @JsonCreator
    public TransformerConfiguration() {}

    @JsonCreator
    public TransformerConfiguration(String transformer) {
        this.transformer = transformer;
    }



    public TransformerConfiguration id(String id) {
        this.id = id;
        return this;
    }

    public TransformerConfiguration transformer(String transformer) {
        this.transformer = transformer;
        return this;
    }

    public TransformerConfiguration transformer(Transformer<?, ?> transformer) {
        this.transformer = TransformerFactory.getUniqueName(transformer);
        this.instance = transformer;
        return this;
    }

    public <T> TransformerConfiguration transformer(Consumer<T> consumer) {
        transformer = "Consumer";
        instance = ((Transformer<T, T>) (context, in, out) -> {
            consumer.accept(in);
            out.accept(context, in);
        });
        return this;
    }

    public <T> TransformerConfiguration transformer(BiConsumer<Context, T> consumer) {
        transformer = "Consumer";
        instance = ((Transformer<T, T>) (context, in, out) -> {
            consumer.accept(context, in);
            out.accept(context, in);
        });
        return this;
    }

    public TransformerConfiguration parameters(Object parameters) {
        this.parameters = OBJECT_MAPPER.valueToTree(parameters);
        return this;
    }

    public TransformerConfiguration jsonParameters(String json) {
        try {
            parameters = GingesterConfiguration.OBJECT_READER.readTree(json);
        } catch (JsonProcessingException e) {
            parameters = JsonNodeFactory.instance.textNode(json);
        }
        return this;
    }

    public TransformerConfiguration report(boolean report) {
        this.report = report;
        return this;
    }



    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(transformer);
    }

    public Optional<Transformer<?, ?>> getInstance() {
        return Optional.ofNullable(instance);
    }

    public Optional<JsonNode> getParameters() {
        return Optional.ofNullable(parameters);
    }

    public Optional<Boolean> getReport() {
        return Optional.ofNullable(report);
    }
}
