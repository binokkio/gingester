package b.nana.technology.gingester.transformers.base.transformers.http;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Names(1)
public final class Http implements Transformer<Object, InputStream> {

    private final String method;
    private final TemplateMapper<URI> uriTemplate;
    private final Map<String, Template> headers;
    private final HttpClient httpClient;
    private final Function<Object, HttpRequest.BodyPublisher> bodyPublisher;
    private final boolean stashPeerCertificates;

    public Http(Parameters parameters) {
        method = parameters.method;
        uriTemplate = Context.newTemplateMapper(parameters.uri, URI::create);
        headers = parameters.headers.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Context.newTemplate(e.getValue())
                ));
        httpClient = HttpClient.newBuilder().followRedirects(parameters.followRedirects).build();
        bodyPublisher = getBodyPublisher();
        stashPeerCertificates = parameters.stashPeerCertificates;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public Class<?> getInputType() {
        return switch (method) {
            case "HEAD", "GET", "DELETE" -> Object.class;
            case "PATCH", "POST", "PUT" -> InputStream.class;
            default -> throw new IllegalStateException("No case for " + method);
        };
    }

    private Function<Object, HttpRequest.BodyPublisher> getBodyPublisher() {
        return switch (method) {
            case "HEAD", "GET", "DELETE" -> o -> HttpRequest.BodyPublishers.noBody();
            case "PATCH", "POST", "PUT" -> o -> HttpRequest.BodyPublishers.ofInputStream(() -> (InputStream) o);
            default -> throw new IllegalStateException("No case for " + method);
        };
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) throws Exception {

        URI uri = uriTemplate.render(context, in);

        HttpRequest.Builder requestBuilder = HttpRequest
                .newBuilder(uri)
                .method(method, bodyPublisher.apply(in));

        headers.forEach((name, template) ->
                requestBuilder.header(name, template.render(context, in)));

        HttpResponse<InputStream> response =
                httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());

        Context.Builder contextBuilder;

        if (stashPeerCertificates) {

            List<Certificate> peerCertificates = response.sslSession()
                    .map(s -> {
                        try {
                            return s.getPeerCertificates();
                        } catch (SSLPeerUnverifiedException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(Arrays::asList)
                    .orElse(List.of());


            contextBuilder = context.stash(Map.of(
                    "description", uri,
                    "status", response.statusCode(),
                    "headers", response.headers().map(),
                    "peerCertificates", peerCertificates
            ));
        } else {
            contextBuilder = context.stash(Map.of(
                    "description", uri,
                    "status", response.statusCode(),
                    "headers", response.headers().map()
            ));
        }

        try (InputStream body = response.body()) {
            out.accept(contextBuilder, body);
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "method", "uri", "headers", "followRedirects" })
    public static class Parameters {
        public String method;
        public TemplateParameters uri;
        public Map<String, TemplateParameters> headers = Collections.emptyMap();
        public HttpClient.Redirect followRedirects = HttpClient.Redirect.NORMAL;
        public boolean stashPeerCertificates;
    }
}
