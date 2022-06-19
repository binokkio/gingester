package b.nana.technology.gingester.transformers.audio.string;

import com.sun.speech.freetts.audio.AudioPlayer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class WavStreamAudioPlayer implements AudioPlayer {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final OutputStream outputStream;
    private AudioFormat audioFormat;

    WavStreamAudioPlayer(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public boolean write(byte[] audioData) {
        return this.write(audioData, 0, audioData.length);
    }

    public boolean write(byte[] bytes, int offset, int size) {
        byteArrayOutputStream.write(bytes, offset, size);
        return true;
    }

    public void close() {

        byte[] bytes = byteArrayOutputStream.toByteArray();

        AudioInputStream audioInputStream = new AudioInputStream(
                new ByteArrayInputStream(bytes),
                audioFormat,
                bytes.length / audioFormat.getFrameSize()
        );

        try {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AudioFormat getAudioFormat() {
        return this.audioFormat;
    }

    public void setAudioFormat(AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }

    public float getVolume() {
        return 1F;
    }

    public void setVolume(float volume) {}

    public long getTime() {
        return -1L;
    }

    public void resetTime() {}

    public void pause() {}

    public void resume() {}

    public void cancel() {}

    public void startFirstSampleTimer() {}

    public void reset() {}

    public boolean drain() {
        return true;
    }

    public void begin(int size) {}

    public boolean end() {
        return true;
    }

    public void showMetrics() {}
}
