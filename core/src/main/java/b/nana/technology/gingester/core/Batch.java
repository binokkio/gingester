package b.nana.technology.gingester.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class Batch<T> implements Iterable<Batch.Entry<T>> {

    private final int capacity;
    private final List<Entry<T>> values;

    Batch(int capacity) {
        this.capacity = capacity;
        this.values = capacity == 0 ? null : new ArrayList<>(capacity);
    }

    Batch(Context context, T value) {
        this.capacity = 1;
        this.values = List.of(new Entry<>(context, value));
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
