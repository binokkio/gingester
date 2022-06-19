package b.nana.technology.gingester.transformers.audio.string;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

@Names(1)
public final class Speak implements Transformer<String, String> {

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        VoiceManager voiceManager = VoiceManager.getInstance();
        Voice voice = voiceManager.getVoice("kevin16");
        voice.allocate();
        voice.speak(in);
        voice.deallocate();
        out.accept(context, in);
    }
}
