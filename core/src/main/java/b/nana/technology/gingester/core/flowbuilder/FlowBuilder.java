package b.nana.technology.gingester.core.flowbuilder;

import b.nana.technology.gingester.core.GingesterNext;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import b.nana.technology.gingester.core.transformers.ELog;
import b.nana.technology.gingester.core.transformers.passthrough.BiConsumerPassthrough;
import b.nana.technology.gingester.core.transformers.passthrough.ConsumerPassthrough;
import b.nana.technology.gingester.core.transformers.passthrough.Passthrough;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class FlowBuilder {

    private final Map<String, Node> nodes = new LinkedHashMap<>();

    private Node last;
    private List<String> linkFrom = List.of();
    private List<String> syncFrom = List.of();
    private List<String> divertFrom = List.of();

    private int reportIntervalSeconds;
    private boolean debugMode;
    private boolean shutdownHook;

    public FlowBuilder() {

        Node elog = new Node();
        elog.id("__elog__");
        elog.transformer(new ELog());
        nodes.put(elog.requireId(), elog);

        Node seed = new Node();
        seed.id("__seed__");
        seed.transformer(new Passthrough());
        seed.addExcept("__elog__");
        nodes.put(seed.requireId(), seed);
    }

    public FlowBuilder add(Node node) {

        String id = getId(node);
        node.id(id);
        nodes.put(id, node);
        last = node;

        linkFrom.stream().map(nodes::get).forEach(n -> n.addLink(id));
        linkFrom = List.of(id);

        // TODO handle divert from
        divertFrom = List.of();

        return this;
    }

    public FlowBuilder add(Transformer<?, ?> transformer) {
        return add(new Node().transformer(transformer));
    }

    public <T> FlowBuilder add(Consumer<T> consumer) {
        add(new Node().name("Consumer").transformer(new ConsumerPassthrough<>(consumer)));
        return this;
    }

    public <T> FlowBuilder add(BiConsumer<Context, T> biConsumer) {
        add(new Node().name("BiConsumer").transformer(new BiConsumerPassthrough<>(biConsumer)));
        return this;
    }

    public FlowBuilder linkTo(String link) {
        return linkTo(List.of(link));
    }

    public FlowBuilder linkTo(List<String> links) {
        last.setLinks(links);
        linkFrom = List.of();
        return this;
    }

    public FlowBuilder sync() {
        last.setSyncs(syncFrom);
        return this;
    }

    public FlowBuilder exceptTo(String except) {
        return exceptTo(List.of(except));
    }

    public FlowBuilder exceptTo(List<String> excepts) {
        last.setExcepts(excepts);
        return this;
    }

    public FlowBuilder linkFrom(String linkFrom) {
        return linkFrom(List.of(linkFrom));
    }

    public FlowBuilder linkFrom(List<String> linkFrom) {
        this.linkFrom = linkFrom;
        return this;
    }

    public FlowBuilder syncFrom(String syncFrom) {
        return syncFrom(List.of(syncFrom));
    }

    public FlowBuilder syncFrom(List<String> syncFrom) {
        this.syncFrom = syncFrom;
        return this;
    }

    /**
     * Add cli instructions.
     *
     * @param cli cli instructions template
     */
    public FlowBuilder cli(String[] cli) {
        CliParser.parse(this, cli);
        return this;
    }

    /**
     * Add cli instructions.
     * <p>
     * The given cli string will be rendered using the Apache Freemarker template engine using the
     * square-bracket-tag and square-bracket-interpolation syntax.
     *
     * @param cli cli instructions template
     */
    public FlowBuilder cli(String cli) {
        cli(cli, Collections.emptyMap());
        return this;
    }

    /**
     * Add cli instructions.
     * <p>
     * The given cli string will be rendered using the Apache Freemarker template engine using the
     * square-bracket-tag and square-bracket-interpolation syntax.
     *
     * @param cli cli instructions template
     * @param parameters the parameters for the template, e.g. a Java Map
     */
    public FlowBuilder cli(String cli, Object parameters) {
        CliParser.parse(this, cli, parameters);
        return this;
    }

    /**
     * Add cli instructions.
     * <p>
     * The string obtained from the given URL will be rendered using the Apache Freemarker template engine using
     * the square-bracket-tag and square-bracket-interpolation syntax.
     *
     * @param cli URL for the cli instructions
     */
    public FlowBuilder cli(URL cli) {
        cli(cli, Collections.emptyMap());
        return this;
    }

    /**
     * Add cli instructions.
     * <p>
     * The string obtained from the given URL will be rendered using the Apache Freemarker template engine using
     * the square-bracket-tag and square-bracket-interpolation syntax.
     *
     * @param cli URL for the cli instructions
     * @param parameters the parameters for the template, e.g. a Java Map
     */
    public FlowBuilder cli(URL cli, Object parameters) {
        CliParser.parse(this, cli, parameters);
        return this;
    }

    /**
     * Set report interval in seconds.
     *
     * When set Gingester will report flow details at the given interval.
     * Set 0 to disable reporting.
     *
     * @param reportIntervalSeconds the interval at which to report, or 0 to disable reporting
     */
    public FlowBuilder setReportIntervalSeconds(int reportIntervalSeconds) {
        this.reportIntervalSeconds = reportIntervalSeconds;
        return this;
    }

    /**
     * Enable debug mode.
     *
     * When enabled Gingester will not optimize transformers out of the context stack and will therefore
     * produce more detailed transform traces.
     */
    public FlowBuilder enableDebugMode() {
        debugMode = true;
        return this;
    }

    /**
     * Enable the shutdown hook.
     *
     * When enabled Gingester will register a virtual-machine shutdown hook. When the hook is triggered
     * Gingester will attempt to stop the flow gracefully.
     */
    public FlowBuilder enableShutdownHook() {
        shutdownHook = true;
        return this;
    }

    public GingesterNext build() {
        return new GingesterNext(this);
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public String getLastId() {
        return last.requireId();
    }

    private String getId(Node node) {
        if (node.getId().isPresent()) {
            String id = node.getId().get();
            if (nodes.containsKey(id))
                throw new IllegalArgumentException("Transformer id " + id + " already in use");
            return id;
        } else {
            String name = node.getName()
                    .orElseGet(() -> TransformerFactory.getUniqueName(node.getTransformer().orElseThrow()));
            String id = name;
            int i = 1;
            while (nodes.containsKey(id))
                id = name + '_' + i++;
            return id;
        }
    }
}
