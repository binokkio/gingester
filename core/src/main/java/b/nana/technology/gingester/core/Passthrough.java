package b.nana.technology.gingester.core;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Passthrough<T> extends Transformer<T, T> {

    @Override
    List<Class<?>> getInputClasses() {
        // my input classes are the classes my outputs have as inputs
        return outputs.stream().flatMap(output -> output.to.getInputClasses().stream()).collect(Collectors.toList());
    }

    @Override
    List<Class<?>> getOutputClasses() {
        // my output classes are the classes my inputs have as outputs
        return inputs.stream().flatMap(input -> input.from.getOutputClasses().stream()).collect(Collectors.toList());
    }
}
