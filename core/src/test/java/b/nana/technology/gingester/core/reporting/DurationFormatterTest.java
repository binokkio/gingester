package b.nana.technology.gingester.core.reporting;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationFormatterTest {

    @Test
    public void test() {

        DurationFormatter durationFormatter = new DurationFormatter();

        assertEquals(Duration.ofHours(1).plusSeconds(10), durationFormatter.parse("1h10s"));
        assertEquals(Duration.ofDays(10), durationFormatter.parse("10d"));

        assertThrows(IllegalArgumentException.class, () -> durationFormatter.parse("1"));
        assertThrows(IllegalArgumentException.class, () -> durationFormatter.parse("1l"));
        assertThrows(IllegalArgumentException.class, () -> durationFormatter.parse("1ml"));
        assertThrows(IllegalArgumentException.class, () -> durationFormatter.parse("1h0"));
        assertThrows(IllegalArgumentException.class, () -> durationFormatter.parse("d"));
        assertThrows(IllegalArgumentException.class, () -> durationFormatter.parse("asdf"));
    }
}