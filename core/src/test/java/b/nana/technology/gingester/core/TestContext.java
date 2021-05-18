package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestContext {

    @Test
    void testContextStringFormatWithoutSpecifiers() {
        Context.StringFormat stringFormat = new Context.StringFormat("Hello, World!");
        assertEquals("Hello, World!", stringFormat.format(Context.SEED));
    }

    @Test
    void testContextStringFormatSimple() {
        Context.StringFormat stringFormat = new Context.StringFormat("Hello, {target}!");
        Context context = Context.SEED.extend(null).details(Map.of("target", "World")).build();
        assertEquals("Hello, World!", stringFormat.format(context));
    }

    @Test
    void testContextStringFormatMultiple() {
        Context.StringFormat stringFormat = new Context.StringFormat("Hello, {a} and {b} {c}!");
        Context context = Context.SEED
                .extend(null).details(Map.of("a", "World", "b", "Unit Test")).build()
                .extend(null).details(Map.of("c", "Reader")).build();
        assertEquals("Hello, World and Unit Test Reader!", stringFormat.format(context));
    }
}
