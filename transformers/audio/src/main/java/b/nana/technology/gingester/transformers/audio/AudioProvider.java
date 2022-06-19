package b.nana.technology.gingester.transformers.audio;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.audio.string.Speak;
import b.nana.technology.gingester.transformers.audio.string.ToSpeech;

import java.util.Collection;
import java.util.List;

public final class AudioProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Play.class,
                Speak.class,
                ToSpeech.class
        );
    }
}
