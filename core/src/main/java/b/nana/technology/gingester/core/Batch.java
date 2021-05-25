package b.nana.technology.gingester.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

final class Batch<T> implements Iterable<Batch.Entry<T>> {

    private final int capacity;
    private final List<Entry<T>> values;

    Batch(int capacity) {
        this.capacity = capacity;
        this.values = new ArrayList<>(capacity);
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

    public int getSize() {
        return values.size();
    }

    @Override
    public Iterator<Entry<T>> iterator() {
        return values.iterator();
    }

    public Stream<Entry<T>> stream() {
        return values.stream();
    }

    protected static class Entry<T> {

        private final Context context;
        private final T value;

        private Entry(Context context, T value) {
            this.context = context;
            this.value = value;
        }

        public Context getContext() {
            return context;
        }

        public T getValue() {
            return value;
        }
    }
}