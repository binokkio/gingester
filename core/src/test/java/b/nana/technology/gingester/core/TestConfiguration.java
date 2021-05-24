package b.nana.technology.gingester.core;

import b.nana.technology.gingester.test.transformers.Emphasize;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConfiguration {

    @Test
    void testEmphasizeHelloWorld() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        Gingester.Builder gBuilder = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world.json")).toBuilder();
        gBuilder.link(gBuilder.getTransformer("Emphasize", Emphasize.class), result::set);
        gBuilder.build().run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeHelloWorldSync() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        Gingester.Builder gBuilder = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world-sync.json")).toBuilder();
        gBuilder.link(gBuilder.getTransformer("Emphasize", Emphasize.class), result::set);
        gBuilder.build().run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeHelloWorldSyncLink() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        Gingester.Builder gBuilder = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world-sync-link.json")).toBuilder();
        gBuilder.link(gBuilder.getTransformer("Emphasize", Emphasize.class), result::set);
        gBuilder.build().run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeHelloWorldDoubleSync() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        Gingester.Builder gBuilder = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world-double-sync.json")).toBuilder();
        gBuilder.link(gBuilder.getTransformer("Emphasize", Emphasize.class), result::set);
        gBuilder.build().run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeHelloWorldRestore() throws IOException {
        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world.json"));
        Gingester gBuilder = configuration.toBuilder().build();
        Configuration restored = Configuration.fromGingester(gBuilder);
        assertEquals(configuration.toJson(), restored.toJson());
    }

    @Test
    void testEmphasizeHelloWorldSyncRestore() throws IOException {
        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world-sync.json"));
        Gingester gBuilder = configuration.toBuilder().build();
        Configuration restored = Configuration.fromGingester(gBuilder);
        assertEquals(configuration.toJson(), restored.toJson());
        assertEquals(configuration.hash(), Configuration.fromGingester(gBuilder).hash());
    }

    @Test
    void testEmphasizeHelloWorldSyncLinkRestore() throws IOException {
        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world-sync-link.json"));
        Gingester gBuilder = configuration.toBuilder().build();
        Configuration restored = Configuration.fromGingester(gBuilder);
        assertEquals(configuration.toJson(), restored.toJson());
        assertEquals(configuration.hash(), Configuration.fromGingester(gBuilder).hash());
    }

    @Test
    void testEmphasizeHelloWorldDoubleSyncRestore() throws IOException {
        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world-double-sync.json"));
        Gingester gBuilder = configuration.toBuilder().build();
        Configuration restored = Configuration.fromGingester(gBuilder);
        assertEquals(configuration.toJson(), restored.toJson());
        assertEquals(configuration.hash(), Configuration.fromGingester(gBuilder).hash());
    }

    @Test
    void testTransformersGetUniqueNames() {
        Gingester.Builder builder = new Gingester.Builder();
        builder.add(new Emphasize());
        builder.add(new Emphasize());
        String configuration = builder.build().toConfiguration().toJson();
        assertTrue(configuration.contains("Emphasize-1"));
        assertTrue(configuration.contains("Emphasize-2"));
    }
}
