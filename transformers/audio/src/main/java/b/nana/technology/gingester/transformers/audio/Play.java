package b.nana.technology.gingester.transformers.audio;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Play implements Transformer<InputStream, Void> {

    @Override
    public void transform(Context context, InputStream in, Receiver<Void> out) throws LineUnavailableException, UnsupportedAudioFileException, IOException {

//        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, true);
//        AudioInputStream audioStream = new AudioInputStream(in, audioFormat, -1);

        AudioInputStream audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
        AudioFormat audioFormat = audioStream.getFormat();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceLine.open(audioFormat);
        sourceLine.start();

        int read;
        byte[] buffer = new byte[8192];
        while ((read = audioStream.read(buffer, 0, buffer.length)) != -1) {
            sourceLine.write(buffer, 0, read);
        }

        sourceLine.drain();
        sourceLine.close();
    }
}
