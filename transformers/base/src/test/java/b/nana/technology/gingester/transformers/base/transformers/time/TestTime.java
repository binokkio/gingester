package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.TimeBase;
import b.nana.technology.gingester.transformers.base.transformers.string.ToTime;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestTime {

    @Test
    void testBasicIsoDateToIsoWeekDate() {

        ToTime toTime = new ToTime(new ToTime.Parameters("BASIC_ISO_DATE"));
        ToString toString = new ToString(new ToString.Parameters("ISO_WEEK_DATE"));

        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(toTime, "20210527");
        gBuilder.link(toTime, toString);
        gBuilder.link(toString, result::set);
        gBuilder.build().run();

        assertEquals("2021-W21-4", result.get());
    }

    @Test
    void testBasicIsoDateToIsoLocalDateTime() {

        ToTime.Parameters toTimeParameters = new ToTime.Parameters();
        toTimeParameters.format = "BASIC_ISO_DATE";
        toTimeParameters.defaults = Map.of(
                'H', 1,
                'm', 2,
                's', 3
        );
        ToTime toTime = new ToTime(toTimeParameters);

        ToString toString = new ToString(new TimeBase.Parameters("ISO_LOCAL_DATE_TIME"));

        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(toTime, "20210527");
        gBuilder.link(toTime, toString);
        gBuilder.link(toString, result::set);
        gBuilder.build().run();

        assertEquals("2021-05-27T01:02:03", result.get());
    }
}
