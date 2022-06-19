package b.nana.technology.gingester.transformers.audio.string;

import com.sun.speech.freetts.audio.AudioPlayer;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.OutputStream;

final class PcmStreamAudioPlayer implements AudioPlayer {

    private final OutputStream outputStream;
    private AudioFormat audioFormat;

    PcmStreamAudioPlayer(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public boolean write(byte[] audioData) {
        return this.write(audioData, 0, audioData.length);
    }

    public boolean write(byte[] bytes, int offset, int size) {
        try {
            this.outputStream.write(bytes, offset, size);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            this.outputStream.flush();
            this.outputStream.close();
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
