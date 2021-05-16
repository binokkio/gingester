package b.nana.technology.gingester.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class Batch<T> implements Iterable<Batch.Entry<T>> {

    private final int capacity;
    private final List<Entry<T>> values;

    final boolean notify;
    final AtomicBoolean done;

    Batch(int capacity) {
        this.capacity = capacity;
        this.values = capacity == 0 ? null : new ArrayList<>(capacity);
        this.notify = false;
        this.done = null;
    }

    Batch(Context context, T value) {
        this(context, value, false);
    }

    Batch(Context context, T value, boolean supportAwait) {
        this.capacity = 1;
        this.values = List.of(new Entry<>(context, value));
        this.notify = supportAwait;
        this.done = supportAwait ? new AtomicBoolean() : null;
    }

    boolean addAndIndicateFull(Context context, T value) {
        int space = capacity - values.size();
        if (space == 1) {
            values.add(new Entry<>(context, value));
            return true;
        } else if (space > 1) {
            values.add(new Entry<>(context, value));
            return false;
        } else {
            throw new IllegalStateException("Batch full");
        }
    }

    void awaitDone() throws InterruptedException {
        synchronized (done) {
            while (!done.get()) {
                done.wait();
            }
        }
    }

    void maybeNotify() {
        if (notify) {
            synchronized (done) {
                done.set(true);
                done.notify();
            }
        }
    }

    @Override
    public Iterator<Entry<T>> iterator() {
        return values.iterator();
    }

    protected static class Entry<T> {

        final Context context;
        final T value;

        private Entry(Context context, T value) {
            this.context = context;
            this.value = value;
        }
    }
}
