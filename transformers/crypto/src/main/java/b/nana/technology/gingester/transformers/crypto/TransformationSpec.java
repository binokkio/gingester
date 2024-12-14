package b.nana.technology.gingester.transformers.crypto;

import java.util.Optional;

public class TransformationSpec {

    private final String transformation;
    private final String algorithm;
    private final String mode;
    private final String padding;

    public TransformationSpec(String transformationSpec) {
        this.transformation = transformationSpec;
        if (transformationSpec != null) {
            String[] parts = transformationSpec.split("/");
            algorithm = parts[0];
            if (parts.length == 3) {
                mode = parts[1];
                padding = parts[2];
            } else if (parts.length != 1) {
                throw new IllegalArgumentException("Invalid transformation: " + transformationSpec);
            } else {
                mode = padding = null;
            }
        } else {
            algorithm = mode = padding = null;
        }
    }

    public Optional<String> getTransformation() {
        return Optional.ofNullable(transformation);
    }

    public Optional<String> getAlgorithm() {
        return Optional.ofNullable(algorithm);
    }

    public Optional<String> getMode() {
        return Optional.ofNullable(mode);
    }

    public Optional<String> getPadding() {
        return Optional.ofNullable(padding);
    }
}
