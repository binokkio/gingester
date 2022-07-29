package b.nana.technology.gingester.core.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(StashesRepeatable.class)
public @interface Stashes {
    String stash();
    Class<?> type();
}
