package b.nana.technology.gingester.transformers.redis;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public abstract class RedisTransformer<I, O> implements Transformer<I, O> {

    private final JedisPool jedisPool;

    public RedisTransformer(Parameters parameters) {
        jedisPool = new JedisPool(new JedisPoolConfig(), parameters.host, parameters.port);
    }

    @Override
    public void transform(Context context, I in, Receiver<O> out) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            transform(context, in, out, jedis);
        }
    }

    protected abstract void transform(Context context, I input, Receiver<O> out, Jedis jedis) throws Exception;

    public static class Parameters {
        public String host = "localhost";
        public int port = 6379;
    }
}
