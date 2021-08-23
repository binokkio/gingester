package b.nana.technology.gingester.core.controller;

public interface SetupControls {
    void link(String id, boolean sync);
    void requireSyncLinks();
    void requireAsyncLinks();
}
