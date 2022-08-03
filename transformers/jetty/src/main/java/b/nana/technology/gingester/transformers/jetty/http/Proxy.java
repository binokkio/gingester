package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public final class Proxy implements Transformer<InputStream, InputStream> {

    private static final Set<String> RESTRICTED_HEADERS = Set.of("Connection", "Host");
    private static final FetchKey FETCH_HTTP = new FetchKey("http");

    private final String proxyRoot;
    private final Map<String, Template> extraHeaders;
    private final boolean prepareProxyResponse;

    public Proxy(Parameters parameters) {
        proxyRoot = requireNonNull(parameters.proxyRoot);
        extraHeaders = parameters.extraHeaders.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Context.newTemplate(e.getValue())));
        prepareProxyResponse = parameters.prepareProxyResponse;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        Map<?, ?> http = (Map<?, ?>) context.require(FETCH_HTTP);
        Map<?, ?> httpRequest = (Map<?, ?>) http.get("request");

        // start building proxy request uri by concatenating the proxy root and client request path
        StringBuilder proxyRequestUri = new StringBuilder()
                .append(proxyRoot)
                .append(httpRequest.get("path"));

        // add query string if present
        String queryString = (String) httpRequest.get("queryString");
        if (queryString != null)
            proxyRequestUri
                    .append('?')
                    .append(queryString);

        // start building the proxy request using the client method and rewritten uri
        HttpRequest.Builder proxyRequestBuilder = HttpRequest.newBuilder()
                .method((String) httpRequest.get("method"), HttpRequest.BodyPublishers.ofInputStream(() -> in))
                .uri(URI.create(proxyRequestUri.toString()));

        // copy all but the restricted headers
        Map<?, ?> headers = (Map<?, ?>) httpRequest.get("headers");
        if (headers != null)
            headers.forEach((name, value) -> {
                if (!RESTRICTED_HEADERS.contains(name)) {
                    proxyRequestBuilder.setHeader((String) name, (String) value);
                }
            });

        extraHeaders.forEach(
                (name, value) -> proxyRequestBuilder.setHeader(name, value.render(context)));

        // proxy the request, TODO look into HttpClient thread safety
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<InputStream> proxyResponse = httpClient.send(proxyRequestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        int proxyResponseStatus = proxyResponse.statusCode();
        Map<String, List<String>> proxyResponseHeaders = proxyResponse.headers().map();

        if (prepareProxyResponse) {
            // transfer proxy response status and headers to upstream response
            Server.ResponseWrapper response = (Server.ResponseWrapper) http.get("response");
            response.setStatus(proxyResponseStatus);
            proxyResponseHeaders.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        }

        Context.Builder contextBuilder = context.stash(Map.of(
                "description", proxyRequestUri,
                "status", proxyResponseStatus,
                "headers", proxyResponseHeaders
        ));

        try (InputStream proxyResponseBody = proxyResponse.body()) {
            out.accept(contextBuilder, proxyResponseBody);
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, proxyRoot -> o("proxyRoot", proxyRoot));
            }
        }

        public String proxyRoot;
        public Map<String, TemplateParameters> extraHeaders = Map.of();
        public boolean prepareProxyResponse = true;
    }
}
