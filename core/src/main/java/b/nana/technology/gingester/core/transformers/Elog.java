package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowRunner;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Elog implements Transformer<Exception, Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowRunner.class);

    private final FetchKey fetchMethod = new FetchKey("method");
    private final FetchKey fetchDescription = new FetchKey("description");
    private final FetchKey fetchCaughtBy = new FetchKey("caughtBy");

    private final FetchKey fetchTarget;
    private final Function<Context, BiConsumer<String, Exception>> targetSupplier;

    public Elog() {
        fetchTarget = null;
        targetSupplier = context -> LOGGER::warn;
    }

    public Elog(String target) {
        fetchTarget = new FetchKey(target);
        targetSupplier = context -> (message, exception) -> {
            OutputStream outputStream = (OutputStream) context.require(fetchTarget);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter, TODO think of something nicer
            synchronized (outputStream) {
                PrintStream printStream = new PrintStream(outputStream, false);
                printStream.println(message);
                exception.printStackTrace(printStream);
                printStream.flush();
            }
        };
    }

    @Override
    public void transform(Context context, Exception in, Receiver<Exception> out) {

        if (fetchTarget != null || LOGGER.isWarnEnabled()) {

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

            targetSupplier.apply(context).accept(message.toString(), in);
        }

        out.accept(context, in);
    }
}
