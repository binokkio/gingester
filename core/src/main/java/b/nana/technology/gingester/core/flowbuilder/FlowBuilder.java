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

    public FlowBuilder setLinks(List<String> links) {
        last.setLinks(links);
        linkFrom = List.of();
        return this;
    }

    public FlowBuilder setExcepts(List<String> excepts) {
        last.setExcepts(excepts);
        return this;
    }

    public FlowBuilder setLinkFrom(List<String> linkFrom) {
        this.linkFrom = linkFrom;
        return this;
    }

    public FlowBuilder setSyncFrom(List<String> syncFrom) {
        this.syncFrom = syncFrom;
        return this;
    }

    public void syncTo() {
        last.setSyncs(syncFrom);
    }

    /**
     * Add cli instructions.
     *
     * @param cli cli instructions template
     * @return this gingester
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
     * @return this gingester
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
     * @return this gingester
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
     * @return this gingester
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
     * @return this gingester
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
     * @return this gingester
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
     *
     * @return this gingester
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
     *
     * @return this gingester
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
