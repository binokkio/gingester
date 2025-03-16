package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PemTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t GenKeyPair
                -t StringDef 'My secret message.'
                -t Encrypt privk
                -s encryptedMessage
                -f pubk
                -t KeyToPem
                -t PemToKey
                -f encryptedMessage
                -t Decrypt PemToKey.pubk
                -a String
                """)
                .add(result::set)
                .run();

        assertEquals("My secret message.", result.get());
    }
}
