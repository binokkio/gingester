package b.nana.technology.gingester.transformers.statistics.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.statistics.common.FrequencyNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.stat.Frequency;

public class Statistics implements Transformer<String, JsonNode> {

    private final ContextMap<FrequencyWrapper> contextMap = new ContextMap<>();
    public final int frequencyLimit;
    public final int frequencyHead;

    public Statistics(Parameters parameters) {
        frequencyLimit = parameters.frequencyLimit;
        frequencyHead = parameters.frequencyHead;
    }

    @Override
    public void prepare(Context context, Receiver<JsonNode> out) {
        FrequencyWrapper frequencyWrapper = new FrequencyWrapper();
        contextMap.put(context, () -> frequencyWrapper);  // TODO different instances per thread, reduce in `finish`
    }

    @Override
    public void transform(Context context, String in, Receiver<JsonNode> out) {
        FrequencyWrapper frequencyWrapper = contextMap.get(context);
        Frequency frequency = frequencyWrapper.frequency;
        if (!frequencyWrapper.limitReached) {
            frequency.addValue(in);
            if (frequency.getUniqueCount() == frequencyLimit) {
                frequencyWrapper.limitReached = true;
            }
        } else if (frequency.getCount(in) > 0) {
            frequency.addValue(in);
        }
    }

    @Override
    public void finish(Context context, Receiver<JsonNode> out) {
        FrequencyWrapper frequencyWrapper = contextMap.remove(context).findFirst().orElseThrow();
        out.accept(
                context.stash("description", "statistics"),
                FrequencyNode.createFrequencyNode(
                        frequencyWrapper.frequency,
                        frequencyLimit,
                        frequencyWrapper.limitReached,
                        frequencyHead
                )
        );
    }

    public static class Parameters {
        public int frequencyLimit = 10000;
        public int frequencyHead = 10;
    }

    private static class FrequencyWrapper {
        private final Frequency frequency = new Frequency();
        private boolean limitReached;
    }
}
