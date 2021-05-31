package b.nana.technology.gingester.core.link;

public interface Link {

    /**
     * Synchronize this link.
     *
     * This will ensure the downstream transform is performed on the same thread
     * immediately after each `emit(..)` by the upstream transformer.
     */
    void sync();

    /**
     * Make this link asynchronous.
     *
     * This will ensure the downstream transform is performed on a different thread
     * some time after each `emit(..)` by the upstream transformer.
     */
    void async();
}
