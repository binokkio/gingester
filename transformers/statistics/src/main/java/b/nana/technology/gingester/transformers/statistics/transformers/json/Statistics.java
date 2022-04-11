package b.nana.technology.gingester.transformers.statistics.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.statistics.common.FrequencyNode;
import com.dynatrace.dynahist.Histogram;
import com.dynatrace.dynahist.bin.BinIterator;
import com.dynatrace.dynahist.layout.CustomLayout;
import com.dynatrace.dynahist.layout.LogQuadraticLayout;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;

import java.io.ByteArrayOutputStream;
import java.util.*;

/*
 * TODO split this class in a `Json.Histograms` and `Json.Frequency` etc. classes.
 */

public final class Statistics implements Transformer<JsonNode, JsonNode> {

    private static final NodeConfiguration DEFAULT_NODE_CONFIGURATION;
    static {
        NodeConfiguration defaultNodeConfiguration = new NodeConfiguration();
        defaultNodeConfiguration.disabled = false;
        defaultNodeConfiguration.arrays = "collapsed";
        defaultNodeConfiguration.frequencyConfiguration = new FrequencyConfiguration();
        defaultNodeConfiguration.frequencyConfiguration.disabled = false;
        defaultNodeConfiguration.frequencyConfiguration.frequencyLimit = 10000;
        defaultNodeConfiguration.frequencyConfiguration.frequencyHead = 10;
        defaultNodeConfiguration.histogramConfiguration = new HistogramConfiguration();
        defaultNodeConfiguration.histogramConfiguration.disabled = false;
        defaultNodeConfiguration.histogramConfiguration.precision = 1d;
        HistogramLayout histogramLayout = new HistogramLayout();
        histogramLayout.bars = 100;
        defaultNodeConfiguration.histogramConfiguration.layouts = Collections.singletonList(histogramLayout);
        DEFAULT_NODE_CONFIGURATION = defaultNodeConfiguration;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ContextMap<NodeStatistics> contextMap = new ContextMap<>();
    private final Map<String, NodeConfiguration> nodeConfigurations;

    public Statistics(Parameters parameters) {
        nodeConfigurations = parameters;
    }

    @Override
    public void prepare(Context context, Receiver<JsonNode> out) {
        contextMap.put(context, new NodeStatistics(null, null, ""));  // TODO use ContextMapReduce
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        contextMap.get(context).accept(in);
    }

    @Override
    public void finish(Context context, Receiver<JsonNode> out) {
        ObjectNode result = objectMapper.createObjectNode();
        add(result, contextMap.remove(context));
        out.accept(context.stash("description", "statistics"), result);
    }

    private void add(ObjectNode objectNode, NodeStatistics nodeStatistics) {

        if (nodeStatistics.frequency.getUniqueCount() > 0) {
            objectNode.set(nodeStatistics.pointer, nodeStatistics.getJsonValue());
        }

        nodeStatistics.objectChildren
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> add(objectNode, entry.getValue()));

        nodeStatistics.arrayChildren.forEach(value -> add(objectNode, value));
    }

    private class NodeStatistics {

        private final NodeStatistics root;
        private final String pointer;
        private final NodeConfiguration nodeConfiguration;

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

            nodeConfiguration = createNodeConfiguration(nodeConfigurations.get(pointer), parent != null ? parent.nodeConfiguration : DEFAULT_NODE_CONFIGURATION);

            histogram = Histogram.createDynamic(LogQuadraticLayout.create(
                    nodeConfiguration.histogramConfiguration.precision,
                    1e-5,
                    -Double.MAX_VALUE,
                    Double.MAX_VALUE
            ));
        }

        private NodeConfiguration createNodeConfiguration(NodeConfiguration given, NodeConfiguration parent) {

            // TODO this is too cumbersome

            NodeConfiguration nodeConfiguration = new NodeConfiguration();
            if (given == null) given = nodeConfiguration;  // TODO hacky

            nodeConfiguration.disabled = given.disabled != null ? given.disabled : parent.disabled;
            nodeConfiguration.arrays = given.arrays != null ? given.arrays : parent.arrays;

            nodeConfiguration.frequencyConfiguration = given.frequencyConfiguration != null ? given.frequencyConfiguration : parent.frequencyConfiguration;
            nodeConfiguration.frequencyConfiguration.disabled = nodeConfiguration.frequencyConfiguration.disabled != null ? nodeConfiguration.frequencyConfiguration.disabled : parent.frequencyConfiguration.disabled;
            nodeConfiguration.frequencyConfiguration.frequencyLimit = nodeConfiguration.frequencyConfiguration.frequencyLimit != null ? nodeConfiguration.frequencyConfiguration.frequencyLimit : parent.frequencyConfiguration.frequencyLimit;
            nodeConfiguration.frequencyConfiguration.frequencyHead = nodeConfiguration.frequencyConfiguration.frequencyHead != null ? nodeConfiguration.frequencyConfiguration.frequencyHead : parent.frequencyConfiguration.frequencyHead;

            nodeConfiguration.histogramConfiguration = given.histogramConfiguration != null ? given.histogramConfiguration : parent.histogramConfiguration;
            nodeConfiguration.histogramConfiguration.disabled = nodeConfiguration.histogramConfiguration.disabled != null ? nodeConfiguration.histogramConfiguration.disabled : parent.histogramConfiguration.disabled;
            nodeConfiguration.histogramConfiguration.precision = nodeConfiguration.histogramConfiguration.precision != null ? nodeConfiguration.histogramConfiguration.precision : parent.histogramConfiguration.precision;
            nodeConfiguration.histogramConfiguration.layouts = nodeConfiguration.histogramConfiguration.layouts != null ? nodeConfiguration.histogramConfiguration.layouts : parent.histogramConfiguration.layouts;

            return nodeConfiguration;
        }

        private synchronized void accept(JsonNode jsonNode) {

            count++;

            if (jsonNode.isArray()) {

                for (int i = 0; i < jsonNode.size(); i++) {
                    if (nodeConfiguration.arrays.equals("indexed")) {
                        NodeStatistics nodeStatistics;
                        if (i >= arrayChildren.size()) {
                            nodeStatistics = new NodeStatistics(root, this, pointer + '[' + i + ']');
                            arrayChildren.add(nodeStatistics);
                        } else {
                            nodeStatistics = arrayChildren.get(i);
                        }
                        nodeStatistics.accept(jsonNode.get(i));
                    } else if (nodeConfiguration.arrays.equals("collapsed")) {
                        NodeStatistics nodeStatistics;
                        if (arrayChildren.isEmpty()) {
                            nodeStatistics = new NodeStatistics(root, this, pointer + "[*]");
                            arrayChildren.add(nodeStatistics);
                        }  else {
                            nodeStatistics = arrayChildren.get(0);
                        }
                        nodeStatistics.accept(jsonNode.get(i));
                    } else {
                        throw new IllegalStateException("No handling defined for \"arrays: " + nodeConfiguration.arrays + "\"");
                    }
                }

            } else if (jsonNode.isObject()) {

                Iterator<String> fieldNames = jsonNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    objectChildren
                            .computeIfAbsent(fieldName, x -> new NodeStatistics(root, this, pointer.isEmpty() ? fieldName : pointer + '.' + fieldName))
                            .accept(jsonNode.get(fieldName));
                }

            } else if (!nodeConfiguration.disabled) {

                if (!nodeConfiguration.frequencyConfiguration.disabled) {
                    String asText = jsonNode.asText();
                    if (!frequencyLimitReached) {
                        frequency.addValue(asText);
                        if (frequency.getUniqueCount() == nodeConfiguration.frequencyConfiguration.frequencyLimit) {
                            frequencyLimitReached = true;
                        }
                    } else if (frequency.getCount(asText) > 0) {
                        frequency.addValue(asText);
                    }
                }

                if (jsonNode.isNumber()) {
                    handleNumericalValue(jsonNode.doubleValue());
                } else if (!jsonNode.isBoolean()) {
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
            if (!nodeConfiguration.histogramConfiguration.disabled) {
                histogram.addValue(numericalValue);
            }
        }

        public ObjectNode getJsonValue() {

            ObjectNode rootNode = objectMapper.createObjectNode();

            ObjectNode presenceNode = objectMapper.createObjectNode();
            rootNode.set("presence", presenceNode);
            presenceNode.put("count", count);
            presenceNode.put("percentage", ((double) count) / root.count * 100);

            if (!nodeConfiguration.frequencyConfiguration.disabled) {
                rootNode.set("frequency", FrequencyNode.createFrequencyNode(frequency, nodeConfiguration.frequencyConfiguration.frequencyLimit, frequencyLimitReached, nodeConfiguration.frequencyConfiguration.frequencyHead));
            }

            ObjectNode numericalNode = objectMapper.createObjectNode();
            rootNode.set("numerical", numericalNode);
            fillNumericalNode(numericalNode);

            if (!objectChildren.isEmpty()) {
                rootNode.set("object", objectMapper.valueToTree(objectChildren));
            }

            if (!arrayChildren.isEmpty()) {
                rootNode.set("array", objectMapper.valueToTree(arrayChildren));
            }

            return rootNode;
        }

        private void fillNumericalNode(ObjectNode numericalNode) {

            numericalNode.put("count", numerical.getN());

            if (numerical.getN() > 0) {

                numericalNode.put("percentage", ((double) numerical.getN()) / count * 100);
                numericalNode.put("sum", numerical.getSum());
                numericalNode.put("min", numerical.getMin());
                numericalNode.put("max", numerical.getMax());
                numericalNode.put("mean", numerical.getMean());
                numericalNode.put("variance", numerical.getVariance());
                numericalNode.put("standardDeviation", numerical.getStandardDeviation());

                if (nodeConfiguration.histogramConfiguration.disabled) return;

                // TODO maybe add getBinByRank(0..9) to numericalNode

                ArrayNode histogramsNode = objectMapper.createArrayNode();
                numericalNode.set("histograms", histogramsNode);

                for (HistogramLayout layout : nodeConfiguration.histogramConfiguration.layouts) {
                    try {

                        SimpleHistogramDataset data = new SimpleHistogramDataset(pointer);
                        data.setAdjustForBinSize(false);

                        double from = layout.from != null ? layout.from : histogram.getMin();
                        double to = layout.to != null ? layout.to : histogram.getMax();
                        double step = Math.max((to - from) / layout.bars, nodeConfiguration.histogramConfiguration.precision);
                        double current = from;
                        double[] boundaries = new double[layout.bars + 1];
                        for (int i = 0; i < boundaries.length; i++) {
                            boundaries[i] = current;
                            current += step;
                        }
                        CustomLayout customLayout = CustomLayout.create(boundaries);
                        Histogram customHistogram = Histogram.createDynamic(customLayout);
                        customHistogram.addHistogram(histogram);

                        BinIterator binIterator = customHistogram.getFirstNonEmptyBin();
                        while (true) {

                            if (!binIterator.isUnderflowBin() && !binIterator.isOverflowBin()) {

                                double lowerBound = binIterator.getLowerBound();
                                double upperBound = binIterator.getUpperBound();

                                if (binIterator.isFirstNonEmptyBin()) {
                                    if (upperBound - lowerBound < step / 2) {
                                        lowerBound = upperBound - step;
                                    }
                                } else if (binIterator.isLastNonEmptyBin()) {
                                    if (upperBound - lowerBound < step / 2) {
                                        upperBound = lowerBound + step;
                                    }
                                }

                                try {
                                    SimpleHistogramBin bin = new SimpleHistogramBin(lowerBound, upperBound, true, false);
                                    bin.setItemCount((int) binIterator.getBinCount());
                                    data.addBin(bin);
                                } catch (Exception e) {
                                    // TODO
                                    System.err.printf(
                                            "Bad bin: %f, %f\n",
                                            lowerBound, upperBound
                                    );
                                    e.printStackTrace();
                                }
                            }

                            if (binIterator.isLastNonEmptyBin()) break;
                            binIterator.next();
                        }

                        JFreeChart chart = ChartFactory.createHistogram(null, "value", "count", data);
                        chart.removeLegend();
                        XYPlot xyPlot = chart.getXYPlot();
                        xyPlot.setInsets(new RectangleInsets(0, 0, 0, 0));
                        XYBarRenderer renderer = (XYBarRenderer) xyPlot.getRenderer();
                        renderer.setDrawBarOutline(true);
                        renderer.setBarPainter(new StandardXYBarPainter());
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        ChartUtils.writeChartAsPNG(
                                bytes,
                                chart,
                                800,
                                600
                        );
                        histogramsNode.add(bytes.toByteArray());
                    } catch (Exception e) {
                        e.printStackTrace();  // TODO
                    }
                }
            }
        }
    }



    //

    public static class Parameters extends HashMap<String, NodeConfiguration> {

    }

    public static class NodeConfiguration {
        public Boolean disabled;
        public String arrays;
        public FrequencyConfiguration frequencyConfiguration;
        public HistogramConfiguration histogramConfiguration;
    }

    public static class FrequencyConfiguration {
        public Boolean disabled;
        public Integer frequencyLimit;
        public Integer frequencyHead;
    }

    public static class HistogramConfiguration {
        public Boolean disabled;
        public Double precision;
        public List<HistogramLayout> layouts;
    }

    public static class HistogramLayout {
        public Double from;
        public Double to;
        public Integer bars;
    }
}
