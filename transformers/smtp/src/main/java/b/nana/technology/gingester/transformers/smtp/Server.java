package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.subethamail.smtp.server.SMTPServer;

import java.util.Map;

public final class Server implements Transformer<Object, byte[]> {

    private final int port;

    public Server(Parameters parameters) {
        port = parameters.port;
    }

    @Override
    public void transform(Context context, Object in, Receiver<byte[]> out) {

        SMTPServer server = SMTPServer
                .port(port)
                .messageHandler((messageContext, from, to, data) ->
                        out.accept(
                                context.stash(Map.of(
                                        "from", from,
                                        "to", to
                                )),
                                data
                        )
                )
                .build();

        server.start();

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignore
            }
        }

        server.stop();
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, port -> o("port", port));
            }
        }

        public int port;
    }
}
