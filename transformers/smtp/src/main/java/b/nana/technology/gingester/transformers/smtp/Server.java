package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.subethamail.smtp.server.SMTPServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public final class Server implements Transformer<Object, byte[]> {

    private final Parameters parameters;

    public Server(Parameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingAsync();
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.of(
                "from", String.class,
                "to", String.class,
                "message", byte[].class
        );
    }

    @Override
    public void transform(Context context, Object in, Receiver<byte[]> out) throws UnknownHostException {

        SMTPServer.Builder builder = new SMTPServer.Builder()
                .messageHandler((messageContext, from, to, message) ->
                        out.accept(
                                context.stash(Map.of(
                                        "from", from,
                                        "to", to,
                                        "message", message
                                )),
                                message
                        )
                );

        if (parameters.ip != null) builder.bindAddress(InetAddress.getByName(parameters.ip));
        if (parameters.port != null) builder.port(parameters.port);

        SMTPServer server = builder.build();
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

        public String ip;
        public Integer port;
    }
}
