package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.TransformerConfiguration;

import java.util.List;

public interface Target {

    static Target create(Gingester gingester) {
        return new GingesterTarget(gingester);
    }

    static Target createDummy() {
        return new DummyTarget();
    }

    void setReportIntervalSeconds(int reportIntervalSeconds);
    void enableDebugMode();
    void enableShutdownHook();
    void setSyncFrom(List<String> syncFrom);
    List<String> getSyncFrom();
    String add(TransformerConfiguration transformer);
    TransformerConfiguration getLastTransformer();
}
