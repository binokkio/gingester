package b.nana.technology.gingester.core.reporting;

import java.util.concurrent.atomic.AtomicLong;

public final class SimpleCounter implements Counter {

    private final boolean enabled;
    private final AtomicLong value = new AtomicLong();

    public SimpleCounter() {
        this(true);
    }

    public SimpleCounter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void count() {
        if (enabled) {
            value.incrementAndGet();
        }
    }

    @Override
    public void count(long delta) {
        if (enabled) {
            value.addAndGet(delta);
        }
    }

    @Override
    public long getAsLong() {
        return value.longValue();
    }
}
