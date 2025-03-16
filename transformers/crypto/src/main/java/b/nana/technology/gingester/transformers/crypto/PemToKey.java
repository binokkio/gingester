package b.nana.technology.gingester.transformers.crypto;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Experimental
@Names(1)
@Example(example = "", description = "Decode and stash private and/or public key from PEM input")
public final class PemToKey implements Transformer<String, Key> {

    private static final Pattern algorithmPattern = Pattern.compile("BEGIN (?:(\\w+) )?(PRIVATE|PUBLIC) KEY");

    private final String givenAlgorithm;

    public PemToKey(Parameters parameters) {
        givenAlgorithm = parameters.algorithm;
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.of(Map.of(
                "privk", PrivateKey.class,
                "pubk", PublicKey.class
        ));
    }

    private Optional<String> getGivenAlgorithm() {
        return Optional.ofNullable(givenAlgorithm);
    }

    @Override
    public void transform(Context context, String in, Receiver<Key> out) throws NoSuchAlgorithmException, InvalidKeySpecException {

        // find key header
        Matcher matcher = algorithmPattern.matcher(in);
        if (!matcher.find()) throw new IllegalArgumentException("Can't find key header");

        boolean isPrivate = matcher.group(2).equals("PRIVATE");

        // use given algorithm or look for it in the PEM header
        String algorithm = getGivenAlgorithm().orElse(matcher.group(1));
        if (algorithm == null) throw new IllegalArgumentException("Algorithm not given and not present in key header");

        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        byte[] keyBytes = Base64.getMimeDecoder().decode(in.replaceAll("-.*-", ""));

        if (isPrivate) {

            // assume PKCS8 encoding
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

            if (algorithm.equals("RSA")) {

                RSAPrivateCrtKey privk = (RSAPrivateCrtKey) keyFactory.generatePrivate(keySpec);
                RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());
                PublicKey pubk = keyFactory.generatePublic(publicKeySpec);
                out.accept(context.stash(Map.of(
                        "privk", privk,
                        "pubk", pubk
                )), privk);

            } else throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);

        } else {

            // assume X509 encoding
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

            if (algorithm.equals("RSA")) {

                RSAPublicKey pubk = (RSAPublicKey) keyFactory.generatePublic(keySpec);

                out.accept(context.stash(Map.of(
                        "pubk", pubk
                )), pubk);

            } else throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"algorithm"})
    public static class Parameters {
        public String algorithm = "RSA";
    }
}
