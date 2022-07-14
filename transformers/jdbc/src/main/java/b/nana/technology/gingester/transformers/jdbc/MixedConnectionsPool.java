package b.nana.technology.gingester.transformers.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

public final class MixedConnectionsPool<T> {

    private final Object lock = new Object();
    private final LinkedList<ConnectionWith<T>> idle = new LinkedList<>();

    private final int poolSize;
    private final int withSize;
    private final ConnectionWithConsumer<T> onConnectionCreated;
    private final ConnectionWithConsumer<T> onConnectionMoribund;

    private int used;

    public MixedConnectionsPool(int poolSize, int withSize, ConnectionWithConsumer<T> onConnectionCreated, ConnectionWithConsumer<T> onConnectionMoribund) {
        this.poolSize = poolSize;
        this.withSize = withSize;
        this.onConnectionCreated = onConnectionCreated;
        this.onConnectionMoribund = onConnectionMoribund;
    }

    public ConnectionWith<T> acquire(String url) throws InterruptedException, SQLException {

        ConnectionWith<T> moribund = null;

        synchronized (lock) {
            while (true) {

                // try to reuse one of the idle connections
                Iterator<ConnectionWith<T>> iterator = idle.iterator();
                while (iterator.hasNext()) {
                    ConnectionWith<T> connection = iterator.next();
                    if (connection.getUrl().equals(url)) {
                        iterator.remove();
                        used++;
                        return connection;
                    }
                }

                // no idle connection for the given url

                if (used == poolSize) {

                    // all connections are in use, wait for one to be released...
                    lock.wait();
                    // ...then loop back to retrying idle connections

                } else {

                    // get the oldest idle connection if the pool would otherwise overflow
                    if (used + idle.size() == poolSize) {
                        moribund = idle.removeLast();
                    }

                    used++;
                    break;  // break out of the loop and synchronized block to create a new connection
                }
            }
        }

        // TODO handle exceptions from onConnectionMoribund and onConnectionCreated

        // close oldest idle connection if the pool would otherwise overflow
        if (moribund != null) {
            onConnectionMoribund.accept(moribund);
            moribund.close();
        }

        // create a new connection
        ConnectionWith<T> connection = new ConnectionWith<>(
                url,
                DriverManager.getConnection(url),
                withSize
        );

        onConnectionCreated.accept(connection);

        return connection;
    }

    public void release(ConnectionWith<T> connection) {
        synchronized (lock) {
            used--;
            idle.addFirst(connection);
            lock.notify();
        }
    }

    public void close() throws SQLException {

        if (used != 0) {
            throw new IllegalStateException("Can't close MixedConnectionsPool while connections are in use");
        }

        for (ConnectionWith<T> connection : idle) {
            onConnectionMoribund.accept(connection);
            connection.close();
        }
    }

    public interface ConnectionWithConsumer<T> {
        void accept(ConnectionWith<T> connection) throws SQLException;
    }
}
