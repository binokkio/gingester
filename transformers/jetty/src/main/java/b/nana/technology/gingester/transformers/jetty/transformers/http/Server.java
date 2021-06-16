package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Server extends Transformer<Void, InputStream> {

    private final int port;
    private final SslContextFactory.Server sslContextFactory;
    private final boolean stashHeaders;
    private final boolean stashQuery;

    public Server(Parameters parameters) {
        super(parameters);
        port = parameters.port;
        stashHeaders = parameters.stashHeaders;
        stashQuery = parameters.stashQuery;

        if (parameters.keyStore != null) {
            sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(parameters.keyStore.path);
            sslContextFactory.setKeyStorePassword(parameters.keyStore.password);
            if (parameters.trustStore != null) {
                sslContextFactory.setTrustStorePath(parameters.trustStore.path);
                sslContextFactory.setTrustStorePassword(parameters.trustStore.password);
                sslContextFactory.setNeedClientAuth(true);
            }
        } else if (parameters.trustStore != null) {
            throw new IllegalArgumentException("Can't have trustStore without keyStore");
        } else {
            sslContextFactory = null;
        }
    }

    @Override
    protected void setup(Setup setup) {
        setup.requireDownstreamSync();
        setup.limitWorkers(1);
    }

    @Override
    protected void transform(Context context, Void input) throws Exception {

        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(new ServerQueuedThreadPool());

        ServerConnector connector = sslContextFactory != null ?
                new ServerConnector(server, sslContextFactory) :
                new ServerConnector(server);

        connector.setPort(port);
        server.addConnector(connector);

        server.setHandler(new AbstractHandler() {

            @Override
            public void handle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

                jettyRequest.setHandled(true);
                jettyRequest.setContentType("application/octet-stream");

                Context.Builder contextBuilder = context.extend(Server.this)
                        .description(jettyRequest.getMethod() + " " + target);

                Map<String, Object> stash = new HashMap<>();

                stash.put("request", Map.of(
                        "method", jettyRequest.getMethod(),
                        "path", target
                ));

                stash.put("response", response);

                if (stashHeaders) {
                    Map<String, String> headers = new HashMap<>();
                    jettyRequest.getHeaderNames().asIterator().forEachRemaining(
                            headerName -> headers.put(headerName, jettyRequest.getHeader(headerName)));
                    stash.put("headers", headers);
                }

                if (stashQuery) {
                    Map<String, String> query = new HashMap<>();
                    jettyRequest.getParameterMap();  // triggers query parameter initialization, TODO check proper solution
                    jettyRequest.getQueryParameters().forEach(
                            (queryParameterName, value) -> query.put(queryParameterName, value.get(value.size() - 1)));
                    stash.put("query", query);
                }

                contextBuilder.stash(stash);

                emit(
                        contextBuilder,
                        request.getInputStream()
                );
            }
        });

        server.start();
        server.join();
    }

    private class ServerQueuedThreadPool extends QueuedThreadPool {

        @Override
        public Thread newThread(Runnable runnable) {
            return getThreader().newThread(runnable);
        }
    }

    public static class Parameters {

        public int port = 8080;
        public boolean stashHeaders = true;
        public boolean stashQuery = true;
        public StoreParameters trustStore;
        public StoreParameters keyStore;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int port) {
            this.port = port;
        }
    }

    public static class StoreParameters {
        public String path;
        public String password;
    }
}
