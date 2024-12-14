package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SymmetricEncryptionTest {

    @Test
    void testPbkdf2Decrypt() {

        AtomicReference<String> decrypted = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t StringDef 'rOQK1Lxe2gDWZ5vA+jwTbw==' -t Base64Decode -a Bytes -s salt
                -t StringDef 'password' -t Pbkdf2 -s key
                -t StringDef 'kvnlwyjdwEKFnrsD2ywGHN2Xd4+RiEOfn6UqyDNSsrX5bTvIKtvFt6IxvJ5EHzZt'
                -t Base64Decode
                -t Decrypt key
                -a String
                """)
                .add(decrypted::set)
                .run();

        assertEquals("My secret message.", decrypted.get());
    }

    @Test
    void testPbkdf2EncryptDecrypt() {

        AtomicReference<String> decrypted = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t BytesRandom 16 -s salt
                -t StringDef 'password' -t Pbkdf2 -s key
                -t StringDef 'My secret message.'
                -t Encrypt ^ piv!
                -t Base64Encode
                -a String
                -t Base64Decode
                -t Decrypt ^
                -a String
                """)
                .add(decrypted::set)
                .run();

        assertEquals("My secret message.", decrypted.get());
    }
}