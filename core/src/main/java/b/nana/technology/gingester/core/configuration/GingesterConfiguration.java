package b.nana.technology.gingester.core.configuration;

import java.util.ArrayList;
import java.util.List;

public final class GingesterConfiguration {
    public Boolean gracefulShutdown = true;
    public Integer report;
    public List<String> excepts = new ArrayList<>();
    public List<TransformerConfiguration> transformers = new ArrayList<>();
}
