package b.nana.technology.gingester.transformers.jetty.transformers.http;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.jetty.http.HttpRequest;
import b.nana.technology.gingester.transformers.jetty.http.HttpResponseDummy;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTest {

    @Test
    void testHttpServerFlowCanBeTested() {

        HttpResponseDummy response = new HttpResponseDummy();

        new FlowBuilder().cli("" +
                "-sft HttpServer " +
                "-t InputStreamToString " +
                "-t Repeat 3 " +
                "-t StringDef '${http.request.object.remoteAddress}: ${__in__}' " +
                "-stt InputStreamJoin " +
                "-t HttpRespond")
                .replace("HttpServer", (Transformer<Object, InputStream>) (context, in, out) -> {

                    Map<String, Object> stash = Map.of(
                            "http", Map.of(
                                    "request", Map.of(
                                            "object", (HttpRequest) () -> "127.0.0.1"
                                    ),
                                    "response", response
                            )
                    );

                    ByteArrayInputStream requestBody = new ByteArrayInputStream("Hello, World!".getBytes());

                    out.accept(context.stash(stash), requestBody);
                })
                .run();

        assertEquals(
                "127.0.0.1: Hello, World!\n127.0.0.1: Hello, World!\n127.0.0.1: Hello, World!",
                response.getOutputStream().toString()
        );
    }
}
