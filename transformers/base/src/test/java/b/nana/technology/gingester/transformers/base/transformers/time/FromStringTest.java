package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FromStringTest {

    @Test
    void testDefaults() throws Exception {

        FromString.Parameters parameters = new FromString.Parameters();
        parameters.format = "Q-yyyy";
        parameters.defaults = Map.of(
            "dayOfQuarter", 10,
            "nanoOfDay", 0
        );
        parameters.zone = "UTC";

        AtomicReference<TemporalAccessor> result = new AtomicReference<>();
        new FromString(parameters).transform(null, "2-2022", (UniReceiver<TemporalAccessor>) result::set);

        assertEquals(ZonedDateTime.parse("2022-04-10T00:00Z[UTC]"), ZonedDateTime.from(result.get()));
    }

    @Test
    void testUnknownDefaultThrows() {

        FromString.Parameters parameters = new FromString.Parameters();
        parameters.format = "Q-yyyy";
        parameters.defaults = Map.of(
            "unknownDefault", 123
        );
        parameters.zone = "UTC";

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new FromString(parameters));
        assertEquals("Unknown default: unknownDefault", e.getMessage());
    }
}