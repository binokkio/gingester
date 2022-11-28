package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Server implements Transformer<Object, InputStream> {

    private final int port;
    private final SslContextFactory.Server sslContextFactory;
    private final boolean stashHeaders;
    private final boolean stashQuery;
    private final boolean stashCookies;
    private final int inflateBufferSize;
    private final String[] gzipIncludedMethods;
    private final String[] gzipExcludedMethods;
    private final String[] gzipIncludedMimeTypes;
    private final String[] gzipExcludedMimeTypes;
    private final String[] gzipIncludedPaths;
    private final String[] gzipExcludedPaths;

    public Server(Parameters parameters) {
        port = parameters.port;
        stashHeaders = parameters.stashHeaders;
        stashQuery = parameters.stashQuery;
        stashCookies = parameters.stashCookies;
        inflateBufferSize = parameters.inflateBufferSize;
        gzipIncludedMethods = getIncludedExcluded(parameters.gzipMethods, true);
        gzipExcludedMethods = getIncludedExcluded(parameters.gzipMethods, false);
        gzipIncludedMimeTypes = getIncludedExcluded(parameters.gzipMimeTypes, true);
        gzipExcludedMimeTypes = getIncludedExcluded(parameters.gzipMimeTypes, false);
        gzipIncludedPaths = getIncludedExcluded(parameters.gzipPaths, true);
        gzipExcludedPaths = getIncludedExcluded(parameters.gzipPaths, false);

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

    private String[] getIncludedExcluded(String[] values, boolean returnIncluded) {
        return Arrays.stream(values)
                .filter(s -> returnIncluded != s.startsWith("!"))
                .map(s -> returnIncluded ? s : s.substring(1))
                .toArray(String[]::new);
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

        Handler handler = new AbstractHandler() {

            @Override
            public void handle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

                jettyRequest.setHandled(true);
                jettyRequest.setContentType("application/octet-stream");  // prevent special handling of request body

                Map<String, Object> httpStash = new HashMap<>();

                Map<String, Object> httpRequestStash = new HashMap<>();
                httpStash.put("request", httpRequestStash);
                httpRequestStash.put("object", new HttpRequestWrapper(request));
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
                    httpRequestStash.put("queryString", jettyRequest.getQueryString());
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

                HttpResponseWrapper responseWrapper = new HttpResponseWrapper(response);

                httpStash.put("response", responseWrapper);

                Context.Builder contextBuilder = context.stash(Map.of(
                        "description", jettyRequest.getMethod() + " " + target,
                        "http", httpStash
                ));

                out.accept(contextBuilder, jettyRequest.getInputStream());
                responseWrapper.await();
            }
        };

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setInflateBufferSize(inflateBufferSize);
        gzipHandler.setIncludedMethods(gzipIncludedMethods);
        gzipHandler.setExcludedMethods(gzipExcludedMethods);
        gzipHandler.setIncludedMimeTypes(gzipIncludedMimeTypes);
        gzipHandler.setExcludedMimeTypes(gzipExcludedMimeTypes);
        gzipHandler.setIncludedPaths(gzipIncludedPaths);
        gzipHandler.setExcludedPaths(gzipExcludedPaths);

        gzipHandler.setHandler(handler);
        server.setHandler(gzipHandler);

        server.start();
        server.join();
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, port -> o("port", port));
            }
        }

        public int port = 8080;
        public boolean stashHeaders = true;
        public boolean stashQuery = true;
        public boolean stashCookies = true;
        public StoreParameters trustStore;
        public StoreParameters keyStore;
        public int inflateBufferSize = 8192;
        public String[] gzipMethods = new String[] { "GET" };
        public String[] gzipMimeTypes = new String[0];
        public String[] gzipPaths = new String[] { "/*" };
    }

    public static class StoreParameters {
        public String path;
        public String password;
    }
}
