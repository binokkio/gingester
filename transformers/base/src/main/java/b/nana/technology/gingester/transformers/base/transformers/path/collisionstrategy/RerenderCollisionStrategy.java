package b.nana.technology.gingester.transformers.base.transformers.path.collisionstrategy;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.template.TemplateMapper;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Map;

public final class RerenderCollisionStrategy implements CollisionStrategy {

    @Override
    public Path apply(Context context, Object in, Path target, TemplateMapper<Path> targetTemplate, Action action) throws Exception {
        Path tried;
        int collisions = 0;
        for (;;) {
            try {
                action.perform(target);
                break;
            } catch (FileAlreadyExistsException e) {
                tried = target;
                target = targetTemplate.render(context.stash("collisions", ++collisions).buildForSelf(), in);
                if (target.equals(tried))
                    throw e;
            }
        }
        return target;
    }
}
