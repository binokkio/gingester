package b.nana.technology.gingester.core.cli;

import b.nana.technology.gingester.core.configuration.TransformerConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class DummyTarget implements Target {

    private final List<TransformerConfiguration> transformers;
    private List<String> syncFrom = Collections.singletonList("__seed__");

    public DummyTarget() {
        transformers = new ArrayList<>();
        transformers.add(new TransformerConfiguration().id("__seed__"));
    }

    @Override
    public void setSyncFrom(List<String> syncFrom) {
        this.syncFrom = syncFrom;
    }

    @Override
    public List<String> getSyncFrom() {
        return syncFrom;
    }

    @Override
    public String add(TransformerConfiguration transformer) {
        transformers.add(transformer);
        return transformer.getId().orElse(transformer.getName().orElseThrow());  // assuming for now this dummy target will not have to deal with name collisions
    }

    @Override
    public TransformerConfiguration getLastTransformer() {
        return transformers.get(transformers.size() - 1);
    }

    public List<TransformerConfiguration> getTransformers() {
        return transformers.stream().filter(t -> t.getId().filter("__seed__"::equals).isEmpty()).collect(Collectors.toList());
    }

    // ignore the rest
    @Override public void setReportIntervalSeconds(int reportIntervalSeconds) {}
    @Override public void enableDebugMode() {}
    @Override public void enableShutdownHook() {}
}