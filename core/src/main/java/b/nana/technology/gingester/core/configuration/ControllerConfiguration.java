package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.transformer.InputStasher;
import b.nana.technology.gingester.core.transformer.OutputFetcher;
import b.nana.technology.gingester.core.transformer.Transformer;
import net.jodah.typetools.TypeResolver;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class ControllerConfiguration<I, O> {

    private final Gingester.ControllerConfigurationInterface gingester;

    private String id;
    private Transformer<I, O> transformer;
    private Integer maxWorkers;
    private Integer maxQueueSize;
    private Integer maxBatchSize;
    public Map<String, String> links = new LinkedHashMap<>();
    private List<String> syncs = Collections.emptyList();
    private List<String> excepts = Collections.emptyList();
    private boolean report;
    private Counter acksCounter;

    public ControllerConfiguration(Gingester.ControllerConfigurationInterface gingester) {
        this.gingester = gingester;
    }

    public ControllerConfiguration<I, O> id(String id) {
        this.id = id;
        return this;
    }

    public ControllerConfiguration<I, O> transformer(Transformer<I, O> transformer) {
        this.transformer = transformer;
        return this;
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

    public ControllerConfiguration<I, O> links(List<String> links) {
        this.links.clear();
        links.forEach(l -> this.links.put(l, l));
        return this;
    }

    public ControllerConfiguration<I, O> syncs(List<String> syncs) {
        this.syncs = syncs;
        return this;
    }

    public ControllerConfiguration<I, O> excepts(List<String> excepts) {
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



    public String getId() {
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

    public Map<String, String> getLinks() {
        return links;
    }

    public List<String> getSyncs() {
        return syncs;
    }

    public List<String> getExcepts() {
        return excepts;
    }

    public boolean getReport() {
        return report;
    }

    public Counter getAcksCounter() {
        return acksCounter;
    }



    @SuppressWarnings("unchecked")
    public Class<I> getInputType() {
        return (Class<I>) TypeResolver.resolveRawArguments(Transformer.class, transformer.getClass())[0];
    }

    @SuppressWarnings("unchecked")
    public Class<O> getOutputType() {
        if (transformer instanceof OutputFetcher) {
            return (Class<O>) getCommonSuperClass(gingester.getControllers().values().stream()
                    .filter(c -> c.links.containsValue(id) || c.excepts.contains(id))
                    .map(c -> c.getStashType(((OutputFetcher) transformer).getOutputStashName()))
                    .collect(Collectors.toList()));
        } else if (transformer.getClass().getAnnotation(Passthrough.class) != null) {
            return (Class<O>) getActualInputType();
        } else {
            return (Class<O>) TypeResolver.resolveRawArguments(Transformer.class, transformer.getClass())[1];
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

    private Class<?> getStashType(String[] name) {
        if (name.length > 2) return Object.class;  // TODO determining stash type for deeply stashed items is currently not supported
        if (transformer instanceof InputStasher && (
                (name.length == 1 && ((InputStasher) transformer).getInputStashName().equals(name[0])) ||
                        (name[0].equals(id) && ((InputStasher) transformer).getInputStashName().equals(name[1]))
        )) {
            return getActualInputType();
        } else {
            return getCommonSuperClass(gingester.getControllers().values().stream()
                    .filter(c -> c.links.containsValue(id) || c.excepts.contains(id))
                    .map(c -> c.getStashType(name))
                    .collect(Collectors.toList()));
        }
    }

    static Class<?> getCommonSuperClass(List<Class<?>> classes) {
        if (classes.isEmpty()) return Object.class;  // TODO this is a quick fix
        AtomicReference<Class<?>> pointer = new AtomicReference<>(classes.get(0));
        while (classes.stream().anyMatch(c -> !pointer.get().isAssignableFrom(c))) {
            Class<?> superClass = pointer.get().getSuperclass();
            pointer.set(superClass == null ? Object.class : superClass);  // TODO this is a quick fix
        }
        return pointer.get();
    }
}
