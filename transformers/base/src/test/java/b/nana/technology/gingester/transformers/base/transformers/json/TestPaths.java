package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.bytes.ToString;
import b.nana.technology.gingester.transformers.base.transformers.object.Distinct;
import b.nana.technology.gingester.transformers.base.transformers.string.ToBytes;
import b.nana.technology.gingester.transformers.base.transformers.string.ToJson;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPaths {

    @Test
    void test() {

        ToJson toJson = new ToJson(new ToJsonBase.Parameters());

        Paths.Parameters parameters = new Paths.Parameters();
        Paths paths = new Paths(parameters);
        Distinct<String> distinct = new Distinct<>(new Distinct.Parameters(true));
        ToBytes toBytes = new ToBytes();
        ToString toString = new ToString();

        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.put("hello", "world");

        ObjectNode nested = JsonNodeFactory.instance.objectNode();
        input.set("nested", nested);
        nested.put("hello", "nested world");

        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        input.set("array", array);

        array.add("hello array world");
        array.add("hello again array world");

        for (int i = 0; i < 5; i++) {
            ObjectNode arrayObject = JsonNodeFactory.instance.objectNode();
            arrayObject.put("hello", "array nested world");
            array.add(arrayObject);
        }

        Queue<String> results = new LinkedBlockingQueue<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(toJson, input.toString());
        gBuilder.link(toJson, paths);
        gBuilder.link(paths, distinct);
        gBuilder.link(distinct, toBytes);  // solution to type resolving, TODO improve
        gBuilder.link(toBytes, toString);
        gBuilder.link(toString, results::add);
        gBuilder.sync(toJson, distinct);
        gBuilder.build().run();

        assertEquals("$.array[]", results.remove());
        assertEquals("$.array[].hello", results.remove());
        assertEquals("$.hello", results.remove());
        assertEquals("$.nested.hello", results.remove());
        assertTrue(results.isEmpty());
    }
}
