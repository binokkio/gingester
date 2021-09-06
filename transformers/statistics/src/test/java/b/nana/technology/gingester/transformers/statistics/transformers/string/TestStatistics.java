package b.nana.technology.gingester.transformers.statistics.transformers.string;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.statistics.transformers.string.Statistics;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStatistics {

//    @Test
//    void test() {
//
//        Generate seed = new Generate(new Generate.Parameters("Seed"));
//
//        Generate.Parameters generateHelloParameters = new Generate.Parameters();
//        generateHelloParameters.payload = "Hello, World!";
//        generateHelloParameters.count = 10;
//        Generate generateHello = new Generate(generateHelloParameters);
//
//        Generate.Parameters generateByeParameters = new Generate.Parameters();
//        generateByeParameters.payload = "Bye, World!";
//        generateByeParameters.count = 5;
//        Generate generateBye = new Generate(generateByeParameters);
//
//        Statistics stringStatistics = new Statistics(new Statistics.Parameters());
//
//        AtomicReference<JsonNode> result = new AtomicReference<>();
//
//        Gingester.Builder gBuilder = Gingester.newBuilder();
//        gBuilder.link(seed, generateHello);
//        gBuilder.link(seed, generateBye);
//        gBuilder.link(generateHello, stringStatistics);
//        gBuilder.link(generateBye, stringStatistics);
//        gBuilder.link(stringStatistics, result::set);
//        gBuilder.sync(seed, stringStatistics);
//        gBuilder.build().run();
//
//        assertEquals(2, result.get().get("distinct").intValue());
//        assertEquals("Hello, World!", result.get().get("head").get(0).get("value").textValue());
//        assertEquals(10, result.get().get("head").get(0).get("count").intValue());
//        assertEquals("Bye, World!", result.get().get("head").get(1).get("value").textValue());
//        assertEquals(5, result.get().get("head").get(1).get("count").intValue());
//    }
}
