package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.transformers.Probe;
import org.junit.jupiter.api.Test;

class GingesterNextTest {

    @Test
    void test() {

        Gingester flowBuilder = new Gingester();
        flowBuilder.add(new Node().transformer(new Probe(new Probe.Parameters())));

        GingesterNext gingesterNext = flowBuilder.build();
        gingesterNext.run();
    }
}