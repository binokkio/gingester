package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.transformers.jetty.common.RequestWrapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.HashMap;
import java.util.Map;

public class Server extends Transformer<Void, RequestWrapper> {

    private final int port;
    private final boolean details;
    private final boolean headerDetails;
    private final boolean queryDetails;

    public Server(Parameters parameters) {
        super(parameters);
        port = parameters.port;
        headerDetails = parameters.headerDetails;
        queryDetails = parameters.queryDetails;
        details = headerDetails | queryDetails;
    }

    @Override
    protected void setup(Setup setup) {
        setup.syncOutputs();
        setup.limitMaxWorkers(1);
    }

    @Override
    protected void transform(Context context, Void input) throws Exception {

        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(new ServerQueuedThreadPool());

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        server.setHandler(new AbstractHandler() {

            @Override
            public void handle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) {

                jettyRequest.setHandled(true);
                jettyRequest.setContentType("application/octet-stream");

                Context.Builder contextBuilder = context.extend(Server.this)
                        .description(jettyRequest.getMethod() + " " + target);

                if (details) {

                    Map<String, Object> details = new HashMap<>();

                    if (headerDetails) {
                        Map<String, String> headers = new HashMap<>();
                        jettyRequest.getHeaderNames().asIterator().forEachRemaining(
                                headerName -> headers.put(headerName, jettyRequest.getHeader(headerName)));
                        details.put("headers", headers);
                    }

                    if (queryDetails) {
                        Map<String, String> query = new HashMap<>();
                        jettyRequest.getParameterMap();  // triggers query parameter initialization, TODO check proper solution
                        jettyRequest.getQueryParameters().forEach(
                                (queryParameterName, value) -> query.put(queryParameterName, value.get(value.size() - 1)));
                        details.put("query", query);
                    }

                    contextBuilder.details(details);
                }

                emit(
                        contextBuilder,
                        new RequestWrapper(target, jettyRequest, request, response)
                );
            }
        });

        server.start();
        server.join();
    }

    private class ServerQueuedThreadPool extends QueuedThreadPool {

        @Override
        public Thread newThread(Runnable runnable) {
            return Server.this.newThread(runnable);
        }
    }

    public static class Parameters {

        public int port = 8080;
        public boolean headerDetails = true;
        public boolean queryDetails = true;

        @JsonCreator
        public Parameters() {

        }

        @JsonCreator
        public Parameters(int port) {
            this.port = port;
        }
    }
}
