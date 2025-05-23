package b.nana.technology.gingester.core.item;

import b.nana.technology.gingester.core.controller.Context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public final class Batch<T> implements Iterable<Item<T>> {

    private final int capacity;
    private final List<Item<T>> values;

    public Batch(int capacity) {
        this.capacity = capacity;
        this.values = new ArrayList<>(capacity);
    }

    public Batch(Context context, T value) {
        this.capacity = 1;
        this.values = List.of(new Item<>(context, value));
    }

    public boolean addAndIndicateFull(Context context, T value) {
        int space = capacity - values.size();
        if (space == 1) {
            values.add(new Item<>(context, value));
            return true;
        } else if (space > 1) {
            values.add(new Item<>(context, value));
            return false;
        } else {
            throw new IllegalStateException("Batch full");
        }
    }

    public int getSize() {
        return values.size();
    }

    public Item<T> peek() {
        return values.get(0);
    }

    @Override
    public Iterator<Item<T>> iterator() {
        return values.iterator();
    }

    public Stream<Item<T>> stream() {
        return values.stream();
    }
}
