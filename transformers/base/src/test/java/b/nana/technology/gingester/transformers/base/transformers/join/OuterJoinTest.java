package b.nana.technology.gingester.transformers.base.transformers.join;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class OuterJoinTest {

    @Test
    void testOneToOneJoin() {

        Deque<Map<String, JsonNode>> result = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-ss joinAs numbers -t JsonDef @ [{id:1,value:11111},{id:2,value:22222},{id:3,value:33333}] -l JsonStream " +
                "-ss joinAs letters -t JsonDef @ [{id:1,value:'aaa'},{id:2,value:'bbb'},{id:3,value:'ccc'}] -l JsonStream " +
                "-t JsonStream '$[*]' " +
                "-s value " +
                "-t JsonPath '$.id' " +
                "-t OuterJoin")
                .add(result::add)
                .run();

        List<Map<String, JsonNode>> sorted = result.stream()
                .sorted(Comparator.comparing(i -> i.get("numbers").get("id").asInt()))
                .collect(Collectors.toList());

        assertEquals(1, sorted.get(0).get("numbers").get("id").asInt());
        assertEquals(1, sorted.get(0).get("letters").get("id").asInt());
        assertEquals(11111, sorted.get(0).get("numbers").get("value").asInt());
        assertEquals("aaa", sorted.get(0).get("letters").get("value").asText());

        assertEquals(2, sorted.get(1).get("numbers").get("id").asInt());
        assertEquals(2, sorted.get(1).get("letters").get("id").asInt());
        assertEquals(22222, sorted.get(1).get("numbers").get("value").asInt());
        assertEquals("bbb", sorted.get(1).get("letters").get("value").asText());

        assertEquals(3, sorted.get(2).get("numbers").get("id").asInt());
        assertEquals(3, sorted.get(2).get("letters").get("id").asInt());
        assertEquals(33333, sorted.get(2).get("numbers").get("value").asInt());
        assertEquals("ccc", sorted.get(2).get("letters").get("value").asText());
    }

    @Test
    void testOneToManyJoin() {

        Deque<Map<String, JsonNode>> result = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-ss joinAs numbers -t JsonDef @ [{id:1,value:11111},{id:2,value:22222}] -l JsonStream " +
                "-ss joinAs letters -t JsonDef @ [{id:1,value:'aaa'},{id:1,value:'AAA'},{id:2,value:'bbb'},{id:2,value:'BBB'}] -l JsonStream " +
                "-t JsonStream '$[*]' " +
                "-s value " +
                "-t JsonPath '$.id' " +
                "-t OuterJoin")
                .add(result::add)
                .run();

        List<Map<String, JsonNode>> sorted = result.stream()
                .sorted(Comparator.comparing(i -> i.get("numbers").get("id").asText() + i.get("letters").get("value").asText()))
                .collect(Collectors.toList());

        assertEquals(1, sorted.get(0).get("numbers").get("id").asInt());
        assertEquals(1, sorted.get(0).get("letters").get("id").asInt());
        assertEquals(11111, sorted.get(0).get("numbers").get("value").asInt());
        assertEquals("AAA", sorted.get(0).get("letters").get("value").asText());

        assertEquals(1, sorted.get(1).get("numbers").get("id").asInt());
        assertEquals(1, sorted.get(1).get("letters").get("id").asInt());
        assertEquals(11111, sorted.get(1).get("numbers").get("value").asInt());
        assertEquals("aaa", sorted.get(1).get("letters").get("value").asText());

        assertEquals(2, sorted.get(2).get("numbers").get("id").asInt());
        assertEquals(2, sorted.get(2).get("letters").get("id").asInt());
        assertEquals(22222, sorted.get(2).get("numbers").get("value").asInt());
        assertEquals("BBB", sorted.get(2).get("letters").get("value").asText());

        assertEquals(2, sorted.get(3).get("numbers").get("id").asInt());
        assertEquals(2, sorted.get(3).get("letters").get("id").asInt());
        assertEquals(22222, sorted.get(3).get("numbers").get("value").asInt());
        assertEquals("bbb", sorted.get(3).get("letters").get("value").asText());
    }

    @Test
    void testJoin3() {

        Deque<Map<String, JsonNode>> result = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-ss joinAs numbers -t JsonDef @ [{id:1,value:11111},{id:2,value:22222},{id:3,value:33333}] -l JsonStream " +
                "-ss joinAs upper -t JsonDef @ [{id:1,value:'AAA'},{id:2,value:'BBB'},{id:3,value:'CCC'}] -l JsonStream " +
                "-ss joinAs lower -t JsonDef @ [{id:1,value:'aaa'},{id:2,value:'bbb'},{id:3,value:'ccc'}] -l JsonStream " +
                "-t JsonStream '$[*]' " +
                "-s value " +
                "-t JsonPath '$.id' " +
                "-t OuterJoin")
                .add(result::add)
                .run();

        List<Map<String, JsonNode>> sorted = result.stream()
                .sorted(Comparator.comparing(i -> i.get("numbers").get("id").asText()))
                .collect(Collectors.toList());

        assertEquals(1, sorted.get(0).get("numbers").get("id").asInt());
        assertEquals(1, sorted.get(0).get("upper").get("id").asInt());
        assertEquals(1, sorted.get(0).get("lower").get("id").asInt());
        assertEquals(11111, sorted.get(0).get("numbers").get("value").asInt());
        assertEquals("AAA", sorted.get(0).get("upper").get("value").asText());
        assertEquals("aaa", sorted.get(0).get("lower").get("value").asText());

        assertEquals(2, sorted.get(1).get("numbers").get("id").asInt());
        assertEquals(2, sorted.get(1).get("upper").get("id").asInt());
        assertEquals(2, sorted.get(1).get("lower").get("id").asInt());
        assertEquals(22222, sorted.get(1).get("numbers").get("value").asInt());
        assertEquals("BBB", sorted.get(1).get("upper").get("value").asText());
        assertEquals("bbb", sorted.get(1).get("lower").get("value").asText());

        assertEquals(3, sorted.get(2).get("numbers").get("id").asInt());
        assertEquals(3, sorted.get(2).get("upper").get("id").asInt());
        assertEquals(3, sorted.get(2).get("lower").get("id").asInt());
        assertEquals(33333, sorted.get(2).get("numbers").get("value").asInt());
        assertEquals("CCC", sorted.get(2).get("upper").get("value").asText());
        assertEquals("ccc", sorted.get(2).get("lower").get("value").asText());
    }

    @Test
    void testOneToOneJoinWithNulls() {

        Deque<Map<String, JsonNode>> result = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-ss joinAs numbers -t JsonDef @ [{id:1,value:11111},{id:2,value:22222}] -l JsonStream " +
                "-ss joinAs letters -t JsonDef @ [{id:2,value:'bbb'},{id:3,value:'ccc'}] -l JsonStream " +
                "-t JsonStream '$[*]' " +
                "-s value " +
                "-t JsonPath '$.id' " +
                "-t OuterJoin")
                .add(result::add)
                .run();

        List<Map<String, JsonNode>> sorted = result.stream()
                .sorted(Comparator.comparing(Object::toString))
                .collect(Collectors.toList());

        assertEquals(1, sorted.get(1).get("numbers").get("id").asInt());
        assertEquals(11111, sorted.get(1).get("numbers").get("value").asInt());
        assertTrue(sorted.get(1).containsKey("letters"));
        assertNull(sorted.get(1).get("letters"));

        assertEquals(2, sorted.get(2).get("numbers").get("id").asInt());
        assertEquals(2, sorted.get(2).get("letters").get("id").asInt());
        assertEquals(22222, sorted.get(2).get("numbers").get("value").asInt());
        assertEquals("bbb", sorted.get(2).get("letters").get("value").asText());

        assertTrue(sorted.get(0).containsKey("numbers"));
        assertNull(sorted.get(0).get("numbers"));
        assertEquals(3, sorted.get(0).get("letters").get("id").asInt());
        assertEquals("ccc", sorted.get(0).get("letters").get("value").asText());
    }

    @Test
    void testOneToManyJoinList() {

        Deque<Map<String, Object>> result = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-ss joinAs numbers -t JsonDef @ [{id:1,value:11111},{id:2,value:22222}] -l JsonStream " +
                "-ss joinAs letters -t JsonDef @ [{id:1,value:'aaa'},{id:1,value:'AAA'},{id:2,value:'bbb'},{id:2,value:'BBB'}] -l JsonStream " +
                "-t JsonStream '$[*]' " +
                "-s value " +
                "-t JsonPath '$.id' " +
                "-t OuterJoin letters")
                .add(result::add)
                .run();

        List<Map<String, Object>> sorted = result.stream()
                .sorted(Comparator.comparing(Object::toString))
                .collect(Collectors.toList());

        assertEquals(1, ((JsonNode) sorted.get(0).get("numbers")).get("id").asInt());
        assertEquals(1, ((JsonNode) ((List<?>) sorted.get(0).get("letters")).get(0)).get("id").asInt());
        assertEquals(1, ((JsonNode) ((List<?>) sorted.get(0).get("letters")).get(1)).get("id").asInt());
        assertEquals(11111, ((JsonNode) sorted.get(0).get("numbers")).get("value").asInt());
        assertEquals("aaa", ((JsonNode) ((List<?>) sorted.get(0).get("letters")).get(0)).get("value").asText());
        assertEquals("AAA", ((JsonNode) ((List<?>) sorted.get(0).get("letters")).get(1)).get("value").asText());

        assertEquals(2, ((JsonNode) sorted.get(1).get("numbers")).get("id").asInt());
        assertEquals(2, ((JsonNode) ((List<?>) sorted.get(1).get("letters")).get(0)).get("id").asInt());
        assertEquals(2, ((JsonNode) ((List<?>) sorted.get(1).get("letters")).get(1)).get("id").asInt());
        assertEquals(22222, ((JsonNode) sorted.get(1).get("numbers")).get("value").asInt());
        assertEquals("bbb", ((JsonNode) ((List<?>) sorted.get(1).get("letters")).get(0)).get("value").asText());
        assertEquals("BBB", ((JsonNode) ((List<?>) sorted.get(1).get("letters")).get(1)).get("value").asText());
    }
}