package b.nana.technology.gingester.core.configuration;

import java.util.ArrayList;
import java.util.List;

public final class GingesterConfiguration {
    public Integer report;
    public Boolean debugMode;
    public Boolean shutdownHook;
    public List<String> excepts = new ArrayList<>();
    public List<TransformerConfiguration> transformers = new ArrayList<>();
}
