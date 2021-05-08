package b.nana.technology.gingester.transformers.base;

import java.util.Map;
import java.util.Set;

public class Resolver extends b.nana.technology.gingester.core.Resolver {
    public Resolver() {
        super(
                Set.of("b.nana.technology.gingester.transformers.base.transformers"),
                Map.of("inputstream", "InputStream")
        );
    }
}
