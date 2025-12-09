package b.nana.technology.gingester.transformers.base.transformers.route;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;

@Names(1)
@Passthrough
@Description("Route items based on their ordinality within the sync context, cycling the given links")
@Example(example = "A B", description = "Route the first item to A, the second to B, the third to A, and so forth")
public final class CycleRoute extends OrdinalRoute {

    public CycleRoute(Parameters parameters) {
        super(parameters, true);
    }
}
