package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class ELog implements Transformer<Exception, Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gingester.class);

    private final FetchKey fetchMethod = new FetchKey("method");
    private final FetchKey fetchDescription = new FetchKey("description");
    private final FetchKey fetchCaughtBy = new FetchKey("caughtBy");

    @Override
    public void transform(Context context, Exception in, Receiver<Exception> out) {

        if (LOGGER.isWarnEnabled()) {

            StringBuilder message = new StringBuilder()
                    .append(in.getClass().getSimpleName())
                    .append(" during ")
                    .append(context.getTransformerId())
                    .append("::")
                    .append(context.require(fetchMethod));

            String description = context.fetchReverse(fetchDescription).map(Object::toString).collect(Collectors.joining(" :: "));
            if (!description.isEmpty()) {
                message
                        .append(" of ")
                        .append(description);
            }

            String at = context.streamReverse().skip(1).map(Context::getTransformerId).collect(Collectors.joining(" > "));
            message
                    .append("\nAt ")
                    .append(at);

            String caughtBy = (String) context.require(fetchCaughtBy);
            if (!caughtBy.equals("__seed__")) {
                message
                    .append(", caught by ")
                    .append(caughtBy);
            }

            in.setStackTrace(Arrays.stream(in.getStackTrace())
                    .filter(e -> !e.getClassName().startsWith("b.nana.technology.gingester.core.controller."))
                    .toArray(StackTraceElement[]::new));

            LOGGER.warn(message.toString(), in);
        }

        out.accept(context, in);
    }
}
