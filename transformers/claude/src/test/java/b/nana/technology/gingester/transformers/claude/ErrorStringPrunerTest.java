package b.nana.technology.gingester.transformers.claude;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorStringPrunerTest {

    @Test
    void testPrune() {

        String error = """
                java.lang.RuntimeException: Rethrowing
                
                	at b.nana.technology.gingester.transformers.claude.ErrorStringPrunerTest.foo(ErrorStringPrunerTest.java:14)
                	at b.nana.technology.gingester.transformers.claude.ErrorStringPrunerTest.foo(ErrorStringPrunerTest.java:15)
                	at b.nana.technology.gingester.transformers.claude.ErrorStringPrunerTest.foo(ErrorStringPrunerTest.java:16)
                Caused by: java.lang.IllegalStateException: Testing
                	at b.nana.technology.gingester.transformers.claude.ErrorStringPrunerTest.foo(ErrorStringPrunerTest.java:12)
                	at b.nana.technology.gingester.transformers.claude.ErrorStringPrunerTest.foo(ErrorStringPrunerTest.java:13)
                	at b.nana.technology.gingester.transformers.claude.ErrorStringPrunerTest.foo(ErrorStringPrunerTest.java:14)
                	... 70 more
                """;

        String expectedResult = """
                java.lang.RuntimeException: Rethrowing
                	at b.nana.technology.gingester.transformers.claude.ErrorStringPrunerTest.foo(ErrorStringPrunerTest.java:14)
                Caused by: java.lang.IllegalStateException: Testing
                	at b.nana.technology.gingester.transformers.claude.ErrorStringPrunerTest.foo(ErrorStringPrunerTest.java:12)
                	... 70 more
                """;

        assertEquals(expectedResult, ErrorStringPruner.prune(error));
    }
}