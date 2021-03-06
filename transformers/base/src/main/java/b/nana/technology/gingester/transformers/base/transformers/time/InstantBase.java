package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;

public abstract class InstantBase<I> implements Transformer<I, TemporalAccessor> {

    private final ZoneId zoneId;

    public InstantBase(Parameters parameters) {
        zoneId = parameters.zone != null ? ZoneId.of(parameters.zone) : ZoneId.systemDefault();
    }

    protected final ZoneId getZoneId() {
        return zoneId;
    }

    public static class Parameters {

        public String zone;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String zone) {
            this.zone = zone;
        }
    }
}
