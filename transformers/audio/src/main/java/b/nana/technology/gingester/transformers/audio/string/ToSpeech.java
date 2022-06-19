package b.nana.technology.gingester.transformers.audio.string;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.AudioPlayer;

import java.io.OutputStream;

@Names(1)
public final class ToSpeech implements Transformer<String, OutputStreamWrapper> {

    private final Encoding encoding;

    public ToSpeech(Parameters parameters) {
        encoding = parameters.encoding;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.requireOutgoingAsync();
    }

    @Override
    public void transform(Context context, String in, Receiver<OutputStreamWrapper> out) {

        OutputStreamWrapper outputStreamWrapper = new OutputStreamWrapper();
        AudioPlayer audioPlayer = getAudioPlayer(outputStreamWrapper);
        out.accept(context, outputStreamWrapper);

        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        VoiceManager voiceManager = VoiceManager.getInstance();
        Voice voice = voiceManager.getVoice("kevin16");
        voice.setAudioPlayer(audioPlayer);
        voice.allocate();
        voice.speak(in);
        voice.deallocate();
        audioPlayer.close();
    }

    private AudioPlayer getAudioPlayer(OutputStream outputStream) {
        switch (encoding) {
            case PCM: return new PcmStreamAudioPlayer(outputStream);
            case WAV: return new WavStreamAudioPlayer(outputStream);
            default: throw new IllegalArgumentException("No case for: " + encoding);
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, encoding -> o("encoding", encoding));
            }
        }

        public Encoding encoding = Encoding.WAV;
    }

    public enum Encoding {
        PCM,
        WAV
    }
}
