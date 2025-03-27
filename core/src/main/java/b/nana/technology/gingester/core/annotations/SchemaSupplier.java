package b.nana.technology.gingester.core.annotations;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaSupplier {
    Class<? extends Supplier<ObjectNode>> value();
}
