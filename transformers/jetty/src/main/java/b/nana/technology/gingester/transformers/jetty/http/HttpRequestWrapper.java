package b.nana.technology.gingester.transformers.jetty.http;

import jakarta.servlet.http.HttpServletRequest;

final class HttpRequestWrapper implements HttpRequest {

    private final HttpServletRequest httpServletRequest;

    HttpRequestWrapper(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public String getRemoteAddress() {
        return httpServletRequest.getRemoteAddr();
    }
}
