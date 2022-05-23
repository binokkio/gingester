package b.nana.technology.gingester.transformers.jetty.http;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.PrefixInputStream;
import b.nana.technology.gingester.transformers.base.common.iostream.Splitter;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Split implements Transformer<InputStream, InputStream> {

    private static final Pattern BOUNDARY_PATTERN = Pattern.compile("(?:^|;| )boundary=([^; ]*)");
    private static final byte[] CR_LF = new byte[] { '\r', '\n' };
    private static final byte[] CR_LF_CR_LF = new byte[] { '\r', '\n' , '\r', '\n' };
    private static final byte[] FINAL_BOUNDARY_SENTINEL = new byte[] { '-', '-' };
    private static final Pattern NAME_PATTERN = Pattern.compile("[; ]name=\"(.*?)\"");
    private static final Pattern FILENAME_PATTERN = Pattern.compile("[; ]filename=\"(.*?)\"");

    private final FetchKey fetchHttpRequestHeadersContentType = new FetchKey("http.request.headers.Content-Type");

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        String contentType = (String) context.fetch(fetchHttpRequestHeadersContentType).orElseThrow(
                () -> new IllegalStateException("Can't split stream without Content-Type header"));

        Matcher boundaryMatcher = BOUNDARY_PATTERN.matcher(contentType);

        if (!boundaryMatcher.find()) {
            throw new IllegalStateException("Boundary not found in Content-Type header");
        }

        byte[] delimiter = ("\r\n--" + boundaryMatcher.group(1)).getBytes();

        PrefixInputStream prefixedInput = new PrefixInputStream(in);
        prefixedInput.prefix(CR_LF);
        Splitter partsSplitter = new Splitter(prefixedInput, delimiter);

        // skip preamble
        //noinspection ResultOfMethodCallIgnored
        partsSplitter.getNextInputStream().orElseThrow().skip(Long.MAX_VALUE);

        long counter = 0;
        byte[] peek = new byte[2];
        Optional<InputStream> optionalPart;
        while ((optionalPart = partsSplitter.getNextInputStream()).isPresent()) {
            InputStream part = optionalPart.get();

            // must be either "--" or "\r\n" according to the RFC, and knowing the
            // InputStream implementation it is not possible for read to be less than 2
            //noinspection ResultOfMethodCallIgnored
            part.read(peek);

            if (Arrays.equals(peek, FINAL_BOUNDARY_SENTINEL)) {
                // skip epilogue
                //noinspection ResultOfMethodCallIgnored
                part.skip(Long.MAX_VALUE);
                break;
            } else {
                Splitter partSplitter = new Splitter(part, CR_LF_CR_LF);
                String headersString = new String(partSplitter.getNextInputStream().orElseThrow().readAllBytes());
                Matcher nameMatcher = NAME_PATTERN.matcher(headersString);
                if (!nameMatcher.find()) throw new IllegalStateException("Can't find name");
                String name = nameMatcher.group(1);
                Matcher filenameMatcher = FILENAME_PATTERN.matcher(headersString);
                Context.Builder contextBuilder = context.stash("description", ++counter);
                if (filenameMatcher.find()) {
                    contextBuilder.stash(Map.of(
                            "name", name,
                            "filename", filenameMatcher.group(1)
                    ));
                } else {
                    contextBuilder.stash(Map.of(
                            "name", name
                    ));
                }
                out.accept(contextBuilder, partSplitter.getRemaining());
            }
        }
    }
}
