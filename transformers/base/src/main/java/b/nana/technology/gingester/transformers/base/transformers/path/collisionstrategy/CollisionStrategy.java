package b.nana.technology.gingester.transformers.base.transformers.path.collisionstrategy;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.template.TemplateMapper;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.nio.file.Path;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "none", value = NoCollisionStrategy.class),
        @JsonSubTypes.Type(name = "re-render", value = ReRenderCollisionStrategy.class),
        @JsonSubTypes.Type(name = "stem-counter", value = StemCounterCollisionStrategy.class)
})
public interface CollisionStrategy {

    Path apply(Context context, Object in, Path target, TemplateMapper<Path> targetTemplate, Action action) throws Exception;

    interface Action {
        void perform(Path path) throws Exception;
    }
}