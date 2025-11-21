package b.nana.technology.gingester.transformers.base.transformers.http;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.CacheKey;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.reporting.DurationFormatter;
import b.nana.technology.gingester.core.reporting.SamplerFormatter;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Names(1)
public final class Http implements Transformer<Object, InputStream> {

    private static final DurationFormatter durationFormatter = new DurationFormatter();

    private final String method;
    private final TemplateMapper<URI> uriTemplate;
    private final Map<String, Template> headers;
    private final HttpClient httpClient;
    private final Function<Object, HttpRequest.BodyPublisher> bodyPublisher;
    private final List<Integer> retry;
    private final List<Duration> backoff;
    private final boolean stashPeerCertificates;
    private final long[][] retries;

    public Http(Parameters parameters) {


        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .followRedirects(parameters.followRedirects);

        if (parameters.allowUntrusted) {
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                httpClientBuilder.sslContext(sslContext);
            } catch (Exception e) {
                throw new RuntimeException(e);  // TODO
            }
        }

        httpClient = httpClientBuilder.build();

        method = parameters.method;
        uriTemplate = Context.newTemplateMapper(parameters.uri, URI::create);
        headers = parameters.headers.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Context.newTemplate(e.getValue())
                ));
        bodyPublisher = getBodyPublisher();
        retry = parameters.retry;
        backoff = parameters.backoff.stream().map(durationFormatter::parse).toList();
        stashPeerCertificates = parameters.stashPeerCertificates;
        retries = new long[retry.size()][backoff.size()];
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
    public CacheKey getCacheKey(Context context, Object in) {

        CacheKey cacheKey = new CacheKey()
                .add(method)
                .add(uriTemplate.render(context, in))
                .add(retry);

        // TODO allow parameters to influence which headers are added
        headers.values().stream()
                .map(t -> t.render(context, in))
                .forEach(cacheKey::add);

        // add input value for PATCH, POST, and PUT
        if (method.charAt(0) == 'P') cacheKey.add(in);

        return cacheKey;
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) throws Exception {

        URI uri = uriTemplate.render(context, in);

        HttpRequest.Builder requestBuilder = HttpRequest
                .newBuilder(uri)
                .method(method, bodyPublisher.apply(in));

        headers.forEach((name, template) ->
                requestBuilder.header(name, template.render(context, in)));

        HttpResponse<InputStream> response;
        int status;

        if (retry.isEmpty()) {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
            status = response.statusCode();
        } else {

            int attempt = 1;
            while (true) {

                response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
                status = response.statusCode();

                // if the status code is not listed as a retry, break out of the retry loop
                int retryIndex = retry.indexOf(status);
                if (retryIndex == -1)
                    break;

                int backoffIndex = Math.min(backoff.size() - 1, attempt - 1);

                // increment the backoff and retry count for this status code
                synchronized (retries) {
                    retries[retryIndex][backoffIndex]++;
                }

                // consume body, TODO improve
                try (InputStream body = response.body()) {
                    body.readAllBytes();
                }

                // backoff
                //noinspection BusyWait
                Thread.sleep(backoff.get(backoffIndex).toMillis());

                attempt++;
            }
        }


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
                    "status", status,
                    "headers", response.headers().map(),
                    "peerCertificates", peerCertificates
            ));
        } else {
            contextBuilder = context.stash(Map.of(
                    "description", uri,
                    "status", status,
                    "headers", response.headers().map()
            ));
        }

        try (InputStream body = response.body()) {
            out.accept(contextBuilder, body);
        }
    }

    @Override
    public String onReport() {

        synchronized (retries) {

            String prefix = "Backoff and retry per status code: ";
            StringBuilder stringBuilder = new StringBuilder(prefix);

            for (int i = 0; i < retries.length; i++) {

                if (retries[i][0] != 0) {

                    stringBuilder
                            .append("{ ")
                            .append(retry.get(i))
                            .append(": ");

                    Duration total = Duration.ZERO;

                    for (int j = 0; j < retries[i].length && retries[i][j] != 0; j++) {

                        total = total.plus(backoff.get(j).multipliedBy(retries[i][j]));

                        stringBuilder
                                .append(SamplerFormatter.DEFAULT_SAMPLE_FORMATTER.format(retries[i][j]))
                                .append('x')
                                .append(durationFormatter.format(backoff.get(j)))
                                .append(" + ");
                    }

                    stringBuilder.setLength(stringBuilder.length() - 3);

                    stringBuilder
                            .append(" = ")
                            .append(durationFormatter.format(total))
                            .append(" }, ");
                }
            }

            if (stringBuilder.length() == prefix.length())
                return Transformer.super.onReport();

            return stringBuilder.substring(0, stringBuilder.length() - 2);
        }
    }

    // Thank you https://stackoverflow.com/questions/1201048/allowing-java-to-use-an-untrusted-certificate-for-ssl-https-connection/1201102#1201102 !
    private static final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
    };

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "method", "uri", "headers", "followRedirects" })
    public static class Parameters {
        public String method;
        public TemplateParameters uri;
        public Map<String, TemplateParameters> headers = Collections.emptyMap();
        public HttpClient.Redirect followRedirects = HttpClient.Redirect.NORMAL;
        public boolean allowUntrusted;
        public boolean stashPeerCertificates;
        public List<Integer> retry = List.of(429, 503);
        public List<String> backoff = List.of("5s", "15s", "30s", "1m");
    }
}
