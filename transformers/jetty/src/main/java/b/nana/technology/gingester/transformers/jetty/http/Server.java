package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class Server implements Transformer<Object, InputStream> {

    private final int port;
    private final SslContextFactory.Server sslContextFactory;
    private final boolean stashHeaders;
    private final boolean stashQuery;
    private final boolean stashCookies;

    public Server(Parameters parameters) {
        port = parameters.port;
        stashHeaders = parameters.stashHeaders;
        stashQuery = parameters.stashQuery;
        stashCookies = parameters.stashCookies;

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
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
        controls.maxWorkers(1);
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) throws Exception {

        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSendDateHeader(false);
        httpConfiguration.setSendServerVersion(false);

        ServerConnector connector = sslContextFactory != null ?
                new ServerConnector(server, sslContextFactory, new HttpConnectionFactory(httpConfiguration)) :
                new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));

        connector.setPort(port);
        server.addConnector(connector);

        server.setHandler(new AbstractHandler() {

            @Override
            public void handle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

                jettyRequest.setHandled(true);
                jettyRequest.setContentType("application/octet-stream");  // prevent special handling of request body

                Map<String, Object> httpStash = new HashMap<>();

                httpStash.put("request", Map.of(
                        "method", jettyRequest.getMethod(),
                        "path", target
                ));

                Map<String, Object> httpRequestStash = new HashMap<>();
                httpStash.put("request", httpRequestStash);
                httpRequestStash.put("method", jettyRequest.getMethod());
                httpRequestStash.put("path", target);

                if (stashHeaders) {
                    Map<String, String> headers = new HashMap<>();
                    jettyRequest.getHeaderNames().asIterator().forEachRemaining(
                            headerName -> headers.put(headerName, jettyRequest.getHeader(headerName)));
                    httpRequestStash.put("headers", headers);
                }

                if (stashQuery) {
                    Map<String, String> query = new HashMap<>();
                    jettyRequest.getParameterMap();  // trigger query parameter initialization, TODO check proper solution
                    jettyRequest.getQueryParameters().forEach(
                            (queryParameterName, value) -> query.put(queryParameterName, value.get(value.size() - 1)));
                    httpRequestStash.put("query", query);
                }

                if (stashCookies) {
                    Cookie[] cookies = jettyRequest.getCookies();
                    Map<String, Cookie> cookieMap = new HashMap<>();
                    if (cookies != null) {
                        for (Cookie cookie : jettyRequest.getCookies()) {
                            cookieMap.put(cookie.getName(), cookie);
                        }
                    }
                    httpRequestStash.put("cookies", cookieMap);
                }

                httpStash.put("response", Map.of(
                        "servlet", response
                ));

                Context.Builder contextBuilder = context.stash(Map.of(
                        "description", jettyRequest.getMethod() + " " + target,
                        "http", httpStash
                ));

                out.accept(contextBuilder, request.getInputStream());
            }
        });

        server.start();
        server.join();
    }

    public static class Parameters {

        public int port = 8080;
        public boolean stashHeaders = true;
        public boolean stashQuery = true;
        public boolean stashCookies = true;
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
