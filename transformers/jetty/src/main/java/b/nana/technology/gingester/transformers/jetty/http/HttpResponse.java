package b.nana.technology.gingester.transformers.jetty.http;

import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.io.OutputStream;

public interface HttpResponse {
    void setStatus(int status);
    void addHeader(String name, String value);
    boolean hasHeader(String name);
    void addCookie(Cookie cookie);
    OutputStream getOutputStream() throws IOException;
    void finish();
    void await();
}
