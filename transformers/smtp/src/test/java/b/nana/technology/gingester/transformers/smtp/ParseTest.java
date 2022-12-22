package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ParseTest {

    @Test
    void testParsePlainTextFromGmail() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen '/test-from-gmail.txt' " +
                "-t SmtpParse " +
                "-t SmtpGetInlinePlainText")
                .add(result::set)
                .run();

        assertEquals("This is a test mail from Gmail <https://gmail.com>.", result.get().trim());
    }

    @Test
    void testParseFromClawsMail() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen '/test-from-claws-mail.txt' " +
                "-t SmtpParse " +
                "-t SmtpGetInlinePlainText")
                .add(result::set)
                .run();

        assertEquals("This is a test mail from Claws Mail.", result.get().trim());
    }
}
