package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Passthrough
public final class BasicAuth implements Transformer<Object, Object> {

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");
    private final FetchKey fetchHttpAuthorizationHeader = new FetchKey("http.request.headers.Authorization");
    // NOTE that the above is a bit iffy, a valid authorization header is taken from any upstream transformer

    private final String wwwAuthenticateHeaderValue;
    private final Map<String, String> credentials;

    public BasicAuth(Parameters parameters) {
        wwwAuthenticateHeaderValue = "Basic realm=\"" + parameters.realm + "\"";
        credentials = parameters.credentials;
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.of("username", String.class);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        Optional<Object> optionalAuthorization = context.fetch(fetchHttpAuthorizationHeader);
        if (optionalAuthorization.isPresent()) {
            String authorization = (String) optionalAuthorization.get();

            try {
                // Thank you https://stackoverflow.com/questions/16000517/how-to-get-password-from-http-basic-authentication !
                String base64 = authorization.substring("Basic".length()).trim();
                byte[] decoded = Base64.getDecoder().decode(base64);
                String[] parts = new String(decoded, StandardCharsets.UTF_8).split(":", 2);
                String username = parts[0];
                String password = parts[1];
                if (password.equals(credentials.get(username))) {
                    out.accept(context.stash("username", username), in);
                    return;
                }
            } catch (Exception e) {
                // ignore and continue to the 401 response
            }
        }

        // respond with a 401 and don't output anything
        HttpResponse response = (HttpResponse) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        response.setStatus(401);
        response.addHeader("WWW-Authenticate", wwwAuthenticateHeaderValue);
        response.finish();
    }

    public static class Parameters {
        public String realm = "site";
        public Map<String, String> credentials;
    }
}
