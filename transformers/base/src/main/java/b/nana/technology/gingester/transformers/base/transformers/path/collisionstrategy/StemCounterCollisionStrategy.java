package b.nana.technology.gingester.transformers.base.transformers.path.collisionstrategy;

import java.nio.file.Path;

public final class StemCounterCollisionStrategy extends CounterCollisionStrategy {

    @Override
    protected String getCounterSibling(Path path, int counter) {

        String filename = path.getFileName().toString();
        String[] parts = filename.split("\\.", 2);

        StringBuilder result = new StringBuilder();
        result
                .append(parts[0])
                .append('-')
                .append(counter);

        if (parts.length > 1)
            result
                    .append('.')
                    .append(parts[1]);

        return result.toString();
    }
}
