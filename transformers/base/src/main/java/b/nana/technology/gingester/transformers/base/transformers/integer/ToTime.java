package b.nana.technology.gingester.transformers.base.transformers.integer;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;

public class ToTime extends Transformer<Long, TemporalAccessor> {

    private final ZoneId zone;

    public ToTime(Parameters parameters) {
        super(parameters);
        zone = ZoneId.of(parameters.zone);
    }

    @Override
    protected void transform(Context context, Long input) throws Exception {
        emit(context, Instant.ofEpochMilli(input).atZone(zone));
    }

    public static class Parameters {

        public String zone = "UTC";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String zone) {
            this.zone = zone;
        }
    }
}
