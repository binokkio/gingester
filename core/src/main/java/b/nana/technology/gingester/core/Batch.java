package b.nana.technology.gingester.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * TODO also restrict the length of time a batch accepts new values
 *      or somehow support preventing batches from building up too long and
 *      downstream transformers being idle
 */

final class Batch<T> implements Iterable<Batch.Entry<T>> {

    final int capacity;
    private final List<Entry<T>> values;

    Batch(int capacity) {
        this.capacity = capacity;
        this.values = capacity == 0 ? null : new ArrayList<>(capacity);
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
