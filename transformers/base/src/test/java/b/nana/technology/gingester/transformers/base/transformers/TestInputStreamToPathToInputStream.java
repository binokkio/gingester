package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToPath;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString;
import b.nana.technology.gingester.transformers.base.transformers.path.ToInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestInputStreamToPathToInputStream {

    @Test
    void test() throws IOException {

        Path tempDirectory = Files.createTempDirectory("gingester-inputstream-test-to-path-");
        Path tempFile = tempDirectory.resolve("test");
        ToPath toPath = new ToPath(new ToPath.Parameters(tempFile.toString()));
        ToInputStream toInputStream = new ToInputStream();
        ToString toString = new ToString();
        String write = "Hello, World!";
        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();
        gingester.seed(toPath, new ByteArrayInputStream(write.getBytes(StandardCharsets.UTF_8)));
        gingester.link(toPath, toInputStream);
        gingester.link(toInputStream, toString);
        gingester.link(toString, result::set);
        gingester.run();

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
        ToInputStream toInputStream = new ToInputStream();
        ToString toString = new ToString();
        String write = "Hello, World!";
        AtomicReference<String> result = new AtomicReference<>();

        Gingester gingester = new Gingester();
        gingester.seed(toPath, new ByteArrayInputStream(write.getBytes(StandardCharsets.UTF_8)));
        gingester.link(toPath, toInputStream);
        gingester.link(toInputStream, toString);
        gingester.link(toString, result::set);
        gingester.run();

        assertEquals(write, result.get());

        Files.delete(tempFile);
        Files.delete(tempDirectory);
    }
}
