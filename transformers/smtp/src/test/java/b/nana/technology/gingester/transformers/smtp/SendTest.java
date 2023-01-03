package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.transformers.smtp.mimetree.MimeTreeNode;
import org.apache.james.mime4j.MimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.subethamail.smtp.server.SMTPServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SendTest {

    private SMTPServer server;
    private List<Message> messages;

    static class Message {
        String from;
        String to;
        byte[] message;

        Message(String from, String to, byte[] message) {
            this.from = from;
            this.to = to;
            this.message = message;
        }
    }

    @BeforeEach
    void beforeEach() {
        messages = new ArrayList<>();
        server = SMTPServer
                .port(5025)
                .messageHandler((messageContext, from, to, message) -> messages.add(new Message(from, to, message)))
                .build();
        server.start();
    }

    @AfterEach
    void afterEach() {
        server.stop();
    }

    @Test
    void testSimple() throws MimeException, IOException {

        new FlowBuilder().cli("" +
                "-t StringDef 'Hëllo, World!' " +
                "-t SmtpSend '{smtp:{port:5025},from:\"tester1@localhost\",to:\"tester2@localhost\",subject:\"Testing\"}'"
        ).run();

        assertEquals(1, messages.size());

        Message message = messages.get(0);
        assertEquals("tester1@localhost", message.from);
        assertEquals("tester2@localhost", message.to);

        MimeTreeNode mimeTree = MimeTreeNode.parse(message.message);
        assertEquals("Testing", mimeTree.requireHeader("Subject"));
        assertEquals("Hëllo, World!", mimeTree.decodeBody().trim());
    }

    @Test
    void testInlineAlternatives() throws MimeException, IOException {

        new FlowBuilder().cli("" +
                "-t SmtpSend '{smtp:{port:5025},from:\"tester1@localhost\",to:\"tester2@localhost\",subject:\"Testing\",inline:{\"text/plain\":\"Hëllo, World!\",\"text/html\":\"<em>Hëllo, World!</em>\"}}'"
        ).run();

        assertEquals(1, messages.size());

        Message message = messages.get(0);
        assertEquals("tester1@localhost", message.from);
        assertEquals("tester2@localhost", message.to);

        MimeTreeNode mimeTree = MimeTreeNode.parse(message.message);

        assertEquals("Testing", mimeTree.requireHeader("Subject"));

        assertEquals("text/plain; charset=UTF-8", mimeTree.getChild(0).getChild(0).requireHeader("Content-Type"));
        assertEquals("Hëllo, World!", mimeTree.getChild(0).getChild(0).decodeBody().trim());

        assertEquals("text/html; charset=UTF-8", mimeTree.getChild(0).getChild(1).requireHeader("Content-Type"));
        assertEquals("<em>Hëllo, World!</em>", mimeTree.getChild(0).getChild(1).decodeBody().trim());
    }

    @Test
    void testSendHelloWorldCli() throws MimeException, IOException {

        new FlowBuilder().cli(getClass().getResource("/send-hello-world.cli")).run();

        assertEquals(1, messages.size());

        Message message = messages.get(0);
        assertEquals("tester1@localhost", message.from);
        assertEquals("tester2@localhost", message.to);

        MimeTreeNode mimeTree = MimeTreeNode.parse(message.message);

        assertEquals("Testing", mimeTree.requireHeader("Subject"));

        assertEquals("text/plain; charset=UTF-8", mimeTree.getChild(0).getChild(0).getChild(0).getChild(0).requireHeader("Content-Type"));
        assertEquals("Hëllo, World!", mimeTree.getChild(0).getChild(0).getChild(0).getChild(0).decodeBody().trim());

        assertEquals("text/html; charset=UTF-8", mimeTree.getChild(0).getChild(0).getChild(0).getChild(1).requireHeader("Content-Type"));
        assertEquals("<em>Hëllo, World!</em>", mimeTree.getChild(0).getChild(0).getChild(0).getChild(1).decodeBody().trim());
    }
}
