package b.nana.technology.gingester.transformers.statistics.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Transformer;
import com.dynatrace.dynahist.Histogram;
import com.dynatrace.dynahist.bin.BinIterator;
import com.dynatrace.dynahist.layout.LogQuadraticLayout;
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
    private final Map<String, NodeConfiguration> nodeConfigurations;

    public Statistics(Parameters parameters) {
        super(parameters);
        nodeConfigurations = parameters;
    }

    @Override
    protected void prepare(Context context) {
        contextMap.put(context, new NodeStatistics(null, null, ""));
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

        private final NodeStatistics root;
        private final String pointer;

        private final boolean disabled;
        private final int frequencyLimit;
        private final int frequencyHead;
        private final int initialBucketSize;
        private final int finalBucketCount;

        private final List<NodeStatistics> arrayChildren = new ArrayList<>();
        private final Map<String, NodeStatistics> objectChildren = new LinkedHashMap<>();
        private final Frequency frequency = new Frequency();
        private final SummaryStatistics numerical = new SummaryStatistics();
        private final Histogram histogram;

        private long count;
        private boolean frequencyLimitReached;

        NodeStatistics(NodeStatistics root, NodeStatistics parent, String pointer) {

            this.root = root != null ? root : this;
            this.pointer = pointer;

            NodeConfiguration nodeConfiguration = nodeConfigurations.get(pointer);
            if (nodeConfiguration == null) nodeConfiguration = new NodeConfiguration();

            disabled = nodeConfiguration.disabled != null ? nodeConfiguration.disabled : parent != null && parent.disabled;
            frequencyLimit = nodeConfiguration.frequencyLimit != null ? nodeConfiguration.frequencyLimit : parent != null ? parent.frequencyLimit : 10000;
            frequencyHead = nodeConfiguration.frequencyHead != null ? nodeConfiguration.frequencyHead : parent != null ? parent.frequencyHead : 10;
            initialBucketSize = nodeConfiguration.initialBucketSize != null ? nodeConfiguration.initialBucketSize : parent != null ? parent.initialBucketSize : 1;
            finalBucketCount = nodeConfiguration.finalBucketCount != null ? nodeConfiguration.finalBucketCount : parent != null ? parent.finalBucketCount : 100;

            histogram = Histogram.createDynamic(LogQuadraticLayout.create(
                    initialBucketSize,
                    1e-5,
                    Double.MIN_VALUE,
                    Double.MAX_VALUE
            ));
        }

        private void accept(JsonNode jsonNode) {

            count++;

            if (jsonNode.isArray()) {

                for (int i = 0; i < jsonNode.size(); i++) {
                    NodeStatistics nodeStatistics;
                    if (i >= arrayChildren.size()) {
                        nodeStatistics = new NodeStatistics(root, this, pointer + '/' + i);
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
                    objectChildren
                            .computeIfAbsent(fieldName, x -> new NodeStatistics(root, this, pointer + '/' + fieldName))  // TODO encode '/' and '~', see https://datatracker.ietf.org/doc/html/rfc6901#section-3
                            .accept(jsonNode.get(fieldName));
                }

            } else if (!disabled) {

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
            histogram.addValue(numericalValue);
        }

        @JsonValue
        public Object getJsonValue() {

            if (frequency.getUniqueCount() > 0 || frequencyLimitReached || numerical.getN() > 0) {

                ObjectNode rootNode = objectMapper.createObjectNode();

                rootNode.put("pointer", pointer);

                ObjectNode presenceNode = objectMapper.createObjectNode();
                rootNode.set("presence", presenceNode);
                presenceNode.put("count", count);
                presenceNode.put("percentage", ((double) count) / root.count * 100);

                ObjectNode frequencyNode = objectMapper.createObjectNode();
                rootNode.set("frequency", frequencyNode);
                if (frequencyLimitReached) {
                    frequencyNode.put("frequencyLimitReached", true);
                    frequencyNode.put("frequencyLimit", frequencyLimit);
                } else {
                    frequencyNode.put("unique", frequency.getUniqueCount());
                    frequencyNode.put("percentage", ((double) frequency.getUniqueCount()) / count * 100);

                    ArrayNode headNode = objectMapper.createArrayNode();
                    frequencyNode.set("head", headNode);
                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(frequency.entrySetIterator(), 0), false)
                            .sorted(Comparator.comparingLong(Map.Entry<Comparable<?>, Long>::getValue).reversed())
                            .limit(frequencyHead)
                            .forEach(frequencyEntry -> {
                                String frequencyEntryKey = (String) frequencyEntry.getKey();
                                ObjectNode frequencyEntryNode = objectMapper.createObjectNode();
                                frequencyEntryNode.put("value", frequencyEntryKey);
                                frequencyEntryNode.put("count", frequency.getCount(frequencyEntryKey));
                                frequencyEntryNode.put("percentage", frequency.getPct(frequencyEntryKey) * 100);
                                headNode.add(frequencyEntryNode);
                            });
                }

                ObjectNode numericalNode = objectMapper.createObjectNode();
                rootNode.set("numerical", numericalNode);
                numericalNode.put("count", numerical.getN());
                numericalNode.put("percentage", numerical.getN() / count * 100);
                numericalNode.put("sum", numerical.getSum());
                numericalNode.put("min", numerical.getMin());
                numericalNode.put("max", numerical.getMax());
                numericalNode.put("mean", numerical.getMean());
                numericalNode.put("variance", numerical.getVariance());
                numericalNode.put("standardDeviation", numerical.getStandardDeviation());

                if (histogram.getTotalCount() > 0) {
                    ArrayNode buckets = objectMapper.createArrayNode();
                    numericalNode.set("buckets", buckets);
                    BinIterator binIterator = histogram.getFirstNonEmptyBin();
                    while (true) {
                        ObjectNode bucketNode = objectMapper.createObjectNode();
                        buckets.add(bucketNode);
                        bucketNode.put("from", binIterator.getLowerBound());
                        bucketNode.put("to", binIterator.getUpperBound());
                        bucketNode.put("count", binIterator.getBinCount());
                        if (binIterator.isLastNonEmptyBin()) break;
                        binIterator.next();
                    }
                }

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

    public static class Parameters extends HashMap<String, NodeConfiguration> {

    }

    public static class NodeConfiguration {
        public Boolean disabled;
        public Integer frequencyLimit;
        public Integer frequencyHead;
        public Integer initialBucketSize;
        public Integer finalBucketCount;
    }
}
