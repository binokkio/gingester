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
import java.util.regex.Pattern;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

@Passthrough
public final class Oidc implements Transformer<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(Oidc.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<UUID, Session> staticSessions = new ConcurrentHashMap<>();

    private final Map<UUID, Session> sessions;

    private final FetchKey fetchResponse = new FetchKey("http.response");
    private final FetchKey fetchRequestMethod = new FetchKey("http.request.method");
    private final FetchKey fetchRequestPath = new FetchKey("http.request.path");
    private final FetchKey fetchRequestQueryString = new FetchKey("http.request.queryString");
    private final FetchKey fetchRequestQueryCode = new FetchKey("http.request.query.code");
    private final FetchKey fetchRequestQueryLogin;
    private final FetchKey fetchRequestCookie;

    private final String clientId;
    private final String scopes;
    private final String authUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String redirectUrl;
    private final String redirectPath;
    private final String cookieName;
    private final String tokenRequestStart;
    private final Pattern loginParamPattern;
    private final boolean optional;
    private final boolean accessTokenIsJwt;

    public Oidc(Parameters parameters) {

        sessions = parameters.staticSessions ? staticSessions : new ConcurrentHashMap<>();

        if (parameters.configUrl != null) {

            JsonNode response;
            try {
                response = objectMapper.readTree(httpClient.send(HttpRequest.newBuilder()
                        .GET().uri(URI.create(parameters.configUrl))
                        .header("Accept", "application/json")
                        .build(), ofString()).body());
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException("Failed to load config from configUrl", e);
            }

            authUrl = response.get("authorization_endpoint").textValue();
            tokenUrl = response.get("token_endpoint").textValue();
            userInfoUrl = response.path("userinfo_endpoint").textValue();
        } else {
            authUrl = requireNonNull(parameters.authUrl, "Missing authUrl parameter");
            tokenUrl = requireNonNull(parameters.tokenUrl, "Missing tokenUrl parameter");
            userInfoUrl = parameters.userInfoUrl;
        }

        String loginParam = requireNonNull(parameters.loginParam, "Missing loginParam parameter");

        clientId = requireNonNull(parameters.clientId, "Missing clientId parameter");
        scopes = String.join("%20", requireNonNull(parameters.scopes, "Missing scopes parameter"));
        redirectUrl = requireNonNull(parameters.redirectUrl, "Missing redirectUrl parameter");
        redirectPath = getUrlPath(redirectUrl);
        cookieName = requireNonNull(parameters.cookieName, "Missing cookieName parameter");
        loginParamPattern = Pattern.compile("&?" + loginParam + "(?:=[^&]*)?&?");
        optional = parameters.optional;
        accessTokenIsJwt = parameters.accessTokenIsJwt;

        fetchRequestQueryLogin = new FetchKey("http.request.query." + loginParam);
        fetchRequestCookie = new FetchKey("http.request.cookies." + cookieName);

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

        HttpResponse response = (HttpResponse) context.fetch(fetchResponse)
                .orElseThrow(() -> new IllegalStateException("Context did not come from HttpServer"));

        Cookie cookie = (Cookie) context.fetch(fetchRequestCookie)
                .orElseGet(() -> new Cookie(cookieName, UUID.randomUUID().toString()));

        cookie.setPath("/");
        cookie.setMaxAge(2592000);
        response.addCookie(cookie);

        UUID sessionId = UUID.fromString(cookie.getValue());
        Session session = sessions.computeIfAbsent(sessionId, uuid -> new Session());

        Optional<Object> optionalCode = context.fetch(fetchRequestQueryCode);
        if (context.require(fetchRequestPath).equals(redirectPath) && optionalCode.isPresent()) {

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

            boolean loginRequestParamIsPresent = context.fetch(fetchRequestQueryLogin).isPresent();
            if (session.hasAccess(now) && !loginRequestParamIsPresent) {
                out.accept(context.stash(session.stash), in);
            } else if (optional && !loginRequestParamIsPresent) {
                out.accept(context, in);
            } else if (context.require(fetchRequestMethod).equals("GET")) {

                session.returnTo = (String) context.require(fetchRequestPath);
                context.fetch(fetchRequestQueryString)
                        .map(String.class::cast)
                        .map(queryString -> loginParamPattern.matcher(queryString).replaceAll(m -> m.group().startsWith("&") && m.group().endsWith("&") ? "&" : ""))
                        .filter(not(String::isEmpty))
                        .ifPresent(queryString -> session.returnTo += "?" + queryString);

                String location = authUrl +
                        "?client_id=" + clientId +
                        "&scope=" + scopes +
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
        return httpClient.send(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)).uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .build(), ofString()).body();
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

            JsonNode root = objectMapper.readTree(tokenResponse);

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
                    stash.put("userInfo", objectMapper.readTree(requestUserInfo()));
                } catch (IOException | InterruptedException e) {
                    logger.warn("Exception while getting user info", e);
                }
            }
        }

        private String requestUserInfo() throws IOException, InterruptedException {
            return httpClient.send(HttpRequest.newBuilder()
                    .GET().uri(URI.create(userInfoUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .build(), ofString()).body();
        }
    }

    public static class Parameters {
        public String clientId;
        public String clientSecret;
        public String configUrl;
        public String authUrl;
        public String tokenUrl;
        public String userInfoUrl;
        public String redirectUrl;
        public String loginParam = "login";
        public String cookieName = "gingester-oidc";
        public List<String> scopes = List.of("openid");
        public boolean optional;
        public boolean staticSessions;
        public boolean accessTokenIsJwt;
    }
}
