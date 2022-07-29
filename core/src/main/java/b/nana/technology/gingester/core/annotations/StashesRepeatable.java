package b.nana.technology.gingester.core.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Plumbing class to allow repeated @Stashes annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface StashesRepeatable {
    Stashes[] value();
}