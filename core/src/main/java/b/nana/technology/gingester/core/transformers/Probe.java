package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Names(1)
@Passthrough
public final class Probe implements Transformer<Object, Object> {

    private final FetchKey fetchDescription = new FetchKey("description");
    private final int limit;
    private final FetchKey fetchTarget;
    private final Function<Context, Consumer<String>> targetSupplier;

    public Probe(Parameters parameters) {
        limit = parameters.limit;

        if (parameters.target.equals("stdout")) {
            fetchTarget = null;
            targetSupplier = context -> System.out::print;
        } else if (parameters.target.equals("stderr")) {
            fetchTarget = null;
            targetSupplier = context -> System.err::print;
        } else {
            fetchTarget = new FetchKey(parameters.target);
            targetSupplier = context -> message -> {
                OutputStream outputStream = (OutputStream) context.require(fetchTarget);
                //noinspection SynchronizationOnLocalVariableOrMethodParameter, TODO think of something nicer
                synchronized (outputStream) {
                    PrintStream printStream = new PrintStream(outputStream, false);
                    printStream.print(message);
                    printStream.flush();
                }
            };
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        Consumer<String> target = targetSupplier.apply(context);

        String description = context.fetchReverse(fetchDescription)
                .map(Object::toString)
                .collect(Collectors.joining(" :: "));

        String trace = context.streamReverse().map(Context::getTransformerId).collect(Collectors.joining(" > "));

        if (limit > 0) {
            target.accept(
                    "---- " + description + " ----\nTrace " +
                    trace + "\n\n" +
                    context.prettyStash(limit) + '\n' +
                    in + '\n' +
                    "-".repeat(description.length() + 10) + '\n'
            );
        } else {
            target.accept(
                    "---- " + description + " ----\nTrace " +
                    trace + "\n\n" +
                    in + '\n' +
                    "-".repeat(description.length() + 10) + '\n'
            );
        }

        out.accept(context, in);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, limit -> o("limit", limit));
                rule(JsonNode::isTextual, target -> o("target", target));
                rule(JsonNode::isArray, array -> o("target", array.get(0), "limit", array.get(1)));
            }
        }

        public String target = "stdout";
        public int limit = 10;
    }
}
