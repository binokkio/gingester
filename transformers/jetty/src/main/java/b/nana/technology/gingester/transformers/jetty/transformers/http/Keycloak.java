package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Passthrough;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Keycloak<T> extends Passthrough<T> {

    private final Map<String, Object> sessions = new ConcurrentHashMap<>();

    @Override
    protected void transform(Context context, T input) {
        HttpServletResponse response = (HttpServletResponse) context.fetch("response").orElseThrow();
        Cookie cookie = new Cookie("session", UUID.randomUUID().toString());
        response.addCookie(cookie);
        emit(context, input);
    }
}
