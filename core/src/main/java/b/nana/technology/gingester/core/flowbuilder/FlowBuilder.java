package b.nana.technology.gingester.core.flowbuilder;

import java.util.List;

public final class FlowBuilder {

    private List<String> linkFrom;
    private List<String> syncFrom;
    private List<String> divertFrom;

    public FlowBuilder add(Node node) {
        return this;
    }
}
