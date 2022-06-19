package b.nana.technology.gingester.transformers.base.transformers.http;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
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
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public final class Get implements Transformer<Object, InputStream> {

    private final Template uriTemplate;
    private final HttpClient.Redirect followRedirects;
    private final Map<String, Template> headers;

    public Get(Parameters parameters) {
        uriTemplate = Context.newTemplate(parameters.uri);
        followRedirects = parameters.followRedirects;
        headers = parameters.headers.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Context.newTemplate(e.getValue())
                ));
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingSync();
    }

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(followRedirects).build();
        URI uri = URI.create(uriTemplate.render(context));
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
        headers.forEach((name, template) -> requestBuilder.header(name, template.render(context)));
        HttpResponse<InputStream> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        Context.Builder contextBuilder = context.stash(Map.of(
                "description", uri.toString(),
                "headers", response.headers().map()
        ));
        out.accept(contextBuilder, response.body());
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, uri -> o("uri", uri));
            }
        }

        public TemplateParameters uri;
        public HttpClient.Redirect followRedirects = HttpClient.Redirect.NORMAL;
        public Map<String, TemplateParameters> headers = Collections.emptyMap();
    }
}
