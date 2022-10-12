package b.nana.technology.gingester.transformers.git;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GiterateTest {

    @Test
    void test() throws IOException {

        Path tempDir = Files.createTempDirectory("gingester-giterate-test-");
        Path bundle = tempDir.resolve("gingester-year-one.bundle");

        new FlowBuilder().cli("-t ResourceOpen /gingester-year-one.bundle -t PathWrite " + bundle).run();

        AtomicReference<String> result = new AtomicReference<>();
        new FlowBuilder()
                .cli(getClass().getResource("/giterate-gingester-year-one.cli"), Map.of(
                        "origin", bundle,
                        "branch", "main",
                        "scratch", tempDir
                ))
                .add(result::set)
                .run();

        assertEquals("" +
                "date,total,added,removed\n" +
                "2021-05-08,19,19,0\n" +
                "2021-06-08,45,30,4\n" +
                "2021-07-08,53,8,0\n" +
                "2021-08-08,53,0,0\n" +
                "2021-09-08,61,9,1\n" +
                "2021-10-08,83,58,36\n" +
                "2021-11-08,95,13,1\n" +
                "2021-12-08,107,15,3\n" +
                "2022-01-08,121,16,2\n" +
                "2022-02-08,128,8,1\n" +
                "2022-03-08,128,0,0\n" +
                "2022-04-08,135,9,2", result.get());

        new FlowBuilder().cli("-t PathDef " + tempDir + " -t PathDelete recursive").run();
    }
}