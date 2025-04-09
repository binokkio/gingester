package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.cli.CliParser;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.Held;
import b.nana.technology.gingester.core.jsongraph.JsonGraph;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import b.nana.technology.gingester.core.transformers.Elog;
import b.nana.technology.gingester.core.transformers.Seed;
import b.nana.technology.gingester.core.transformers.Void;
import b.nana.technology.gingester.core.transformers.passthrough.BiConsumerPassthrough;
import b.nana.technology.gingester.core.transformers.passthrough.ConsumerPassthrough;
import b.nana.technology.graphtxt.GraphTxt;
import b.nana.technology.graphtxt.SimpleNode;

import java.net.URL;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public final class FlowBuilder {

    private final TransformerFactory transformerFactory = TransformerFactory.withSpiProviders();
    private final IdFactory idFactory = new IdFactory();
    private final Deque<String> scopes = new ArrayDeque<>();
    private final Seed seedTransformer = new Seed();

    final Held held;
    final Map<Id, Node> nodes = new LinkedHashMap<>();
    int reportIntervalSeconds;
    boolean debugMode;
    boolean shutdownHook;
    Context parentContext = null;
    Map<String, ?> seedStash = null;
    Object seedValue = "seed signal";

    private Node last;
    private List<Id> linkFrom = List.of();
    private List<Id> syncFrom = List.of(Id.SEED);
    private List<Id> divertFrom = List.of();

    public FlowBuilder() {
        this(Held.getDefaultHeld());
    }

    public FlowBuilder(Held held) {

        this.held = held;

        Node elog = new Node();
        elog.transformer(new Elog());
        nodes.put(Id.ELOG, elog);

        Node seed = new Node();
        seed.transformer(seedTransformer);
        seed.addExcept(Id.ELOG.getGlobalId());
        nodes.put(Id.SEED, seed);

        last = seed;
    }

    public Held getHeld() {
        return held;
    }

    public FlowBuilder parentContext(Context parentContext) {
        this.parentContext = parentContext;
        return this;
    }

    public FlowBuilder seedStash(Map<String, ?> seedStash) {
        this.seedStash = seedStash;
        return this;
    }

    public FlowBuilder seedValue(Object seedValue) {
        this.seedValue = seedValue;
        this.seedTransformer.setOutputType(seedValue.getClass());
        return this;
    }

    public FlowBuilder enterScope(String scope) {

        if (!Id.ID_PART.matcher(scope).matches())
            throw new IllegalArgumentException("Bad scope: \"" + scope + "\", must match " + Id.ID_PART.pattern());

        scopes.addLast(scope);
        return this;
    }

    public FlowBuilder exitScope() {
        scopes.removeLast();
        return this;
    }

    public Node node() {
        return new Node(this);
    }

    void add(Node node) {

        Id id = getId(node);
        nodes.put(id, node);
        last = node;

        node.scope(id_ -> idFactory.getGlobalId(id_, scopes));

        linkFrom.stream().map(this::getNode).forEach(n -> n.addLink(id.toString(), id.getGlobalId()));
        linkFrom = List.of(id);

        divertFrom.stream().map(this::getNode).forEach(n -> n.updateLinks(id.getGlobalId()));
        divertFrom = List.of();
    }

    public FlowBuilder add(Transformer<?, ?> transformer) {
        return node().transformer(transformer).add();
    }

    public <T> FlowBuilder add(Consumer<T> consumer) {
        return node().name("Consumer").transformer(new ConsumerPassthrough<>(consumer)).add();
    }

    public <T> FlowBuilder add(BiConsumer<Context, T> biConsumer) {
        return node().name("BiConsumer").transformer(new BiConsumerPassthrough<>(biConsumer)).add();
    }

    /**
     * Convenience method to add a consumer immediately downstream of a transformer.
     *
     * @param consumer the consumer
     * @param linkFrom the id of the transformer to add the consumer to
     */
    public <T> FlowBuilder addTo(Consumer<T> consumer, String linkFrom) {
        linkFrom(linkFrom);
        return add(consumer);
    }

    /**
     * Convenience method to add a bi-consumer immediately downstream of a transformer.
     *
     * @param biConsumer the bi-consumer
     * @param linkFrom the id of the transformer to add the consumer to
     */
    public <T> FlowBuilder addTo(BiConsumer<Context, T> biConsumer, String linkFrom) {
        linkFrom(linkFrom);
        return add(biConsumer);
    }

    /**
     * @see #linkTo(List)
     */
    public FlowBuilder linkTo(String link) {
        return linkTo(List.of(link));
    }

    /**
     * Link the output of the most recently added transformer.
     *
     * @param links the ids of the transformers to link the output to
     */
    public FlowBuilder linkTo(List<String> links) {
        last.setLinks(idFactory.getGlobalIds(links, scopes));
        linkFrom = List.of();
        return this;
    }

    /**
     * Sync the most recently added transformer with the current sync-from transformers.
     */
    public FlowBuilder sync() {
        last.setSyncs(syncFrom.stream().map(Id::getGlobalId).collect(Collectors.toList()));
        return this;
    }

    public String getElog(String target) {
        Id id = idFactory.getId("$__elog_" + target + "__");
        if (!nodes.containsKey(id))
            nodes.put(id, node().transformer(new Elog(target)));
        return id.getGlobalId();
    }

    /**
     * @see #exceptTo(List)
     */
    public FlowBuilder exceptTo(String except) {
        return exceptTo(List.of(except));
    }

    public FlowBuilder exceptTo(List<String> excepts) {
        last.setExcepts(idFactory.getGlobalIds(excepts, scopes));
        return this;
    }

    /**
     * @see #linkFrom(List)
     */
    public FlowBuilder linkFrom(String linkFrom) {
        return linkFrom(List.of(linkFrom));
    }

    /**
     * Set the current link-from transformers.
     *
     * @param linkFrom the ids of the transformers to link from when the
     */
    public FlowBuilder linkFrom(List<String> linkFrom) {
        this.linkFrom = idFactory.getIds(linkFrom, scopes);
        return this;
    }

    /**
     * @see #syncFrom(List)
     */
    public FlowBuilder syncFrom(String syncFrom) {
        return syncFrom(List.of(syncFrom));
    }

    /**
     * Set the current sync-from transformers.
     *
     * @param syncFrom the ids of the transformers to sync from when {@link #sync()} is called
     */
    public FlowBuilder syncFrom(List<String> syncFrom) {
        this.syncFrom = idFactory.getIds(syncFrom, scopes);
        return this;
    }

    public FlowBuilder splice(Transformer<?, ?> transformer, String targetId, String linkName) {

        Node node = new Node().transformer(transformer);
        Id id = getId(node);
        nodes.put(id, node);
        last = node;

        Node target = getNode(idFactory.getId(targetId, scopes));
        node.addLink(linkName, target.getLink(linkName));
        target.updateLink(linkName, id.getGlobalId());

        return this;
    }

    /**
     * @see #divert(List)
     */
    public FlowBuilder divert(String divertFrom) {
        return divert(List.of(divertFrom));
    }

    /**
     * Remove everything downstream from the given nodes, those nodes will be linked to the next node added.
     *
     * @param divertFrom the ids of the nodes to divert from
     */
    public FlowBuilder divert(List<String> divertFrom) {

        this.divertFrom = idFactory.getIds(divertFrom, scopes);

        Set<String> knifeTargets = new HashSet<>();

        for (Id id : this.divertFrom) {
            Collection<String> links = getNode(id).getLinks().values();
            if (links.isEmpty()) {
                Node dummy = new Node(this).name("Dummy");
                Id dummyId = getId(dummy);
                nodes.put(dummyId, node());
                getNode(id).addLink(dummyId.toString(), dummyId.getGlobalId());
                knifeTargets.add(dummyId.getGlobalId());
            } else {
                knifeTargets.addAll(links);
            }
        }

        knife(knifeTargets);
        this.last = null;
        this.linkFrom = List.of();
        return this;
    }

    /**
     * @see #knife(Set)
     */
    public FlowBuilder knife(String targetId) {
        return knife(Set.of(targetId));
    }

    /**
     * Remove the given nodes and everything downstream from them.
     *
     * @param targetIds the ids of the nodes to knife
     */
    public FlowBuilder knife(Set<String> targetIds) {
        return knife(targetIds, true);
    }

    private FlowBuilder knife(Set<String> targetIds, boolean check) {

        Set<String> nextTargetIds = new HashSet<>();

        List<Id> ids = idFactory.getIds(targetIds, scopes);
        linkFrom = linkFrom.stream().filter(not(ids::contains)).toList();
        for (Id targetId : ids) {
            Node removed = nodes.remove(targetId);
            if (check && removed == null) throw new IllegalArgumentException("No transformer has id " + targetId);
            else if (removed == null) continue;
            if (last == removed) last = null;
            nextTargetIds.addAll(removed.getLinks().values());
            nodes.forEach((id, node) -> node.updateLinks(targetId.getGlobalId(), "$__void__"));
        }

        if (!nextTargetIds.isEmpty())
            knife(nextTargetIds, false);

        return this;
    }

    /**
     * Replace a transformer.
     *
     * @param targetId the id of the node whose transformer to replace
     * @param transformer the transformer to use as replacement
     */
    public FlowBuilder replace(String targetId, Transformer<?, ?> transformer) {

        Id id = idFactory.getId(targetId, scopes);

        Node node = nodes.get(id);
        if (node == null)
            throw new IllegalArgumentException("No transformer has id " + id);

        node.transformer(transformer);

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
     * @param kwargs the kwargs for the template, e.g. a Java Map
     */
    public FlowBuilder cli(String cli, Object kwargs) {
        CliParser.parse(this, cli, kwargs);
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
     * @param kwargs the kwargs for the template, e.g. a Java Map
     */
    public FlowBuilder cli(URL cli, Object kwargs) {
        CliParser.parse(this, cli, kwargs);
        return this;
    }

    /**
     * Set report interval in seconds.
     * <p>
     * When set the FlowRunner will report flow details at the given interval.
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
     * <p>
     * When enabled the FlowRunner will not optimize transformers out of the context stack and will therefore
     * produce more detailed transform traces.
     */
    public FlowBuilder enableDebugMode() {
        debugMode = true;
        return this;
    }

    /**
     * Enable the shutdown hook.
     * <p>
     * When enabled the FlowRunner will register a virtual-machine shutdown hook. When the hook is triggered
     * the FlowRunner will attempt to stop the flow gracefully.
     */
    public FlowBuilder enableShutdownHook() {
        shutdownHook = true;
        return this;
    }

    /**
     * Create a plain text representation of the current state of this FlowBuilder.
     */
    public String toTxtGraph() {
        List<b.nana.technology.graphtxt.Node> nodes = this.nodes.entrySet().stream()
                .map(entry -> SimpleNode.of(
                        entry.getKey().toString(),
                        Stream.concat(entry.getValue().getLinks().values().stream(), entry.getValue().getExcepts().stream())
                                .map(s -> s.substring(1))  // remove leading scope delimiter, TODO
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
        return new GraphTxt(nodes).getText();
    }

    /**
     * Create a JSON graph representation of the current state of this FlowBuilder.
     */
    public String toJsonGraph() {
        JsonGraph jsonGraph = new JsonGraph();
        nodes.forEach((id, node) -> {
            if (!id.isSystemId()) {
                jsonGraph.add(
                        id.getGlobalId().substring(1),
                        transformerFactory.getUniqueName(node.requireTransformer()),
                        transformerFactory.getParameters(node.requireTransformer()).orElse(null),
                        node.getLinks().values().stream().map(v -> v.substring(1)).toList(),
                        node.getSyncs().stream().map(v -> v.substring(1)).toList()
                );
            }
        });
        return jsonGraph.toString();
    }

    /**
     * Construct a FlowRunner for the current state of this FlowBuilder and run it.
     * <p>
     * Further use of this FlowBuilder is an error leading to undefined behavior.
     */
    public void run() {
        build().run();
    }

    public TransformerFactory getTransformerFactory() {
        return transformerFactory;
    }

    IdFactory getIdFactory() {
        return idFactory;
    }

    Node getNode(Id id) {
        Node node = nodes.get(id);
        if (node == null)
            throw new IllegalArgumentException("No transformer has id " + id);
        return node;
    }

    private FlowRunner build() {

        if (nodes.values().stream().map(Node::getLinks).map(Map::values).flatMap(Collection::stream).anyMatch("$__void__"::equals)) {
            Node void_ = new Node();
            void_.transformer(new Void());
            nodes.put(idFactory.getId("$__void__", scopes), void_);
        }

        return new FlowRunner(this);
    }

    private Id getId(Node node) {
        if (node.getId().isPresent()) {
            Id id = idFactory.getId(node.getId().get(), scopes);
            if (nodes.containsKey(id))
                throw new IllegalArgumentException("Transformer id " + id + " already in use");
            return id;
        } else {
            String name = node.getName()
                    .orElseGet(() -> transformerFactory.getUniqueName(node.requireTransformer()));
            Id id = idFactory.getId(name, scopes);
            int i = 1;
            while (nodes.containsKey(id))
                id = idFactory.getId(name + '_' + i++, scopes);
            return id;
        }
    }

    public Id getLastId() {
        int count = nodes.size();
        return nodes.keySet().stream().skip(count - 1).findFirst().orElseThrow();
    }
}
