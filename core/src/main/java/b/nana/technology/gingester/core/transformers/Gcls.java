package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;

import java.util.List;
import java.util.Optional;

@Names(1)
public final class Gcls implements Transformer<Object, Object> {

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        transform(context, out);
    }

    private <I, O> void transform(Context context, Receiver<Object> out) throws Exception {

        List<Class<? extends Transformer<?, ?>>> transformers = TransformerFactory.getTransformers().toList();
        for (Class<? extends Transformer<?, ?>> transformer : transformers) {

            String name = TransformerFactory.getUniqueName(transformer);

            //noinspection unchecked
            Optional<Class<?>> defaultParametersConstructor = TransformerFactory
                    .getParameterRichConstructor((Class<? extends Transformer<I, O>>) transformer)
                    .map(constructor -> constructor.getParameterTypes()[0]);

            if (defaultParametersConstructor.isPresent()) {
                out.accept(context, new Output(name, defaultParametersConstructor.get().getConstructor().newInstance()));
            } else {
                out.accept(context, new Output(name));
            }
        }
    }

    public static class Output {

        public String name;
        public Object defaultParameters;

        public Output(String name) {
            this(name, null);
        }

        public Output(String name, Object defaultParameters) {
            this.name = name;
            this.defaultParameters = defaultParameters;
        }
    }
}
