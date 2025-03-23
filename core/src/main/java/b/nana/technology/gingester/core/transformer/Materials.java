package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.provider.Provider;

import java.util.*;
import java.util.stream.Collectors;

final class Materials {

    final Set<Class<? extends Transformer<?, ?>>> transformers;
    final Set<Class<? extends Transformer<?, ?>>> pure;
    final Map<String, String> caseHints;

    Materials() {
        this(ServiceLoader.load(Provider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList());
    }

    Materials(List<Provider> providers) {
        transformers = providers.stream()
                .map(Provider::getTransformerClasses)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        pure = transformers
                .stream()
                .filter(c -> c.getAnnotation(Pure.class) != null)
                .collect(Collectors.toSet());

        caseHints = providers.stream()
                .map(Provider::getCaseHints)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> {
                            if (!a.equals(b)) throw new IllegalStateException("Conflicting case hints: " + a + " and " + b);
                            return a;
                        }));
    }
}
