package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

class GroovyTest {

    @Test
    void test() {
        new Gingester("" +
                "-t Groovy 'out.accept(context, 3 * 3)' " +
                "-t Out false").run();
    }
}
