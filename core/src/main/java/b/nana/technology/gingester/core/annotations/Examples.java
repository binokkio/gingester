package b.nana.technology.gingester.core.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Plumbing class to allow repeated @Example annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Examples {
    Example[] value();
}
