package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.annotations.Stashes;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformerTest {

    @Stashes(stash = "hello", type = Integer.class)
    @Stashes(stash = "bye.world", type = Long.class)
    private static class TestTransformer implements Transformer<Object, Object> {
        @Override
        public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        }
    }

    @Test
    void testGetStashDetails() {
        Map<String, Object> stashDetails = new TestTransformer().getStashDetails();
        assertEquals(Integer.class, stashDetails.get("hello"));
        assertEquals(Long.class, ((Map<?, ?>) stashDetails.get("bye")).get("world"));
    }
}