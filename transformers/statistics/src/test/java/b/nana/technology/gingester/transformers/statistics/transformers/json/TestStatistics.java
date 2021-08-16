package b.nana.technology.gingester.transformers.statistics.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.transformers.json.FromDsvInputStream;
import b.nana.technology.gingester.transformers.base.transformers.json.ToString;
import b.nana.technology.gingester.transformers.base.transformers.resource.Open;
import org.junit.jupiter.api.Test;

class TestStatistics {

    @Test
    void test() {

        Open.Parameters openParameters = new Open.Parameters("/basic.csv");
        Open open = new Open(openParameters);
        FromDsvInputStream fromDsvInputStream = new FromDsvInputStream(new FromDsvInputStream.Parameters());
        Statistics.Parameters statisticsParameters = new Statistics.Parameters();
        statisticsParameters.put("", new Statistics.NodeConfiguration());
        statisticsParameters.get("").disabled = true;
        statisticsParameters.put("/popularity", new Statistics.NodeConfiguration());
        statisticsParameters.get("/popularity").disabled = false;
        Statistics statistics = new Statistics(statisticsParameters);
        ToString.Parameters toStringParameters = new ToString.Parameters(true);
        ToString toString = new ToString(toStringParameters);

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(open, fromDsvInputStream);
        gBuilder.link(fromDsvInputStream, statistics);
        gBuilder.link(statistics, toString);
        gBuilder.link(toString, System.out::println);
        gBuilder.sync(open, statistics);
        gBuilder.build().run();
    }
}
