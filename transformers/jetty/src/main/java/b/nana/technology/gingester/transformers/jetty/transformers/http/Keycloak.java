package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Passthrough;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
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

public class Keycloak<T> extends Passthrough<T> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<UUID, Map<String, String>> sessions = new ConcurrentHashMap<>();

    private final String authUrl;
    private final String tokenUrl;
    private final String redirectUrl;
    private final String clientId;
    private final String cookieName;

    public Keycloak(Parameters parameters) {
        clientId = parameters.clientId;
        redirectUrl = parameters.redirectUrl.endsWith("/") ?
                parameters.redirectUrl.substring(0, parameters.redirectUrl.length() - 1) :
                parameters.redirectUrl;
        cookieName = parameters.cookieName;

        String baseUrl = parameters.authUrl;
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        baseUrl += "/realms/";
        baseUrl += parameters.realm;
        baseUrl += "/protocol/openid-connect";

        String authUrl = baseUrl;
        authUrl += "/auth?client_id=";
        authUrl += clientId;
        authUrl += "&response_type=code";
        this.authUrl = authUrl;

        String tokenUrl = baseUrl;
        tokenUrl += "/token";
        this.tokenUrl = tokenUrl;
    }

    @Override
    protected void transform(Context context, T input) throws IOException, InterruptedException {

        HttpServletResponse response = (HttpServletResponse) context.fetch("response").orElseThrow();

        Optional<Object> optionalCookie = context.fetch("cookies", cookieName);
        Cookie cookie;
        if (optionalCookie.isPresent()) {
            cookie = (Cookie) optionalCookie.get();
        } else {
            cookie = new Cookie(cookieName, UUID.randomUUID().toString());
            response.addCookie(cookie);
        }

        UUID sessionId = UUID.fromString(cookie.getValue());

        Optional<Object> optionalCode = context.fetch("query", "code");
        if (optionalCode.isPresent()) {

            String body = "client_id=";
            body += clientId;
            body += "&grant_type=authorization_code&code=";
            body += optionalCode.get();
            body += "&redirect_uri=";
            body += getRedirectUrl(context);

            HttpRequest.Builder tokenRequestBuilder = HttpRequest.newBuilder(URI.create(tokenUrl));
            tokenRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            tokenRequestBuilder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> tokenResponse = httpClient.send(tokenRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            String accessToken = (String) OBJECT_MAPPER.readValue(tokenResponse.body(), Map.class).get("access_token");

            Map<String, String> result = new HashMap<>();
            DecodedJWT s = JWT.decode(accessToken);
            s.getClaims().forEach((sa, c) -> {
                try {
                    result.put(sa, c.asString());
                } catch (Exception e) {
                    // ignore
                }
            });

            sessions.put(sessionId, result);
        }

        if (!sessions.containsKey(sessionId)) {
            response.setStatus(302);
            response.addHeader("Location", authUrl + "&redirect_uri=" + getRedirectUrl(context));
        } else {
            emit(context.extend(this).stash(Map.of("keycloak", sessions.get(sessionId))), input);
        }
    }

    private String getRedirectUrl(Context context) {
        return redirectUrl + context.fetch("request", "path").orElseThrow();
    }

    public static class Parameters {
        public String authUrl;
        public String redirectUrl;
        public String realm;
        public String clientId;
        public String cookieName = "gingester-keycloak";
    }
}
