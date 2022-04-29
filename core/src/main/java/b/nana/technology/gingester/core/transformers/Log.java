package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

@Names(1)
@Passthrough
public final class Log implements Transformer<Object, Object> {

    private final Consumer<String> log;

    public Log(Parameters parameters) {
        Logger logger;
        if (parameters.name != null) {
            logger = LoggerFactory.getLogger(parameters.name);
        } else {
            logger = LoggerFactory.getLogger(getClass());
        }
        log = getLogger(logger, parameters.level);
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
        log.accept(Objects.toString(in));
        out.accept(context, in);
    }

    public static class Parameters {

        public String name;
        public String level = "info";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String level) {
            this.level = level;
        }
    }
}
