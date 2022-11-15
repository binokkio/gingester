package b.nana.technology.gingester.transformers.base.transformers.path.collisionstrategy;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.template.TemplateMapper;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

public abstract class CounterCollisionStrategy implements CollisionStrategy {

    @Override
    public Path apply(Context context, Object in, Path target, TemplateMapper<Path> targetTemplate, Action action) throws Exception {
        Path given = target;
        int collisions = 0;
        for (;;) {
            try {
                action.perform(target);
                break;
            } catch (FileAlreadyExistsException e) {
                target = given.resolveSibling(getCounterSibling(given, ++collisions));
            }
        }
        return target;
    }

    protected abstract String getCounterSibling(Path path, int counter);
}
