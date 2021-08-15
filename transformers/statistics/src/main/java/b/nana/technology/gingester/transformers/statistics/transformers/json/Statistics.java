package b.nana.technology.gingester.transformers.statistics.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Transformer;
import com.dynatrace.dynahist.Histogram;
import com.dynatrace.dynahist.bin.BinIterator;
import com.dynatrace.dynahist.layout.CustomLayout;
import com.dynatrace.dynahist.layout.LogQuadraticLayout;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.DoubleArray;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

public class Statistics extends Transformer<JsonNode, JsonNode> {

    private static final NodeConfiguration DEFAULT_NODE_CONFIGURATION;
    static {
        NodeConfiguration defaultNodeConfiguration = new NodeConfiguration();
        defaultNodeConfiguration.disabled = false;
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

            nodeConfiguration.disabled =
                    given.disabled != null ?
                            given.disabled :
                            parent.disabled;

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

            } else if (!nodeConfiguration.disabled) {

                if (!nodeConfiguration.frequencyConfiguration.disabled && !frequencyLimitReached) {
                    frequency.addValue(jsonNode.asText());
                    if (frequency.getUniqueCount() > nodeConfiguration.frequencyConfiguration.frequencyLimit) {
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
                    frequencyNode.put("frequencyLimit", nodeConfiguration.frequencyConfiguration.frequencyLimit);
                }
                fillFrequencyNode(frequencyNode);

                ObjectNode numericalNode = objectMapper.createObjectNode();
                rootNode.set("numerical", numericalNode);
                numericalNode.put("count", numerical.getN());
                if (numerical.getN() > 0) {
                    fillNumericalNode(numericalNode);
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

        private void fillFrequencyNode(ObjectNode frequencyNode) {

            frequencyNode.put("unique", frequency.getUniqueCount());
            frequencyNode.put("percentage", ((double) frequency.getUniqueCount()) / count * 100);

            ArrayNode headNode = objectMapper.createArrayNode();
            frequencyNode.set("head", headNode);

            DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();

            double remaining = count - StreamSupport.stream(Spliterators.spliteratorUnknownSize(frequency.entrySetIterator(), 0), false)
                    .sorted(Comparator.comparingLong(Map.Entry<Comparable<?>, Long>::getValue).reversed())
                    .limit(nodeConfiguration.frequencyConfiguration.frequencyHead)
                    .mapToDouble(frequencyEntry -> {
                        String frequencyEntryKey = (String) frequencyEntry.getKey();
                        ObjectNode frequencyEntryNode = objectMapper.createObjectNode();
                        headNode.add(frequencyEntryNode);
                        double count = frequency.getCount(frequencyEntryKey);
                        frequencyEntryNode.put("value", frequencyEntryKey);
                        frequencyEntryNode.put("count", count);
                        frequencyEntryNode.put("percentage", frequency.getPct(frequencyEntryKey) * 100);
                        pieDataset.setValue(frequencyEntryKey, count);
                        return count;
                    }).sum();

            if (remaining > 0) {
                pieDataset.setValue("<other>", remaining);
            }

            JFreeChart chart = ChartFactory.createRingChart(null, pieDataset, false, false, false);
            chart.removeLegend();
            RingPlot ringPlot = (RingPlot) chart.getPlot();
            ringPlot.setInsets(new RectangleInsets(0, 0, 0, 0));
            ringPlot.setOutlineVisible(false);
            ringPlot.setSectionDepth(1d/3);
            ringPlot.setBackgroundAlpha(0);
            ringPlot.setShadowPaint(new Color(0, true));
            ringPlot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}\n{1}x / {2}"));
            ringPlot.setLabelPadding(new RectangleInsets(4, 8, 4, 8));
            ringPlot.setLabelBackgroundPaint(Color.white);
            ringPlot.setSectionPaint("<other>", new Color(240, 240, 240));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try {
                ChartUtils.writeChartAsPNG(
                        bytes,
                        chart,
                        800,
                        600
                );
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            frequencyNode.put("ringPlot", bytes.toByteArray());
        }

        private void fillNumericalNode(ObjectNode numericalNode) {

            numericalNode.put("percentage", ((double) numerical.getN()) / count * 100);
            numericalNode.put("sum", numerical.getSum());
            numericalNode.put("min", numerical.getMin());
            numericalNode.put("max", numerical.getMax());
            numericalNode.put("mean", numerical.getMean());
            numericalNode.put("variance", numerical.getVariance());
            numericalNode.put("standardDeviation", numerical.getStandardDeviation());

            SimpleHistogramDataset data = new SimpleHistogramDataset(pointer);
            data.setAdjustForBinSize(false);

            // TODO maybe add getBinByRank(0..9) to numericalNode

            ArrayNode histogramsNode = objectMapper.createArrayNode();
            numericalNode.set("histograms", histogramsNode);

            for (HistogramLayout layout : nodeConfiguration.histogramConfiguration.layouts) {

                double from = layout.from != null ? layout.from : histogram.getMin();
                double to = layout.to != null ? layout.to : histogram.getMax();
                double step = (to - from) / layout.bars;
                double current = from;
                double[] boundaries = new double[layout.bars + 1];
                for (int i = 0; i < boundaries.length; i++) {
                    boundaries[i] = current += step;
                }
                CustomLayout customLayout = CustomLayout.create(boundaries);
                Histogram customHistogram = Histogram.createDynamic(customLayout);
                customHistogram.addHistogram(histogram);

                BinIterator binIterator = customHistogram.getFirstNonEmptyBin();
                while (true) {

                    if (!binIterator.isUnderflowBin() && !binIterator.isOverflowBin()) {

                        double lowerBound = binIterator.getLowerBound();
                        double upperBound = binIterator.getUpperBound();

                        try {
                            SimpleHistogramBin bin = new SimpleHistogramBin(lowerBound, upperBound != lowerBound ? upperBound : upperBound + 1e-5);
                            bin.setItemCount((int) binIterator.getBinCount());
                            data.addBin(bin);
                        } catch (Exception e) {
                            // TODO
                            System.err.printf(
                                    "Bad bin: %f, %f\n",
                                    lowerBound, upperBound
                            );
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
                try {
                    ChartUtils.writeChartAsPNG(
                            bytes,
                            chart,
                            800,
                            600
                    );
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                histogramsNode.add(bytes.toByteArray());
            }
        }
    }



    //

    public static class Parameters extends HashMap<String, NodeConfiguration> {

    }

    public static class NodeConfiguration {
        public Boolean disabled;
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
