package b.nana.technology.gingester.core.batch;

import b.nana.technology.gingester.core.Context;

public final class Item<T> {

    private final Context context;
    private final T value;

    Item(Context context, T value) {
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
