package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.Node;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WormholeTest {

    @Test
    void testLinear() {

        ArrayDeque<Integer> result = new ArrayDeque<>();

        new FlowBuilder().cli("""
                -t IntDef 0
                --wormhole-out
                -t Filter 'in < 5'
                -t Eval 'in + 1'
                --wormhole-in
                """)
                .add(result::add)
                .run();

        assertEquals(5, result.size());
        assertEquals(1, result.pop());
        assertEquals(2, result.pop());
        assertEquals(3, result.pop());
        assertEquals(4, result.pop());
        assertEquals(5, result.pop());
    }

    @Test
    void testNonLinear() {

        ArrayDeque<Integer> result = new ArrayDeque<>();

        new FlowBuilder().cli("""
                -t IntDef 1
                -wo
                -l Process Increment
                -t Increment:Filter 'in < 5'
                -t Eval 'in + 1'
                -wi
                --
                -pt Process
                """)
                .add(result::add)
                .run();

        assertEquals(5, result.size());
        assertEquals(1, result.pop());
        assertEquals(2, result.pop());
        assertEquals(3, result.pop());
        assertEquals(4, result.pop());
        assertEquals(5, result.pop());
    }
}
