package b.nana.technology.gingester.transformers.base.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;

public class Get extends Transformer<Object, byte[]> {

    private final URI uri;
    private final HttpClient.Redirect followRedirects;
    private final Map<String, String> headers;

    public Get(Parameters parameters) {
        super(parameters);
        uri = URI.create(parameters.uri);
        followRedirects = HttpClient.Redirect.valueOf(parameters.followRedirects);
        headers = parameters.headers;
    }

    @Override
    protected void setup(Setup setup) {
        setup.requireDownstreamSync();
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(followRedirects).build();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
        headers.forEach(requestBuilder::header);
        HttpResponse<byte[]> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        Context.Builder contextBuilder = context.extend(this).description(uri.toString());
        emit(contextBuilder, response.body());
    }

    public static class Parameters {

        public String uri;
        public String followRedirects = HttpClient.Redirect.NORMAL.name();
        public Map<String, String> headers = Collections.emptyMap();

        @JsonCreator
        public Parameters() {

        }

        @JsonCreator
        public Parameters(String uri) {
            this.uri = uri;
        }
    }
}
