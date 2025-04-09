package b.nana.technology.gingester.core.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CacheKey {

    Class<?> transformerClass;
    final List<Object> values = new ArrayList<>();

    void setTransformerClass(Class<?> controllerClass) {
        this.transformerClass = controllerClass;
    }

    void set(int index, Object value) {
        values.set(index, value);
    }

    int indexOf(Object value) {
        return values.indexOf(value);
    }

    public CacheKey add(Object value) {
        values.add(value);
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(transformerClass, values);
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof CacheKey other) {
            return transformerClass.equals(other.transformerClass) &&
                    values.equals(other.values);
        }

        return false;
    }
}
