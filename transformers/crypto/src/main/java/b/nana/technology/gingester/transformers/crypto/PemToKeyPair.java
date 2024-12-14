package b.nana.technology.gingester.transformers.crypto;

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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Experimental
@Names(1)
public final class PemToKeyPair implements Transformer<String, KeyPair> {

    private static final Pattern algorithmPattern = Pattern.compile("BEGIN (\\w+) PRIVATE KEY");

    private final String givenAlgorithm;

    public PemToKeyPair(Parameters parameters) {
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
    public void transform(Context context, String in, Receiver<KeyPair> out) throws NoSuchAlgorithmException, InvalidKeySpecException {

        // use given algorithm or look for it in the PEM header
        String algorithm = getGivenAlgorithm().orElseGet(() -> {
            Matcher matcher = algorithmPattern.matcher(in);
            if (!matcher.find())
                throw new IllegalArgumentException("Can't find private key header");
            return matcher.group(1);
        });

        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

        // assume PKCS8 encoding
        byte[] keyBytes = Base64.getMimeDecoder().decode(in.replaceAll("-.*-", ""));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        if (algorithm.equals("RSA")) {

            RSAPrivateCrtKey privk = (RSAPrivateCrtKey) keyFactory.generatePrivate(keySpec);
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());
            PublicKey pubk = keyFactory.generatePublic(publicKeySpec);
            KeyPair keyPair = new KeyPair(pubk, privk);
            out.accept(context.stash(Map.of(
                    "privk", privk,
                    "pubk", pubk
            )), keyPair);

        } else throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"algorithm"})
    public static class Parameters {
        public String algorithm = "RSA";
    }
}
