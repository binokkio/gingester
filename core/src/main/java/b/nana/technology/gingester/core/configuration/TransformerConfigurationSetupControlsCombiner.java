package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class TransformerConfigurationSetupControlsCombiner {

    private TransformerConfigurationSetupControlsCombiner() {}

    public static <I, O> ControllerConfiguration<I, O> combine(Gingester.ControllerConfigurationInterface gingester, String id, Transformer<?, ?> instance, TransformerConfiguration transformer, SetupControls setup) {

        ControllerConfiguration<I, O> configuration = new ControllerConfiguration<>(gingester);

        configuration
                .id(id)
                .transformer((Transformer<I, O>) instance)
                .report(transformer.getReport().orElse(false))
                .links(choose(transformer::getLinks, setup::getLinks, Collections::emptyList))
                .syncs(choose(transformer::getSyncs, setup::getSyncs, Collections::emptyList))
                .excepts(choose(transformer::getExcepts, setup::getExcepts, Collections::emptyList));

        choose(setup::getMaxBatchSize, transformer::getMaxBatchSize).ifPresent(configuration::maxBatchSize);
        choose(setup::getMaxQueueSize, transformer::getMaxQueueSize).ifPresent(configuration::maxQueueSize);
        choose(setup::getMaxWorkers, transformer::getMaxWorkers).ifPresent(configuration::maxWorkers);

        setup.getAcksCounter().ifPresent(configuration::acksCounter);

        return configuration;
    }

    private static <T> T choose(Supplier<Optional<T>> first, Supplier<Optional<T>> second, Supplier<T> third) {
        return first.get().orElseGet(() -> second.get().orElseGet(third));
    }

    private static Optional<Integer> choose(Supplier<Optional<Integer>> max, Supplier<Optional<Integer>> configured) {
        return Stream.of(max, configured)
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }
}
