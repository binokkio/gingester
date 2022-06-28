package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

import static org.junit.jupiter.api.Assertions.*;

class FromMillisTest {

    @Test
    void test() {
        new FromMillis(new InstantBase.Parameters()).transform(null, 1630958048123L, (UniReceiver<TemporalAccessor>) i -> {
            // TODO risky testing here, if the receiver does not get called the test will still pass
            assertEquals(2021, i.get(ChronoField.YEAR));
            assertEquals(9, i.get(ChronoField.MONTH_OF_YEAR));
            assertEquals(6, i.get(ChronoField.DAY_OF_MONTH));
        });
    }
}