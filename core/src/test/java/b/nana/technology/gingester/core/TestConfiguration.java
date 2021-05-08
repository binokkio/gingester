package b.nana.technology.gingester.core;

import b.nana.technology.gingester.test.transformers.Emphasize;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConfiguration {

    @Test
    void testEmphasizeHelloWorld() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        Gingester gingester = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world.json")).build();
        gingester.link(gingester.getTransformer("Emphasize", Emphasize.class), result::set);
        gingester.run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeHelloWorldSync() throws IOException {
        AtomicReference<String> result = new AtomicReference<>();
        Gingester gingester = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world-sync.json")).build();
        gingester.link(gingester.getTransformer("Emphasize", Emphasize.class), result::set);
        gingester.run();
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testEmphasizeHelloWorldRestore() throws IOException {
        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world.json"));
        Gingester gingester = configuration.build();
        Configuration restored = Configuration.fromGingester(gingester);
        assertEquals(configuration.toJson(), restored.toJson());
    }

    @Test
    void testEmphasizeHelloWorldSyncRestore() throws IOException {
        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/emphasize-hello-world-sync.json"));
        Gingester gingester = configuration.build();
        Configuration restored = Configuration.fromGingester(gingester);
        assertEquals(configuration.toJson(), restored.toJson());
        assertEquals(configuration.hash(), Configuration.fromGingester(gingester).hash());
    }
}
