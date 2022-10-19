package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@Names(1)
@Passthrough
public final class Log implements Transformer<Object, Object> {

    private final Consumer<String> log;
    private final Template messageTemplate;

    public Log(Parameters parameters) {

        Logger logger = parameters.name != null ?
            LoggerFactory.getLogger(parameters.name) :
            LoggerFactory.getLogger(getClass());

        log = getLogger(logger, parameters.level);
        messageTemplate = Context.newTemplate(parameters.message);
    }

    private Consumer<String> getLogger(Logger logger, String level) {
        switch (level) {
            case "trace": return logger::trace;
            case "debug": return logger::debug;
            case "info": return logger::info;
            case "warn": return logger::warn;
            case "error": return logger::error;
            default: throw new IllegalArgumentException("Unknown log level: " + level);
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        log.accept(messageTemplate.render(context));
        out.accept(context, in);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, message -> o("message", message));
                rule(JsonNode::isArray, array -> {
                    if (array.size() == 2) {
                        return o("level", array.get(0), "message", array.get(1));
                    } else if (array.size() == 3) {
                        return o("name", array.get(0), "level", array.get(1), "message", array.get(2));
                    } else {
                        throw new IllegalArgumentException("Unexpected number of arguments for Log: " + array.size());
                    }
                });
            }
        }

        public String name;
        public String level = "info";
        public TemplateParameters message;
    }
}
