package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.controller.Held;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.ContextPlus;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.template.TemplateType;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Names(1)
public final class Gcli implements Transformer<Object, Object> {

    private final List<BiFunction<Context, Object, String>> segments;
    private final boolean giveContext;
    private final Map<String, Object> kwargs;

    private Held held;

    public Gcli(Parameters parameters) {

        if (parameters.segments.isEmpty())
            throw new IllegalArgumentException("Gcli transformer needs at least 1 segment");

        segments = parameters.segments.stream()
                .map(Gcli::getGcliSupplier)
                .collect(Collectors.toList());

        giveContext = parameters.giveContext;

        kwargs = parameters.kwargs;
    }

    @Override
    public void setup(SetupControls controls) {
        this.held = controls.getHeld();
    }

    private static BiFunction<Context, Object, String> getGcliSupplier(SourceParameters sourceParameters) {
        switch (sourceParameters.is) {

            case FILE:
                return Context.newTemplateMapper(
                        new TemplateParameters(sourceParameters.source),
                        s -> Files.readString(Paths.get(s))
                )::render;

            case HOT_FILE:
                Template pathTemplate = Context.newTemplate(new TemplateParameters(sourceParameters.source));
                return (c, i) -> {
                    try {
                        return Files.readString(Paths.get(pathTemplate.render(c, i)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

            case RESOURCE:
                return Context.newTemplateMapper(
                        new TemplateParameters(sourceParameters.source),
                        s -> new String(Gcli.class.getResourceAsStream(s).readAllBytes())
                )::render;

            case STASH:
                FetchKey fk = new FetchKey(sourceParameters.source);
                return (c, i) -> {
                    Object value = c.require(fk);
                    return value instanceof TextNode ? ((TextNode) value).asText() : value.toString();
                };

            case STRING:
                return Context.newTemplate(new TemplateParameters(sourceParameters.source))::render;

            default: throw new IllegalStateException("No case for " + sourceParameters.is);
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        FlowBuilder flowBuilder = new FlowBuilder(held).seedValue(in);
        if (giveContext) flowBuilder.parentContext(context);
        if (kwargs.isEmpty()) {
            ContextPlus contextPlus = new ContextPlus(context, in);
            segments.forEach(gs -> flowBuilder.cli(gs.apply(context, in), contextPlus));
        } else {
            Context contextWithKwargs = context.stash(kwargs).buildForSelf();
            ContextPlus contextPlus = new ContextPlus(contextWithKwargs, in);
            segments.forEach(gs -> flowBuilder.cli(gs.apply(contextWithKwargs, in), contextPlus));
        }
        flowBuilder.add(o -> out.accept(context, o)).run();
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, NormalizingDeserializer::a);
                rule(JsonNode::isObject, o -> o.has("source") ? a(o) : o);
                rule(JsonNode::isArray, array -> {
                    JsonNode last = array.get(array.size() - 1);
                    if (last.isObject() && !last.has("source")) {
                        ((ArrayNode) array).remove(array.size() - 1);
                        return o("segments", array, "kwargs", last);
                    } else {
                        return o("segments", array);
                    }
                });
            }
        }

        public List<SourceParameters> segments;
        public boolean giveContext = true;
        public Map<String, Object> kwargs = Map.of();
    }

    public static class SourceParameters {
        public String source;
        public TemplateType is = TemplateType.STRING;

        @JsonCreator
        public SourceParameters() {

        }

        @JsonCreator
        public SourceParameters(String source) {
            this.source = source;
        }
    }
}
