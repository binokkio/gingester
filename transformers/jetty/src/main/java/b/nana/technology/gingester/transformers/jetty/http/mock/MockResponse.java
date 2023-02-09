package b.nana.technology.gingester.transformers.jetty.http.mock;

import b.nana.technology.gingester.transformers.jetty.http.HttpResponse;
import jakarta.servlet.http.Cookie;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MockResponse implements HttpResponse {

    private int status;
    private final Map<String, String> headers = new HashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final AtomicBoolean finished = new AtomicBoolean();

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public boolean hasHeader(String name) {
        return headers.containsKey(name);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    @Override
    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void finish() {

        if (finished.getAndSet(true))
            throw new IllegalStateException("Already finished");

        synchronized (finished) {
            finished.notifyAll();
        }
    }

    public boolean hasFinished() {
        return finished.get();
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
