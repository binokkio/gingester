package b.nana.technology.gingester.transformers.base.transformers.json.extract;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.string.Format;
import b.nana.technology.gingester.transformers.base.transformers.string.ToJson;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestTime {

    @Test
    void test() {

        ToJson toJson = new ToJson(new ToJsonBase.Parameters());

        Time.Parameters timeParameters = new Time.Parameters();
        timeParameters.jsonPath = "$.time";
        timeParameters.format = "yyyy-MM-dd";
        Time time = new Time(timeParameters);

        Format format = new Format(new Format.Parameters("{time.year}"));

        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(toJson, "{\"hello\":\"world\",\"time\":\"2021-05-26\"}");
        gBuilder.link(toJson, time);
        gBuilder.link(time, format);
        gBuilder.link(format, result::set);
        gBuilder.build().run();

        assertEquals("2021", result.get());
    }
}
