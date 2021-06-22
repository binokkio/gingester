package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.Gunzip;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.Split;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToJson;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestToDsvInputStream {

    @Test
    void test() {

        Gunzip gunzip = new Gunzip();
        Split inputStreamSplit = new Split(new Split.Parameters("\n"));
        ToJson inputStreamToJson = new ToJson(new ToJsonBase.Parameters());
        ToDsvInputStream.Parameters toDsvInputStreamParameters = new ToDsvInputStream.Parameters();
        toDsvInputStreamParameters.delimiter = '1';
        toDsvInputStreamParameters.quote = '2';
        ToDsvInputStream toDsvInputStream = new ToDsvInputStream(toDsvInputStreamParameters);
        ToString inputStreamToString = new ToString();

        AtomicReference<String> result = new AtomicReference<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(gunzip, getClass().getResourceAsStream("/hello-world.ndjson.gz"));
        gBuilder.sync(gunzip, toDsvInputStream);
        gBuilder.link(gunzip, inputStreamSplit);
        gBuilder.link(inputStreamSplit, inputStreamToJson);
        gBuilder.link(inputStreamToJson, toDsvInputStream);
        gBuilder.link(toDsvInputStream, inputStreamToString);
        gBuilder.link(inputStreamToString, result::set);
        gBuilder.build().run();

        assertEquals(
                "message\n" +
                "2Hello, World 1!2\n" +
                "2Hello, World 22!2\n" +
                "Hello, World 3!\n",
                result.get()
        );
    }
}
