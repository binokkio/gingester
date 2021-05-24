package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.transformers.string.Generate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestInputSuperOutput {

    @Test
    void testInputSuperOutput() {

        Generate.Parameters helloParameters = new Generate.Parameters();
        helloParameters.payload = "Hello";
        helloParameters.count = 2;
        Generate hello = new Generate(helloParameters);

        Generate.Parameters worldParameters = new Generate.Parameters();
        worldParameters.payload = "World";
        worldParameters.count = 2;
        Generate world = new Generate(worldParameters);

        List<String> results = Collections.synchronizedList(new ArrayList<>());

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.link(hello, world);
        gBuilder.link(world, (Consumer<String>) results::add);
        gBuilder.build().run();

        assertEquals(List.of("World", "World", "World", "World"), results);
    }
}
