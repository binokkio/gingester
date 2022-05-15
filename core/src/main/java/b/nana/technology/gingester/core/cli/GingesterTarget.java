package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.TransformerConfiguration;

import java.util.List;

final class GingesterTarget implements Target {

    private final Gingester gingester;

    public GingesterTarget(Gingester gingester) {
        this.gingester = gingester;
    }

    @Override
    public void setReportIntervalSeconds(int reportIntervalSeconds) {
        gingester.setReportIntervalSeconds(reportIntervalSeconds);
    }

    @Override
    public void enableDebugMode() {
        gingester.enableDebugMode();
    }

    @Override
    public void enableShutdownHook() {
        gingester.enableShutdownHook();
    }

    @Override
    public void setSyncFrom(List<String> syncFrom) {
        gingester.setSyncFrom(syncFrom);
    }

    @Override
    public List<String> getSyncFrom() {
        return gingester.getSyncFrom();
    }

    @Override
    public String add(TransformerConfiguration transformer) {
        return gingester.add(transformer);
    }

    @Override
    public TransformerConfiguration getLastTransformer() {
        return gingester.getLastTransformer();
    }
}