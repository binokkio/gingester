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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

@Passthrough
public final class Oidc implements Transformer<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Oidc.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    private final FetchKey fetchHttpResponse = new FetchKey("http.response");
    private final FetchKey fetchHttpRequestMethod = new FetchKey("http.request.method");
    private final FetchKey fetchHttpRequestPath = new FetchKey("http.request.path");
    private final FetchKey fetchHttpRequestQueryString = new FetchKey("http.request.queryString");
    private final FetchKey fetchHttpRequestQueryLogin = new FetchKey("http.request.query.gingester-oidc-login");
    private final FetchKey fetchHttpRequestQueryCode = new FetchKey("http.request.query.code");
    private final FetchKey fetchHttpRequestCookiesCookieName;

    private final String clientId;
    private final List<String> scopes;
    private final String authUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String redirectUrl;
    private final String redirectPath;
    private final String cookieName;
    private final String tokenRequestStart;
    private final boolean optional;
    private final boolean accessTokenIsJwt;

    public Oidc(Parameters parameters) {

        clientId = requireNonNull(parameters.clientId, "Missing clientId parameter");
        scopes = requireNonNull(parameters.scopes, "Missing scopes parameter");
        authUrl = requireNonNull(parameters.authUrl, "Missing authUrl parameter");
        tokenUrl = requireNonNull(parameters.tokenUrl, "Missing tokenUrl parameter");
        userInfoUrl = parameters.userInfoUrl;
        redirectUrl = requireNonNull(parameters.redirectUrl, "Missing redirectUrl parameter");
        redirectPath = getUrlPath(redirectUrl);
        cookieName = requireNonNull(parameters.cookieName, "Missing cookieName parameter");
        optional = parameters.optional;
        accessTokenIsJwt = parameters.accessTokenIsJwt;

        fetchHttpRequestCookiesCookieName = new FetchKey("http.request.cookies." + cookieName);

        tokenRequestStart =
                "client_id=" + parameters.clientId +
                "&client_secret=" + requireNonNull(parameters.clientSecret, "Missing clientSecret parameter");
    }

    private String getUrlPath(String url) {
        try {
            return new URL(url).getPath();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        HttpResponse response = (HttpResponse) context.fetch(fetchHttpResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        Cookie cookie = (Cookie) context.fetch(fetchHttpRequestCookiesCookieName)
                .orElseGet(() -> new Cookie(cookieName, UUID.randomUUID().toString()));

        cookie.setPath("/");
        cookie.setMaxAge(2592000);
        response.addCookie(cookie);

        UUID sessionId = UUID.fromString(cookie.getValue());
        Session session = sessions.computeIfAbsent(sessionId, uuid -> new Session());

        Optional<Object> optionalCode = context.fetch(fetchHttpRequestQueryCode);
        if (context.require(fetchHttpRequestPath).equals(redirectPath) && optionalCode.isPresent()) {

            session.handleTokenResponse(requestToken(String.format(
                    "%s&grant_type=authorization_code&code=%s&redirect_uri=%s",
                    tokenRequestStart,
                    optionalCode.get(),
                    redirectUrl
            )));

            response.setStatus(302);
            response.addHeader("Location", session.returnTo);
            response.finish();

        } else {

            Instant now = Instant.now();

            if (session.needsRefresh(now)) {
                session.handleTokenResponse(requestToken(String.format(
                        "%s&grant_type=refresh_token&refresh_token=%s",
                        tokenRequestStart,
                        session.refreshToken
                )));
            }

            if (session.hasAccess(now)) {
                out.accept(context.stash(session.stash), in);
            } else if (optional && context.fetch(fetchHttpRequestQueryLogin).isEmpty()) {
                out.accept(context, in);
            } else if (context.require(fetchHttpRequestMethod).equals("GET")) {

                session.returnTo = (String) context.require(fetchHttpRequestPath);
                context.fetch(fetchHttpRequestQueryString).ifPresent(
                        queryString -> session.returnTo += "?" + queryString);

                String location = authUrl +
                        "?client_id=" + clientId +
                        "&scope=" + String.join("%20", scopes) +
                        "&state=" + Long.toHexString(sessionId.getLeastSignificantBits()) +  // TODO improve value and verify on callback
                        "&response_type=code" +
                        "&redirect_uri=" + redirectUrl;

                response.setStatus(302);
                response.addHeader("Location", location);
                response.finish();

            } else {
                throw new IllegalStateException("No access token available");
            }
        }
    }

    private String requestToken(String requestBody) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(tokenUrl));
        requestBuilder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
        return HTTP_CLIENT.send(requestBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString()).body();
    }

    private class Session {

        private String returnTo;
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

            if (!root.has("access_token"))
                throw new IllegalStateException("Token response did not contain access_token: " + root);

            accessToken = root.get("access_token").asText();

            stash = new HashMap<>();
            stash.put("accessToken", accessToken);

            JsonNode accessExpiresAtNode = root.path("expires_in");
            if (accessExpiresAtNode.isValueNode())
                accessExpiresAt = Instant.now().plus(Duration.ofSeconds(accessExpiresAtNode.asInt())).minus(Duration.ofSeconds(30));

            JsonNode refreshTokenNode = root.path("refresh_token");
            if (refreshTokenNode.isValueNode())
                refreshToken = refreshTokenNode.asText();

            JsonNode refreshExpiresAtNode = root.path("refresh_expires_in");
            if (refreshExpiresAtNode.isValueNode())
                refreshExpiresAt = Instant.now().plus(Duration.ofSeconds(refreshExpiresAtNode.asInt())).minus(Duration.ofSeconds(30));

            if (accessTokenIsJwt) {
                DecodedJWT decoded = JWT.decode(accessToken);
                HashMap<String, JsonNode> claims = new HashMap<>();
                decoded.getClaims().forEach((key, value) -> claims.put(key, value.as(JsonNode.class)));
                stash.put("claims", claims);
            }

            if (userInfoUrl != null) {
                try {
                    stash.put("userInfo", OBJECT_MAPPER.readTree(requestUserInfo()));
                } catch (IOException | InterruptedException e) {
                    LOGGER.warn("Exception while getting user info", e);
                }
            }
        }

        private String requestUserInfo() throws IOException, InterruptedException {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(userInfoUrl));
            requestBuilder.header("Authorization", "Bearer " + accessToken);
            requestBuilder.GET();
            return HTTP_CLIENT.send(requestBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString()).body();
        }
    }

    public static class Parameters {
        public String clientId;
        public String clientSecret;
        public List<String> scopes = List.of("openid");
        public String configUrl;
        public String authUrl;
        public String tokenUrl;
        public String userInfoUrl;
        public String redirectUrl;
        public String cookieName = "gingester-oidc";
        public boolean optional;
        public boolean accessTokenIsJwt;
    }
}
