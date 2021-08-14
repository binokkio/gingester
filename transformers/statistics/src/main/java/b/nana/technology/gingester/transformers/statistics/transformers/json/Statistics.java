package b.nana.technology.gingester.transformers.statistics.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.*;
import java.util.stream.StreamSupport;

public class Statistics extends Transformer<JsonNode, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ContextMap<NodeStatistics> contextMap = new ContextMap<>();
    private final int frequencyLimit;
    private final int frequencyHead;

    public Statistics(Parameters parameters) {
        frequencyLimit = parameters.frequencyLimit;
        frequencyHead = parameters.frequencyHead;
    }

    @Override
    protected void prepare(Context context) {
        contextMap.put(context, new NodeStatistics());
    }

    @Override
    protected void transform(Context context, JsonNode input) {
        contextMap.require(context).accept(input);
    }

    @Override
    protected void finish(Context context) {
        emit(
                context.extend(this).description("statistics"),
                objectMapper.valueToTree(contextMap.requireRemove(context))
        );
    }

    private class NodeStatistics {

        private final List<NodeStatistics> arrayChildren = new ArrayList<>();
        private final Map<String, NodeStatistics> objectChildren = new LinkedHashMap<>();
        private final Frequency frequency = new Frequency();
        private final SummaryStatistics numerical = new SummaryStatistics();
        private boolean frequencyLimitReached;

        private void accept(JsonNode jsonNode) {

            if (jsonNode.isArray()) {

                for (int i = 0; i < jsonNode.size(); i++) {
                    NodeStatistics nodeStatistics;
                    if (i >= arrayChildren.size()) {
                        nodeStatistics = new NodeStatistics();
                        arrayChildren.add(nodeStatistics);
                    } else {
                        nodeStatistics = arrayChildren.get(i);
                    }
                    nodeStatistics.accept(jsonNode);
                }

            } else if (jsonNode.isObject()) {

                Iterator<String> fieldNames = jsonNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    objectChildren.computeIfAbsent(fieldName, x -> new NodeStatistics()).accept(jsonNode.get(fieldName));
                }

            } else {

                if (!frequencyLimitReached) {
                    frequency.addValue(jsonNode.asText());
                    if (frequency.getUniqueCount() > frequencyLimit) {
                        frequency.clear();
                        frequencyLimitReached = true;
                    }
                }

                if (jsonNode.isNumber()) {
                    handleNumericalValue(jsonNode.doubleValue());
                } else {
                    double numericalValue = jsonNode.asDouble();
                    String textValue = jsonNode.asText();
                    if (numericalValue != 0 || textValue.equals("0") || textValue.equals("0.0")) {
                        handleNumericalValue(numericalValue);
                    }
                }
            }
        }

        private void handleNumericalValue(double numericalValue) {
            numerical.addValue(numericalValue);
            // TODO bucketing
        }

        @JsonValue
        public Object getJsonValue() {

            if (frequency.getUniqueCount() > 0 || frequencyLimitReached || numerical.getN() > 0) {

                ObjectNode rootNode = objectMapper.createObjectNode();

                ArrayNode frequencyNode = objectMapper.createArrayNode();
                rootNode.set("frequency", frequencyNode);
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(frequency.entrySetIterator(), 0), false)
                        .sorted(Comparator
                                .comparingLong(Map.Entry<Comparable<?>, Long>::getValue).reversed()
//                                .thenComparing((a, b) -> a.getKey().compareTo(b.getKey()))  TODO
                        )
                        .limit(frequencyHead)
                        .forEach(frequencyEntry -> {
                            String frequencyEntryKey = (String) frequencyEntry.getKey();
                            ObjectNode frequencyEntryNode = objectMapper.createObjectNode();
                            frequencyEntryNode.put("value", frequencyEntryKey);
                            frequencyEntryNode.put("count", frequency.getCount(frequencyEntryKey));
                            frequencyEntryNode.put("percentage", frequency.getPct(frequencyEntryKey) * 100);
                            frequencyNode.add(frequencyEntryNode);
                        });

                ObjectNode numericalNode = objectMapper.createObjectNode();
                rootNode.set("numerical", numericalNode);
                numericalNode.put("count", numerical.getN());
                numericalNode.put("sum", numerical.getSum());
                numericalNode.put("min", numerical.getMin());
                numericalNode.put("max", numerical.getMax());
                numericalNode.put("mean", numerical.getMean());
                numericalNode.put("variance", numerical.getVariance());
                numericalNode.put("standardDeviation", numerical.getStandardDeviation());
                // TODO put buckets

                if (!objectChildren.isEmpty()) {
                    rootNode.set("object", objectMapper.valueToTree(objectChildren));
                }

                if (!arrayChildren.isEmpty()) {
                    rootNode.set("array", objectMapper.valueToTree(arrayChildren));
                }

                return rootNode;

            } else {
                if (!objectChildren.isEmpty() && !arrayChildren.isEmpty()) {
                    ObjectNode rootNode = objectMapper.createObjectNode();
                    rootNode.set("object", objectMapper.valueToTree(objectChildren));
                    rootNode.set("array", objectMapper.valueToTree(arrayChildren));
                    return rootNode;
                } else if (!objectChildren.isEmpty()) {
                    return objectChildren;
                } else if (!arrayChildren.isEmpty()) {
                    return arrayChildren;
                } else {
                    return null;  // TODO
                }
            }
        }
    }

    public static class Parameters {
        public int frequencyLimit = 10000;
        public int frequencyHead = 10;
    }
}
