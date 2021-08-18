package b.nana.technology.gingester.transformers.statistics.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.transformers.statistics.common.FrequencyNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.stat.Frequency;

public class Statistics extends Transformer<String, JsonNode> {

    private final ContextMap<FrequencyWrapper> contextMap = new ContextMap<>();
    public final int frequencyLimit;
    public final int frequencyHead;

    public Statistics(Parameters parameters) {
        super(parameters);
        frequencyLimit = parameters.frequencyLimit;
        frequencyHead = parameters.frequencyHead;
    }

    @Override
    protected void prepare(Context context) {
        contextMap.put(context, new FrequencyWrapper());
    }

    @Override
    protected void transform(Context context, String input) {
        FrequencyWrapper frequencyWrapper = contextMap.require(context);
        Frequency frequency = frequencyWrapper.frequency;
        if (!frequencyWrapper.limitReached) {
            frequency.addValue(input);
            if (frequency.getUniqueCount() == frequencyLimit) {
                frequencyWrapper.limitReached = true;
            }
        } else if (frequency.getCount(input) > 0) {
            frequency.addValue(input);
        }
    }

    @Override
    protected void finish(Context context) {
        FrequencyWrapper frequencyWrapper = contextMap.requireRemove(context);
        emit(context, FrequencyNode.createFrequencyNode(
                frequencyWrapper.frequency,
                frequencyLimit,
                frequencyWrapper.limitReached,
                frequencyHead
        ));
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
