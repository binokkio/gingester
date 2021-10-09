package b.nana.technology.gingester.core.reporting;

import java.util.function.LongSupplier;

public interface Counter extends LongSupplier {
    void count();
    void count(long delta);
}
