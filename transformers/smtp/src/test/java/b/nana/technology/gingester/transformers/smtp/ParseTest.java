package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.transformers.smtp.mimetree.MimeTreeNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

class ParseTest {

    @Test
    void testParseFromGmail() {

        AtomicReference<MimeTreeNode> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen '/test-from-gmail.txt' " +
                "-t SmtpParse")
                .add(result::set)
                .run();

        System.out.println(result.get());
    }

    @Test
    void testParseFromClawsMail() {

        AtomicReference<MimeTreeNode> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen '/test-from-claws-mail.txt' " +
                "-t SmtpParse")
                .add(result::set)
                .run();

        System.out.println(result.get());
    }
}
