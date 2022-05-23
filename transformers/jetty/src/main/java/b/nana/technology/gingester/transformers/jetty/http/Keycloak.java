package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO this is a very quick and dirty implementation only tested to work with a specific use case

@Passthrough
public final class Keycloak implements Transformer<Object, Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final Map<UUID, Map<String, JsonNode>> sessions = new ConcurrentHashMap<>();

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");
    private final FetchKey fetchHttpRequestPath = new FetchKey("http.request.path");
    private final FetchKey fetchHttpQueryCode = new FetchKey("http.query.code");
    private final FetchKey fetchHttpRequestCookiesCookieName;

    private final String authUrl;
    private final String tokenUrl;
    private final String redirectUrl;
    private final String clientId;
    private final String cookieName;

    public Keycloak(Parameters parameters) {
        String baseUrl = stripTrailingSlash(parameters.authUrl) + "/realms/" + parameters.realm + "/protocol/openid-connect";
        authUrl = baseUrl + "/auth?client_id=" + parameters.clientId + "&response_type=code";
        tokenUrl = baseUrl + "/token";
        clientId = parameters.clientId;
        redirectUrl = stripTrailingSlash(parameters.redirectUrl);
        cookieName = parameters.cookieName;

        fetchHttpRequestCookiesCookieName = new FetchKey("http.request.cookies." + cookieName);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Server.ResponseWrapper response = (Server.ResponseWrapper) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        Cookie cookie = (Cookie) context.fetch(fetchHttpRequestCookiesCookieName)
                .orElseGet(() -> new Cookie(cookieName, UUID.randomUUID().toString()));

        cookie.setMaxAge(2592000);
        response.addCookie(cookie);

        UUID sessionId = UUID.fromString(cookie.getValue());

        Optional<Object> optionalCode = context.fetch(fetchHttpQueryCode);
        if (optionalCode.isPresent()) {

            String tokenRequestBody = String.format(
                    "client_id=%s&grant_type=authorization_code&code=%s&redirect_uri=%s",
                    clientId,
                    optionalCode.get(),
                    getRedirectUrl(context)
            );

            HttpRequest.Builder tokenRequestBuilder = HttpRequest.newBuilder(URI.create(tokenUrl));
            tokenRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody));
            tokenRequestBuilder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            HttpResponse<String> tokenResponse = HTTP_CLIENT.send(tokenRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            String accessTokenString = (String) OBJECT_MAPPER.readValue(tokenResponse.body(), Map.class).get("access_token");
            DecodedJWT accessToken = JWT.decode(accessTokenString);
            Map<String, JsonNode> result = new HashMap<>();
            accessToken.getClaims().forEach((key, value) -> result.put(key, value.as(JsonNode.class)));
            sessions.put(sessionId, result);

            response.setStatus(302);
            response.addHeader("Location", getRedirectUrl(context));
            response.respondEmpty();

        } else if (!sessions.containsKey(sessionId)) {
            response.setStatus(302);
            response.addHeader("Location", authUrl + "&redirect_uri=" + getRedirectUrl(context));
            response.respondEmpty();
        } else {
            out.accept(context.stash("keycloak", sessions.get(sessionId)), in);
        }
    }

    private String getRedirectUrl(Context context) {
        return redirectUrl + context.require(fetchHttpRequestPath);
    }

    private static String stripTrailingSlash(String input) {
        if (input.charAt(input.length() - 1) == '/') {
            return input.substring(0, input.length() - 1);
        } else {
            return input;
        }
    }

    public static class Parameters {
        public String authUrl;
        public String redirectUrl;
        public String realm;
        public String clientId;
        public String cookieName = "gingester-keycloak";
    }
}
