package b.nana.technology.gingester.transformers.redis.common;

import b.nana.technology.gingester.core.Context;
import com.fasterxml.jackson.annotation.JsonCreator;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;

public abstract class KeyTransformer<I, O> extends RedisTransformer<I, O> {

    private final Context.StringFormat keyFormat;

    public KeyTransformer(Parameters parameters) {
        super(parameters);
        keyFormat = new Context.StringFormat(parameters.keyFormat, s -> s, true);
    }

    protected final Context.StringFormat getKeyFormat() {
        return keyFormat;
    }

    public static class Parameters extends RedisTransformer.Parameters {

        public String keyFormat;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String keyFormat) {
            this.keyFormat = keyFormat;
        }
    }
}
