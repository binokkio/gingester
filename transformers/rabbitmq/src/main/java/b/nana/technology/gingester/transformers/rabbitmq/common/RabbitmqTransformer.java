package b.nana.technology.gingester.transformers.rabbitmq.common;

import b.nana.technology.gingester.core.Transformer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public abstract class RabbitmqTransformer<I, O> extends Transformer<I, O> {

    private final Parameters parameters;

    private Connection connection;
    private Channel channel;

    public RabbitmqTransformer(Parameters parameters) {
        super(parameters);
        requireNonNull(parameters.uri);
        requireNonNull(parameters.queue);
        this.parameters = parameters;
    }

    @Override
    protected void setup(Setup setup) {
        setup.limitWorkers(1);
    }

    @Override
    protected void open() throws Exception {

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setThreadFactory(runnable -> getThreader().newThread(runnable));
        connectionFactory.setUri(parameters.uri);
        if (parameters.username != null) connectionFactory.setUsername(parameters.username);
        if (parameters.password != null) connectionFactory.setPassword(parameters.password);

        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(
                parameters.queue,
                parameters.durable,
                parameters.exclusive,
                parameters.autoDelete,
                parameters.arguments
        );
    }

    @Override
    protected void close() throws Exception {
        channel.close();
        connection.close();
    }

    protected Channel getChannel() {
        return channel;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public static class Parameters {
        public String uri;
        public String username;
        public String password;
        public String queue;
        public boolean durable;
        public boolean exclusive;
        public boolean autoDelete;
        public boolean autoAck;
        public Map<String, Object> arguments;
    }
}