package b.nana.technology.gingester.transformers.jetty.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

public class RequestWrapper {

    public final String target;
    public final Request request;
    public final HttpServletRequest servletRequest;
    public final HttpServletResponse servletResponse;

    public RequestWrapper(String target, Request request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.target = target;
        this.request = request;
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }
}