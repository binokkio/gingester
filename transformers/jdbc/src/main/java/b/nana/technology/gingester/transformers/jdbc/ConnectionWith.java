package b.nana.technology.gingester.transformers.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConnectionWith<T> {

    private final String url;
    private final Connection connection;

    private T singleton;
    private final Map<String, T> map = new LinkedHashMap<>(16, .75f, true);  // TODO use LruMap?
    private final int maxMapSize;

    public ConnectionWith(String url, Connection connection, int maxMapSize) {
        this.url = url;
        this.connection = connection;
        this.maxMapSize = maxMapSize;
    }

    public String getUrl() {
        return url;
    }

    public Connection getConnection() {
        return connection;
    }

    public T getSingleton() {
        return singleton;
    }

    public void setSingleton(T singleton) {
        this.singleton = singleton;
    }

    public T getObject(String raw) {
        return map.get(raw);
    }

    public T setObject(String raw, T object) {

        // TODO is the order of put and containsKey the wrong way around here?

        map.put(raw, object);

        if (!map.containsKey(raw) && map.size() == maxMapSize) {
            Iterator<T> iterator = map.values().iterator();
            T removed = iterator.next();
            iterator.remove();
            return removed;
        }

        return null;
    }

    public void close() throws SQLException {
        connection.close();
    }
}
