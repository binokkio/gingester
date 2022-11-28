package b.nana.technology.gingester.transformers.jetty.http;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

final class HttpResponseWrapper implements HttpResponse {

    private final HttpServletResponse httpServletResponse;
    private final AtomicBoolean finished = new AtomicBoolean();

    HttpResponseWrapper(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public void setStatus(int status) {
        httpServletResponse.setStatus(status);
    }

    @Override
    public void addHeader(String name, String value) {
        httpServletResponse.addHeader(name, value);
    }

    @Override
    public boolean hasHeader(String name) {
        return httpServletResponse.containsHeader(name);
    }

    @Override
    public void addCookie(Cookie cookie) {
        httpServletResponse.addCookie(cookie);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return httpServletResponse.getOutputStream();
    }

    @Override
    public void finish() {

        if (finished.getAndSet(true))
            throw new IllegalStateException("Already finished");

        synchronized (finished) {
            finished.notifyAll();
        }
    }

    @Override
    public void await() {
        if (!finished.get()) {
            synchronized (finished) {
                while (!finished.get()) {
                    try {
                        finished.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();  // TODO
                    }
                }
            }
        }
    }
}
