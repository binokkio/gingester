package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.item.CachedItem;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class CachingReceiver<O> implements Receiver<O> {

    private final Held held;
    private final Receiver<O> target;
    private final ArrayList<CachedItem<O>> cacheItems = new ArrayList<>();

    public CachingReceiver(Held held, Receiver<O> target) {
        this.held = held;
        this.target = target;
    }

    @Override
    public void accept(Context context, O output) {
        CachedItem<O> cachedItem = new CachedItem<>(handleValue(output));
        cacheItems.add(cachedItem);
        target.accept(context, cachedItem.getValue());
    }

    @Override
    public void accept(Context.Builder contextBuilder, O output) {
        CachedItem<O> cachedItem = new CachedItem<>(contextBuilder.getStash().orElse(null), handleValue(output));
        cacheItems.add(cachedItem);
        target.accept(contextBuilder, cachedItem.getValue());
    }

    @Override
    public void accept(Context context, O output, String targetId) {
        CachedItem<O> cachedItem = new CachedItem<>(handleValue(output), targetId);
        cacheItems.add(cachedItem);
        target.accept(context, cachedItem.getValue(), targetId);
    }

    @Override
    public void accept(Context.Builder contextBuilder, O output, String targetId) {
        CachedItem<O> cachedItem = new CachedItem<>(contextBuilder.getStash().orElse(null), handleValue(output), targetId);
        cacheItems.add(cachedItem);
        target.accept(contextBuilder, cachedItem.getValue(), targetId);
    }

    private O handleValue(O value) {
        if (value instanceof InputStream inputStream) {
            try {
                //noinspection unchecked
                return (O) held.wrap(inputStream, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return value;
        }
    }

    public List<CachedItem<O>> getCacheItems() {
        return cacheItems;
    }
}
