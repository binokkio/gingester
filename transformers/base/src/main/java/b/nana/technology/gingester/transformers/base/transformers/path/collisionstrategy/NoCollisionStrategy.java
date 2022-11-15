package b.nana.technology.gingester.transformers.base.transformers.path.collisionstrategy;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.template.TemplateMapper;

import java.nio.file.Path;

public final class NoCollisionStrategy implements CollisionStrategy {

    @Override
    public Path apply(Context context, Object in, Path target, TemplateMapper<Path> targetTemplate, Action action) throws Exception {
        action.perform(target);
        return target;
    }
}
