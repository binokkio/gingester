package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.TimeBase;
import b.nana.technology.gingester.transformers.base.transformers.string.ToTime;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestTime {

    @Test
    void testBasicIsoDateToIsoWeekDate() {

        ToTime toTime = new ToTime(new TimeBase.Parameters("BASIC_ISO_DATE"));
        ToString toString = new ToString(new ToString.Parameters("ISO_WEEK_DATE"));

        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(toTime, "20210527");
        gBuilder.link(toTime, toString);
        gBuilder.link(toString, result::set);
        gBuilder.build().run();

        assertEquals("2021-W21-4", result.get());
    }
}
