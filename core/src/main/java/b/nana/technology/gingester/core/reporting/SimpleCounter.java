package b.nana.technology.gingester.core.reporting;

import java.util.concurrent.atomic.LongAdder;

public final class SimpleCounter implements Counter {

    private final boolean enabled;
    private final LongAdder value = new LongAdder();

    public SimpleCounter() {
        this(true);
    }

    public SimpleCounter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void count() {
        if (enabled) {
            value.increment();
        }
    }

    @Override
    public void count(long delta) {
        if (enabled) {
            value.add(delta);
        }
    }

    @Override
    public long getAsLong() {
        return value.longValue();
    }
}
