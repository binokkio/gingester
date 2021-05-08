package b.nana.technology.gingester.test;

import b.nana.technology.gingester.core.Resolver;

import java.util.Map;
import java.util.Set;

public final class TestTransformersResolver extends Resolver {
    public TestTransformersResolver() {
        super(
                Set.of("b.nana.technology.gingester.test.transformers"),
                Map.of("nesteddummy", "NestedDummy")
        );
    }
}
