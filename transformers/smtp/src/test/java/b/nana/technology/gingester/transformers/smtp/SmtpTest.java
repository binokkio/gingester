package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

class SmtpTest {

    @Test
    void test() {

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t SmtpSendText '{subject:\"Testing\",to:\"test@localhost\"}'"
                ).run();
    }
}