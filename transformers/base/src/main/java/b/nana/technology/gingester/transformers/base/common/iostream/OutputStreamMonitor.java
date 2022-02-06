package b.nana.technology.gingester.transformers.base.common.iostream;

public interface OutputStreamMonitor {
    boolean isClosed();
    void awaitClose(long millis) throws InterruptedException;
}
