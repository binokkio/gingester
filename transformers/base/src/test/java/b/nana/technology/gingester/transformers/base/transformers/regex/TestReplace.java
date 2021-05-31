package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestReplace {

    @Test
    void testReplace() {

        Replace.Parameters regexReplaceParameters = new Replace.Parameters();
        regexReplaceParameters.pattern = "[^a-zA-Z0-9-_.]";
        Replace regexReplace = new Replace(regexReplaceParameters);

        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(regexReplace, "Hello, World!");
        gBuilder.link(regexReplace, result::set);
        gBuilder.build().run();

        assertEquals("Hello__World_", result.get());
    }
}
