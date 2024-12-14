package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsymmetricEncryptionTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t GenKeyPair
                -t StringDef 'My secret message.'
                -t Encrypt privk
                -t Decrypt pubk
                -a String
                """)
                .add(result::set)
                .run();

        assertEquals("My secret message.", result.get());
    }
}