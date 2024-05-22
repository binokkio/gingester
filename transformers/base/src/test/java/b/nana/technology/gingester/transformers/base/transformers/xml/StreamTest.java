package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamTest {

    @Test
    void testSimple_items() {

        Deque<String> items = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data/xml/simple.xml " +
                "-t XmlStream '/body/list/item'")
                .add(items::add)
                .run();

        assertEquals(2, items.size());
        assertEquals("<item>Hello</item>", items.pop());
        assertEquals("<item>World</item>", items.pop());
    }

    @Test
    void testSimple_wildcardMessage() {

        Deque<String> items = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data/xml/simple.xml " +
                "-t XmlStream '/*/message'")
                .add(items::add)
                .run();

        assertEquals(1, items.size());
        assertEquals("<message>Hello, World!</message>", items.pop());
    }

    @Test
    void testWithAttributes_world() {

        Deque<String> items = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data/xml/with-attributes.xml " +
                "-t XmlStream [#noparse]/*/message[@audience='world'][/#noparse]")
                .add(items::add)
                .run();

        assertEquals(4, items.size());
        assertEquals("<message audience=\"world\" type=\"hello\">Hello, World 1!</message>", items.pop());
        assertEquals("<message audience=\"world\" type=\"hello\">Hello, World 2!</message>", items.pop());
        assertEquals("<message audience=\"world\" type=\"bye\">Bye, World 1!</message>", items.pop());
        assertEquals("<message audience=\"world\" type=\"bye\">Bye, World 2!</message>", items.pop());
    }

    @Test
    void testWithAttributes_helloWorld() {

        Deque<String> items = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data/xml/with-attributes.xml " +
                "-t XmlStream \"[#noparse]/*/message[@type='hello' and @audience='world'][/#noparse]\"")
                .add(items::add)
                .run();

        assertEquals(2, items.size());
        assertEquals("<message audience=\"world\" type=\"hello\">Hello, World 1!</message>", items.pop());
        assertEquals("<message audience=\"world\" type=\"hello\">Hello, World 2!</message>", items.pop());
    }
}
