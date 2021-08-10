package b.nana.technology.gingester.transformers.redis.common;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public abstract class RedisTransformer<I, O> extends Transformer<I, O> {

    private final JedisPool jedisPool;

    public RedisTransformer(Parameters parameters) {
        super(parameters);
        jedisPool = new JedisPool(new JedisPoolConfig(), parameters.host, parameters.port);
    }

    @Override
    protected final void transform(Context context, I input) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            transform(context, input, jedis);
        }
    }

    protected abstract void transform(Context context, I input, Jedis jedis) throws Exception;

    public static class Parameters {
        public String host = "localhost";
        public int port = 6379;
    }
}
