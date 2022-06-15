package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

// TODO this is a very quick and dirty implementation only tested to work with a specific use case

@Passthrough
public final class Keycloak implements Transformer<Object, Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");
    private final FetchKey fetchHttpRequestMethod = new FetchKey("http.request.method");
    private final FetchKey fetchHttpRequestPath = new FetchKey("http.request.path");
    private final FetchKey fetchHttpRequestQueryCode = new FetchKey("http.request.query.code");
    private final FetchKey fetchHttpRequestCookiesCookieName;

    private final String authUrl;
    private final String tokenUrl;
    private final String redirectUrl;
    private final String clientId;
    private final String cookieName;

    public Keycloak(Parameters parameters) {

        requireNonNull(parameters.authUrl, "Missing authUrl parameter");
        requireNonNull(parameters.realm, "Missing realm parameter");
        requireNonNull(parameters.clientId, "Missing clientId parameter");
        requireNonNull(parameters.redirectUrl, "Missing redirectUrl parameter");
        requireNonNull(parameters.cookieName, "Missing cookieName parameter");

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
        Session session = sessions.computeIfAbsent(sessionId, uuid -> new Session());

        Optional<Object> optionalCode = context.fetch(fetchHttpRequestQueryCode);
        if (optionalCode.isPresent()) {

            session.handleTokenResponse(requestToken(String.format(
                    "client_id=%s&grant_type=authorization_code&code=%s&redirect_uri=%s",
                    clientId,
                    optionalCode.get(),
                    getRedirectUrl(context)
            )));

            response.setStatus(302);
            response.addHeader("Location", getRedirectUrl(context));
            response.respondEmpty();

        } else {

            Instant now = Instant.now();

            if (session.needsRefresh(now)) {

                session.handleTokenResponse(requestToken(String.format(
                        "client_id=%s&grant_type=refresh_token&refresh_token=%s",
                        clientId,
                        session.refreshToken
                )));
            }

            if (session.hasAccess(now)) {
                out.accept(context.stash(session.stash), in);
            } else if (context.require(fetchHttpRequestMethod).equals("GET")) {
                response.setStatus(302);
                response.addHeader("Location", authUrl + "&redirect_uri=" + getRedirectUrl(context));
                response.respondEmpty();
            } else {
                throw new IllegalStateException("No access token available");
            }
        }
    }

    private String requestToken(String requestBody) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(tokenUrl));
        requestBuilder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
        return HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString()).body();
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

    private static class Session {

        private String accessToken;
        private Instant accessExpiresAt;
        private String refreshToken;
        private Instant refreshExpiresAt;
        private Map<String, Object> stash;

        private boolean hasAccess(Instant now) {
            return accessToken != null &&
                    (accessExpiresAt == null || accessExpiresAt.isAfter(now));
        }

        private boolean needsRefresh(Instant now) {
            return accessExpiresAt != null &&
                    accessExpiresAt.isBefore(now) &&
                    refreshToken != null &&
                    (refreshExpiresAt == null || refreshExpiresAt.isAfter(now));
        }

        private void handleTokenResponse(String tokenResponse) throws JsonProcessingException {

            JsonNode root = OBJECT_MAPPER.readTree(tokenResponse);

            accessToken = root.get("access_token").asText();

            JsonNode accessExpiresAtNode = root.path("expires_in");
            if (accessExpiresAtNode.isValueNode())
                accessExpiresAt = Instant.now().plus(Duration.ofSeconds(accessExpiresAtNode.asInt())).minus(Duration.ofSeconds(30));

            JsonNode refreshTokenNode = root.path("refresh_token");
            if (refreshTokenNode.isValueNode())
                refreshToken = refreshTokenNode.asText();

            JsonNode refreshExpiresAtNode = root.path("refresh_expires_in");
            if (refreshExpiresAtNode.isValueNode())
                refreshExpiresAt = Instant.now().plus(Duration.ofSeconds(refreshExpiresAtNode.asInt())).minus(Duration.ofSeconds(30));

            DecodedJWT decoded = JWT.decode(accessToken);
            HashMap<String, JsonNode> claims = new HashMap<>();
            decoded.getClaims().forEach((key, value) -> claims.put(key, value.as(JsonNode.class)));

            stash = Map.of(
                    "accessToken", accessToken,
                    "claims", claims
            );
        }
    }
}
