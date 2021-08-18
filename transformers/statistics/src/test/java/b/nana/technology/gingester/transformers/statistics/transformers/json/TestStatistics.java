package b.nana.technology.gingester.transformers.statistics.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.Split;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToJson;
import b.nana.technology.gingester.transformers.base.transformers.json.FromDsvInputStream;
import b.nana.technology.gingester.transformers.base.transformers.resource.Open;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

class TestStatistics {

    @Test
    void test() {

        Open.Parameters openParameters = new Open.Parameters("/basic.csv");
        Open open = new Open(openParameters);
        FromDsvInputStream fromDsvInputStream = new FromDsvInputStream(new FromDsvInputStream.Parameters());
        Statistics statistics = new Statistics(new Statistics.Parameters());

        AtomicReference<JsonNode> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(open, fromDsvInputStream);
        gBuilder.link(fromDsvInputStream, statistics);
        gBuilder.link(statistics, result::set);
        gBuilder.sync(open, statistics);
        gBuilder.build().run();

//        System.out.println(result.get().toPrettyString());
    }

    @Test
    void testJson() {

        Open.Parameters openParameters = new Open.Parameters("/basic.ndjson");
        Open open = new Open(openParameters);
        Split split = new Split(new Split.Parameters("\n"));
        ToJson toJson = new ToJson(new ToJsonBase.Parameters());
        Statistics statistics = new Statistics(new Statistics.Parameters());

        AtomicReference<JsonNode> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(open, split);
        gBuilder.link(split, toJson);
        gBuilder.link(toJson, statistics);
        gBuilder.link(statistics, result::set);
        gBuilder.sync(open, statistics);
        gBuilder.build().run();

//        System.out.println(result.get().toPrettyString());
    }
}
