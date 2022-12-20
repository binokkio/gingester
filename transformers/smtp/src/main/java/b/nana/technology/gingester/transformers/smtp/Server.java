package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.subethamail.smtp.server.SMTPServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class Server implements Transformer<Object, InputStream> {

    private final int port;

    public Server(Parameters parameters) {
        port = parameters.port;
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) {

        SMTPServer server = SMTPServer
                .port(port)
                .messageHandler(
                        (messageContext, from, to, data) -> {

                            try {
                                MimeMessage mimeMessage = new MimeMessage(
                                        Session.getDefaultInstance(new Properties()),
                                        new ByteArrayInputStream(data)
                                );

                                Map<String, String> headers = new LinkedHashMap<>();
                                Enumeration<Header> headersEnumeration = mimeMessage.getAllHeaders();
                                while (headersEnumeration.hasMoreElements()) {
                                    Header header = headersEnumeration.nextElement();
                                    headers.put(header.getName(), header.getValue());
                                }

                                out.accept(
                                        context.stash(Map.of(
                                                "headers", headers
                                        )),
                                        mimeMessage.getInputStream()
                                );

                            } catch (IOException | MessagingException e) {
                                throw new RuntimeException(e);
                            }

                        })
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
