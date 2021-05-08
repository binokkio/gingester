package b.nana.technology.gingester.core;

import b.nana.technology.gingester.test.TestTransformersResolver;
import b.nana.technology.gingester.test.transformers.Emphasize;
import b.nana.technology.gingester.test.transformers.nesteddummy.Dummy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestResolver {

    @Test
    void testEmptySetThrows() {
        // TODO
    }

    @Test
    void testBadCapitalizationThrows() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Resolver(
                        Set.of(""),
                        Map.of("aaa", "Aab")
                ) {}
        );

        assertEquals("Bad capitalization: aaa -> Aab" , exception.getMessage());
    }

    @Test
    void testOnlyFirstCharIsCapitalized() {
        assertTrue(Resolver.onlyFirstCharIsCapitalized("Foo"));
        assertFalse(Resolver.onlyFirstCharIsCapitalized("foo"));
        assertFalse(Resolver.onlyFirstCharIsCapitalized("FOO"));
    }

    @Test
    void testCountOccurrences() {
        assertEquals(2, Resolver.countOccurrences(".", "foo.bar.Baz"));
        assertEquals(2, Resolver.countOccurrences("..", "foo..bar..Baz"));
    }

    @Test
    void testName() {

        Resolver resolver = new Resolver(
                Set.of("foo"),
                Map.of("inputstream", "InputStream")
        ) {};

        assertEquals(Optional.of("String.HelloWorld"), resolver.name("foo.string.HelloWorld"));
        assertEquals(Optional.of("String.Split.HelloWorld"), resolver.name("foo.string.split.HelloWorld"));
        assertEquals(Optional.of("InputStream.HelloWorld"), resolver.name("foo.inputstream.HelloWorld"));
        assertEquals(Optional.of("Bar.InputStream.HelloWorld"), resolver.name("foo.bar.inputstream.HelloWorld"));
        assertEquals(Optional.of("Bar.InputStream.INPUTSTREAM"), resolver.name("foo.bar.inputstream.INPUTSTREAM"));
        assertEquals(Optional.empty(), resolver.name("bar.inputstream.HelloWorld"));
    }

    @Test
    void testResolve() {
        Resolver resolver = new TestTransformersResolver();
        assertEquals(Optional.of(Emphasize.class), resolver.resolve("Emphasize"));
        assertEquals(Optional.empty(), resolver.resolve("emphasize"));
        assertEquals(Optional.of(Dummy.class), resolver.resolve("NestedDummy.Dummy"));
        assertEquals(Optional.empty(), resolver.resolve("nesteddummy.Dummy"));
        assertEquals(Optional.empty(), resolver.resolve("Nesteddummy.Dummy"));
        assertEquals(Optional.empty(), resolver.resolve("NestedDummy.dummy"));
    }
}
