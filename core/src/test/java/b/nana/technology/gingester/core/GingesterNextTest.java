package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.flowbuilder.FlowBuilder;
import b.nana.technology.gingester.core.flowbuilder.Node;
import b.nana.technology.gingester.core.transformers.Probe;
import org.junit.jupiter.api.Test;

class GingesterNextTest {

    @Test
    void test() {

        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.add(new Node().transformer(new Probe(new Probe.Parameters())));

        GingesterNext gingesterNext = flowBuilder.build();
        gingesterNext.run();
    }
}