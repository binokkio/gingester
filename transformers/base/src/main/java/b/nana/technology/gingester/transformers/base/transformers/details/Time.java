package b.nana.technology.gingester.transformers.base.transformers.details;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Passthrough;

import java.time.ZonedDateTime;
import java.util.Map;

public class Time<T> extends Passthrough<T> {

    @Override
    protected void transform(Context context, T input) throws Exception {

        ZonedDateTime now = ZonedDateTime.now();

        emit(context.extend(this)
                .stash(Map.of(
                        "time", Map.of(
                                "year", now.getYear(),
                                "month", now.getMonthValue(),
                                "day", now.getDayOfMonth(),
                                "hour", now.getHour(),
                                "minute", now.getMinute(),
                                "second", now.getSecond(),
                                "milli", now.getNano() / 1_000_000,
                                "nano", now.getNano() % 1_000_000)
                        )
                ),
                input
        );
    }
}
