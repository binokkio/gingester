package b.nana.technology.gingester.transformers.base.transformers.http;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;

public final class Get implements Transformer<Object, InputStream> {

    private final Context.Template uriTemplate;
    private final HttpClient.Redirect followRedirects;
    private final Map<String, String> headers;

    public Get(Parameters parameters) {
        uriTemplate = Context.newTemplate(parameters.uri);
        followRedirects = parameters.followRedirects;
        headers = parameters.headers;
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
        headers.forEach(requestBuilder::header);
        HttpResponse<InputStream> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        Context.Builder contextBuilder = context.stash(Map.of(
                "description", uri.toString(),
                "headers", response.headers().map()
        ));
        out.accept(contextBuilder, response.body());
    }

    public static class Parameters {

        public String uri;
        public HttpClient.Redirect followRedirects = HttpClient.Redirect.NORMAL;
        public Map<String, String> headers = Collections.emptyMap();

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String uri) {
            this.uri = uri;
        }
    }
}
