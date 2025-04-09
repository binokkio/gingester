package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.common.InputStreamReplicator;
import b.nana.technology.gingester.core.common.LruMap;
import b.nana.technology.gingester.core.item.CachedItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Held implements AutoCloseable {

    private static volatile Held DEFAULT_HELD;
    public static Held getDefaultHeld() {
        if (DEFAULT_HELD == null) {
            synchronized (Held.class) {
                if (DEFAULT_HELD == null) {
                    DEFAULT_HELD = new Held();
                    DEFAULT_HELD.registerShutdownHook();
                }
            }
        }
        return DEFAULT_HELD;
    }


    private int users;
    private boolean closed;

    private final Thread shutdownHook = new Thread(this::shutdownHook);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private final ConcurrentHashMap<String, LruMap<CacheKey, List<CachedItem<?>>>> caches = new ConcurrentHashMap<>();
    private final Path tempDir;

    public Held() {
        try {
            tempDir = Files.createTempDirectory("gingester-");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public synchronized void registerUser() {
        users++;
    }

    public synchronized void deregisterUser() {
        users--;
        maybeClose();
    }

    private synchronized void shutdownHook() {
        shuttingDown.set(true);
        maybeClose();
    }

    private void maybeClose() {
        if (users == 0 && shuttingDown.get())
            close();
    }

    @Override
    public synchronized void close() {

        if (closed) return;
        closed = true;

        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // ignore
        }

        for (LruMap<CacheKey, List<CachedItem<?>>> cache : caches.values()) {
            cache.forEach(this::close);
        }

        try {
            Files.delete(tempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void close(CacheKey cacheKey, List<CachedItem<?>> cachedItems) {

        for (Object o : cacheKey.values) {
            if (o instanceof InputStreamReplicator isr) {
                isr.close();
            }
        }

        for (CachedItem<?> cachedItem : cachedItems) {
            if (cachedItem.getValue() instanceof InputStreamReplicator isr) {
                isr.close();
            }
        }
    }



    public Path getTempDir() {
        return tempDir;
    }

    public <T> LruMap<CacheKey, List<CachedItem<T>>> getCache(String id, int maxEntries) {
        Object cache = caches.computeIfAbsent(id, key -> new LruMap<>(maxEntries, this::close));
        //noinspection unchecked
        return (LruMap<CacheKey, List<CachedItem<T>>>) cache;
    }

    public InputStreamReplicator wrap(InputStream inputStream, boolean calculateHash) throws IOException {
        return new InputStreamReplicator(inputStream, tempDir, 1_000_000, calculateHash);
    }
}
