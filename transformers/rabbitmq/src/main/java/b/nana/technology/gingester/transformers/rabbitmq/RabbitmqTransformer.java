package b.nana.technology.gingester.transformers.rabbitmq;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public abstract class RabbitmqTransformer<I, O> implements Transformer<I, O> {

    private final Parameters parameters;

    private Connection connection;
    private Channel channel;

    public RabbitmqTransformer(Parameters parameters) {
        requireNonNull(parameters.uri);
        requireNonNull(parameters.queue);
        this.parameters = parameters;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.maxWorkers(1);
    }

    @Override
    public void open() throws Exception {

        ConnectionFactory connectionFactory = new ConnectionFactory();
//        connectionFactory.setThreadFactory(runnable -> getThreader().newThread(runnable));
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
    public void close() throws Exception {
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