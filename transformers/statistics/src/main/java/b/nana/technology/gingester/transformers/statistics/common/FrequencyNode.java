package b.nana.technology.gingester.transformers.statistics.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.stat.Frequency;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public final class FrequencyNode {

    private FrequencyNode() {

    }

    public static JsonNode createFrequencyNode(Frequency frequency, int limit, boolean limitReached, int head) {

        ObjectNode frequencyNode = JsonNodeFactory.instance.objectNode();
        long sum = frequency.getSumFreq();

        if (limitReached) {
            frequencyNode.put("frequencyLimitReached", true);
            frequencyNode.put("frequencyLimit", limit);
        }

        frequencyNode.put("distinct", frequency.getUniqueCount());
        frequencyNode.put("percentage", ((double) frequency.getUniqueCount()) / sum * 100);

        ArrayNode headNode = JsonNodeFactory.instance.arrayNode();
        frequencyNode.set("head", headNode);

        DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();

        double remaining = sum - StreamSupport.stream(Spliterators.spliteratorUnknownSize(frequency.entrySetIterator(), 0), false)
                .sorted(Comparator.comparingLong(Map.Entry<Comparable<?>, Long>::getValue).reversed())
                .limit(head)
                .mapToDouble(frequencyEntry -> {
                    String frequencyEntryKey = (String) frequencyEntry.getKey();
                    ObjectNode frequencyEntryNode = JsonNodeFactory.instance.objectNode();
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
        return frequencyNode;
    }
}
