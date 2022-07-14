package b.nana.technology.gingester.transformers.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

public final class MixedConnectionsPool<T> {

    private final Object lock = new Object();
    private final LinkedList<ConnectionWith<T>> idle = new LinkedList<>();

    private final int poolSize;
    private final int numPreparedStatements;
    private final OnConnectionCreatedListener onConnectionCreated;
    private int used;

    public MixedConnectionsPool(int poolSize, int numPreparedStatements, OnConnectionCreatedListener onConnectionCreated) {
        this.poolSize = poolSize;
        this.numPreparedStatements = numPreparedStatements;
        this.onConnectionCreated = onConnectionCreated;
    }

    public ConnectionWith<T> acquire(String url) throws InterruptedException, SQLException {
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

                    // close the oldest idle connection if the pool would otherwise overflow
                    if (used + idle.size() == poolSize) {
                        idle.removeLast().close();
                    }

                    break;  // break out of the loop and synchronized block to create a new connection
                }
            }
        }

        // create a new connection
        ConnectionWith<T> connection = new ConnectionWith<>(
                url,
                DriverManager.getConnection(url),
                numPreparedStatements
        );

        onConnectionCreated.onConnectionCreated(connection.getConnection());

        used++;
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
            connection.close();
        }
    }

    public interface OnConnectionCreatedListener {
        void onConnectionCreated(Connection connection) throws SQLException;
    }
}
