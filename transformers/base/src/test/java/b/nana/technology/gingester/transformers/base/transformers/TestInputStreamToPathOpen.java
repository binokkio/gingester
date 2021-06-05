package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToPath;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString;
import b.nana.technology.gingester.transformers.base.transformers.path.Open;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestInputStreamToPathOpen {

    @Test
    void test() throws IOException {

        Path tempDirectory = Files.createTempDirectory("gingester-inputstream-test-to-path-");
        Path tempFile = tempDirectory.resolve("test");
        ToPath toPath = new ToPath(new ToPath.Parameters(tempFile.toString()));
        Open open = new Open();
        ToString toString = new ToString();
        String write = "Hello, World!";
        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(toPath, new ByteArrayInputStream(write.getBytes(StandardCharsets.UTF_8)));
        gBuilder.link(toPath, open);
        gBuilder.link(open, toString);
        gBuilder.link(toString, result::set);
        gBuilder.build().run();

        assertEquals(write, result.get());

        Files.delete(tempFile);
        Files.delete(tempDirectory);
    }

    @Test
    void testEmitEarly() throws IOException {

        Path tempDirectory = Files.createTempDirectory("gingester-inputstream-test-to-path-");
        Path tempFile = tempDirectory.resolve("test");
        ToPath.Parameters toPathParameters = new ToPath.Parameters();
        toPathParameters.path = tempFile.toString();
        toPathParameters.emitEarly = true;
        toPathParameters.bufferSize = 3;
        ToPath toPath = new ToPath(toPathParameters);
        Open open = new Open();
        ToString toString = new ToString();
        String write = "Hello, World!";
        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(toPath, new ByteArrayInputStream(write.getBytes(StandardCharsets.UTF_8)));
        gBuilder.link(toPath, open);
        gBuilder.link(open, toString);
        gBuilder.link(toString, result::set);
        gBuilder.build().run();

        assertEquals(write, result.get());

        Files.delete(tempFile);
        Files.delete(tempDirectory);
    }
}
