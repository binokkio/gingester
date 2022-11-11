package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.FlowRunner;
import b.nana.technology.gingester.core.Id;
import b.nana.technology.gingester.core.IdFactory;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.transformer.InputStasher;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class ControllerConfiguration<I, O> {

    private final FlowRunner.ControllerConfigurationInterface gingester;

    private final Id id;
    private final Transformer<I, O> transformer;
    private Integer maxWorkers;
    private Integer maxQueueSize;
    private Integer maxBatchSize;
    private Map<String, Id> links = Collections.emptyMap();
    private List<Id> syncs = Collections.emptyList();
    private List<Id> excepts = Collections.emptyList();
    private boolean report;
    private Counter acksCounter;

    public ControllerConfiguration(Id id, Transformer<I, O> transformer, FlowRunner.ControllerConfigurationInterface gingester) {
        this.id = id;
        this.transformer = transformer;
        this.gingester = gingester;
    }

    public ControllerConfiguration<I, O> maxWorkers(int maxWorkers) {
        this.maxWorkers = maxWorkers;
        return this;
    }

    public ControllerConfiguration<I, O> maxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public ControllerConfiguration<I, O> maxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
        return this;
    }

    public ControllerConfiguration<I, O> links(List<Id> links) {
        this.links = new LinkedHashMap<>();
        links.forEach(link -> this.links.put(link.getGlobalId(), link));
        return this;
    }

    public ControllerConfiguration<I, O> links(Map<String, Id> links) {
        this.links = new LinkedHashMap<>(links);
        return this;
    }

    public ControllerConfiguration<I, O> links(Map<String, String> links, IdFactory idFactory) {
        this.links = new LinkedHashMap<>();
        links.forEach((String name, String id) -> this.links.put(name, idFactory.getId(id)));
        return this;
    }

    public ControllerConfiguration<I, O> syncs(List<String> syncs, IdFactory idFactory) {

        if (!syncs.isEmpty() && !transformer.isSyncAware())
            throw new IllegalArgumentException(id + " is synced with " + syncs.stream().map(idFactory::getId).map(Id::toString).collect(Collectors.joining(" and ")) + " but is not sync-aware");

        this.syncs = idFactory.getIds(syncs);
        return this;
    }

    public ControllerConfiguration<I, O> excepts(List<String> excepts, IdFactory idFactory) {
        this.excepts = idFactory.getIds(excepts);
        return this;
    }

    public ControllerConfiguration<I, O> excepts(List<Id> excepts) {
        this.excepts = excepts;
        return this;
    }

    public ControllerConfiguration<I, O> report(boolean report) {
        this.report = report;
        return this;
    }

    public ControllerConfiguration<I, O> acksCounter(Counter acksCounter) {
        this.acksCounter = acksCounter;
        return this;
    }



    public void updateLink(String linkName, Id target) {
        links.replace(linkName, target);
    }



    public Id getId() {
        return id;
    }

    public Transformer<I, O> getTransformer() {
        return transformer;
    }

    public Optional<Integer> getMaxWorkers() {
        return Optional.ofNullable(maxWorkers);
    }

    public Optional<Integer> getMaxQueueSize() {
        return Optional.ofNullable(maxQueueSize);
    }

    public Optional<Integer> getMaxBatchSize() {
        return Optional.ofNullable(maxBatchSize);
    }

    public Map<String, Id> getLinks() {
        return links;
    }

    public List<Id> getSyncs() {
        return syncs;
    }

    public List<Id> getExcepts() {
        return excepts;
    }

    public boolean getReport() {
        return report;
    }

    public Counter getAcksCounter() {
        return acksCounter;
    }



    public Class<? extends I> getInputType() {
        return transformer.getInputType();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends O> getOutputType() {
        if (transformer.isPassthrough()) {
            return (Class<O>) getActualInputType();
        } else {
            Object outputType = transformer.getOutputType();
            if (outputType instanceof FetchKey) {
                return (Class<O>) getCommonSuperClass(gingester.getControllers().values().stream()
                        .filter(c -> c.links.containsValue(id) || c.excepts.contains(id))
                        .map(c -> c.getStashType((FetchKey) outputType))
                        .collect(Collectors.toList()));
            } else {
                return (Class<O>) outputType;
            }
        }
    }

    private Class<?> getActualInputType() {
        List<Class<?>> inputTypes = new ArrayList<>();
        gingester.getControllers().values().stream()
                .filter(c -> c.links.containsValue(id))
                .map(ControllerConfiguration::getOutputType)
                .forEach(inputTypes::add);
//        if (isExceptionHandler) inputTypes.add(Exception.class);
        return getCommonSuperClass(inputTypes);
    }

    private Class<?> getStashType(FetchKey fetchKey) {

        if (fetchKey.isOrdinal()) {
            if (transformer instanceof InputStasher) {
                if (fetchKey.ordinal() == 1) return getActualInputType();
                fetchKey = fetchKey.decrement();
            }
        } else {
            if (fetchKey.getNames().length == 0) return Map.class;
            if (!fetchKey.hasTarget() || fetchKey.matchesTarget(id)) {
                if (fetchKey.getNames().length == 1 && transformer instanceof InputStasher && ((InputStasher) transformer).getInputStashName().equals(fetchKey.getNames()[0])) {
                    return getActualInputType();
                } else {
                    Map<?, ?> pointer = transformer.getStashDetails();
                    for (int i = 0; i < fetchKey.getNames().length - 1; i++) {
                        Object value = pointer.get(fetchKey.getNames()[i]);
                        if (!(value instanceof Map)) {
                            pointer = Map.of();
                            break;
                        }
                        pointer = (Map<?, ?>) value;
                    }
                    Object type = pointer.get(fetchKey.getNames()[fetchKey.getNames().length - 1]);
                    if (type instanceof Map) return Map.class;
                    else if (type instanceof Class) return (Class<?>) type;
                }
            }
        }

        FetchKey localFetchKey = fetchKey;
        return getCommonSuperClass(gingester.getControllers().values().stream()
                .filter(c -> c.links.containsValue(id) || c.excepts.contains(id))
                .map(c -> c.getStashType(localFetchKey))
                .collect(Collectors.toList()));
    }

    private static Class<?> getCommonSuperClass(List<Class<?>> classes) {
        if (classes.isEmpty()) return Object.class;  // TODO this is a quick fix
        AtomicReference<Class<?>> pointer = new AtomicReference<>(classes.get(0));
        while (classes.stream().anyMatch(c -> !pointer.get().isAssignableFrom(c))) {
            Class<?> superClass = pointer.get().getSuperclass();
            pointer.set(superClass == null ? Object.class : superClass);  // TODO this is a quick fix
        }
        return pointer.get();
    }
}
