package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.controller.CacheKey;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheTest {

    @Test
    void test() {

        ArrayDeque<String> results = new ArrayDeque<>();
        int[] calls = new int[2];

        new FlowBuilder()
                .cli("""
                -t Repeat 3
                -l A B
                -t A:StringDef 'Hello' -l C
                -t B:StringDef 'World' -l C
                """)
                .node()
                    .id("C")
                    .maxCacheEntries(10)
                    .transformer(new Transformer<InputStream, InputStream>() {

                        @Override
                        public CacheKey getCacheKey(Context context, InputStream in) {
                            calls[0]++;
                            return new CacheKey().add(in);
                        }

                        @Override
                        public void transform(Context context, InputStream in, Receiver<InputStream> out) throws IOException {
                            calls[1]++;
                            byte[] reversed = Arrays.reverse(in.readAllBytes());
                            out.accept(context, new ByteArrayInputStream(reversed));
                        }
                    })
                    .add()
                .cli("-a String")
                .add(results::add)
                .run();

        assertEquals("olleH", results.remove());
        assertEquals("dlroW", results.remove());
        assertEquals("olleH", results.remove());
        assertEquals("dlroW", results.remove());
        assertEquals("olleH", results.remove());
        assertEquals("dlroW", results.remove());
        assertTrue(results.isEmpty());

        assertEquals(6, calls[0]);
        assertEquals(2, calls[1]);
    }
}
