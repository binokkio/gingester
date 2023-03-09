package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.ContextPlus;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.template.TemplateType;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Names(1)
public final class Gcli implements Transformer<Object, Object> {

    private final List<GcliSegment> gcliSegments;

    public Gcli(Parameters parameters) {
        gcliSegments = parameters.stream()
                .map(GcliSegment::new)
                .collect(Collectors.toList());
    }

    private static class GcliSegment {
        final Function<Context, String> gcliSupplier;
        final Map<String, Object> kwargs;

        GcliSegment(SourceParameters sourceParameters) {
            gcliSupplier = getGcliSupplier(sourceParameters);
            kwargs = sourceParameters.kwargs;
        }
    }

    private static Function<Context, String> getGcliSupplier(SourceParameters sourceParameters) {
        switch (sourceParameters.is) {

            case FILE:
                return Context.newTemplateMapper(
                        new TemplateParameters(sourceParameters.source),
                        s -> Files.readString(Paths.get(s))
                )::render;

            case HOT_FILE:
                Template pathTemplate = Context.newTemplate(new TemplateParameters(sourceParameters.source));
                return c -> {
                    try {
                        return Files.readString(Paths.get(pathTemplate.render(c)));
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
                return c -> (String) c.require(fk);

            case STRING:
                return Context.newTemplate(new TemplateParameters(sourceParameters.source))::render;

            default: throw new IllegalStateException("No case for " + sourceParameters.is);
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        FlowBuilder flowBuilder = new FlowBuilder().seed(context, in);
        gcliSegments.forEach(gs -> flowBuilder.cli(gs.gcliSupplier.apply(context), new ContextPlus(context, in, gs.kwargs)));
        flowBuilder.add(o -> out.accept(context, o)).run();
    }

    public static class Parameters extends ArrayList<SourceParameters> {

    }

    public static class SourceParameters {
        public String source;
        public TemplateType is = TemplateType.STRING;
        public Map<String, Object> kwargs = Map.of();

        @JsonCreator
        public SourceParameters() {

        }

        @JsonCreator
        public SourceParameters(String source) {
            this.source = source;
        }
    }
}
