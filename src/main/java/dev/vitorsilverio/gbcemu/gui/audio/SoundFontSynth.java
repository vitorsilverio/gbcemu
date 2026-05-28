package dev.vitorsilverio.gbcemu.gui.audio;

import dev.vitorsilverio.gbcemu.config.AppSettings;

import javax.sound.midi.*;
import java.io.File;

final class SoundFontSynth implements AutoCloseable {
    private static final int CHANNEL_COUNT = 4;
    private static final int PERCUSSION_MIDI_CHANNEL = 9;

    private final int[] activeNotes = {-1, -1, -1, -1};
    private final int[] activeVelocities = new int[CHANNEL_COUNT];
    private Synthesizer synthesizer;
    private MidiChannel[] midiChannels = new MidiChannel[0];
    private String mode = "Off";
    private String lastError = "";

    void apply(AppSettings.AudioEnhancementConfig config) {
        AppSettings.AudioEnhancementConfig fallback = AppSettings.defaults().normalizedAudioEnhancement();
        String requestedMode = config == null ? fallback.soundFontMode() : config.soundFontMode();
        String requestedPath = config == null ? fallback.soundFontPath() : config.soundFontPath();
        close();
        mode = normalizeMode(requestedMode);
        lastError = "";
        if ("Off".equals(mode)) {
            return;
        }
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            loadSoundFont(requestedPath);
            midiChannels = synthesizer.getChannels();
            configureChannels();
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            close();
        }
    }

    void updateChannelState(int channel, boolean enabled, double frequencyHz, int volume, boolean noise) {
        if (!active() || channel < 1 || channel > CHANNEL_COUNT) {
            return;
        }
        int index = channel - 1;
        if ("Percussion overlay".equals(mode) && !noise) {
            noteOff(index);
            return;
        }
        if (!enabled || volume <= 0 || frequencyHz <= 0) {
            noteOff(index);
            return;
        }
        int midiChannelIndex = midiChannel(index, noise);
        if (midiChannelIndex < 0 || midiChannelIndex >= midiChannels.length || midiChannels[midiChannelIndex] == null) {
            return;
        }
        int note = noise ? percussionNote(frequencyHz) : midiNote(frequencyHz);
        int velocity = Math.max(1, Math.min(127, volume * 8 + 7));
        if (activeNotes[index] == note && Math.abs(activeVelocities[index] - velocity) < 8) {
            return;
        }
        noteOff(index);
        midiChannels[midiChannelIndex].noteOn(note, velocity);
        activeNotes[index] = note;
        activeVelocities[index] = velocity;
    }

    boolean suppressPcmOutput() {
        return active() && "Replace original".equals(mode);
    }

    String debugDescription() {
        if (active()) {
            return "SoundFont " + mode;
        }
        if (lastError == null || lastError.isBlank()) {
            return "SoundFont off";
        }
        return "SoundFont unavailable (" + lastError + ")";
    }

    @Override
    public void close() {
        for (int i = 0; i < activeNotes.length; i++) {
            noteOff(i);
        }
        if (synthesizer != null) {
            synthesizer.close();
            synthesizer = null;
        }
        midiChannels = new MidiChannel[0];
    }

    private boolean active() {
        return synthesizer != null && synthesizer.isOpen() && !"Off".equals(mode);
    }

    private String normalizeMode(String value) {
        if ("Overlay".equals(value) || "Replace original".equals(value) || "Percussion overlay".equals(value)) {
            return value;
        }
        return "Off";
    }

    private void loadSoundFont(String path) throws Exception {
        if (path == null || path.isBlank()) {
            return;
        }
        File file = new File(path);
        if (!file.isFile()) {
            throw new IllegalArgumentException("SoundFont file not found: " + path);
        }
        Soundbank soundbank = MidiSystem.getSoundbank(file);
        if (!synthesizer.isSoundbankSupported(soundbank)) {
            throw new IllegalArgumentException("SoundFont is not supported by the current synthesizer");
        }
        Soundbank defaultSoundbank = synthesizer.getDefaultSoundbank();
        if (defaultSoundbank != null) {
            synthesizer.unloadAllInstruments(defaultSoundbank);
        }
        synthesizer.loadAllInstruments(soundbank);
    }

    private void configureChannels() {
        if (midiChannels.length == 0) {
            return;
        }
        Instrument[] instruments = synthesizer.getLoadedInstruments();
        for (int i = 0; i < Math.min(3, midiChannels.length); i++) {
            midiChannels[i].programChange(instruments.length > 0 ? instruments[0].getPatch().getProgram() : 0);
            midiChannels[i].controlChange(7, 96);
            midiChannels[i].controlChange(10, i == 0 ? 48 : i == 1 ? 80 : 64);
        }
        if (PERCUSSION_MIDI_CHANNEL < midiChannels.length && midiChannels[PERCUSSION_MIDI_CHANNEL] != null) {
            midiChannels[PERCUSSION_MIDI_CHANNEL].controlChange(7, 96);
        }
    }

    private int midiChannel(int channelIndex, boolean noise) {
        if (noise) {
            return PERCUSSION_MIDI_CHANNEL;
        }
        return channelIndex;
    }

    private int midiNote(double frequencyHz) {
        int note = (int) Math.round(69.0 + 12.0 * (Math.log(frequencyHz / 440.0) / Math.log(2.0)));
        return Math.max(0, Math.min(127, note));
    }

    private int percussionNote(double frequencyHz) {
        if (frequencyHz > 8_000) {
            return 42;
        }
        if (frequencyHz > 2_000) {
            return 38;
        }
        return 36;
    }

    private void noteOff(int index) {
        int note = activeNotes[index];
        if (note < 0 || midiChannels.length == 0) {
            activeNotes[index] = -1;
            return;
        }
        int channel = midiChannel(index, index == 3);
        if (channel >= 0 && channel < midiChannels.length && midiChannels[channel] != null) {
            midiChannels[channel].noteOff(note);
        }
        activeNotes[index] = -1;
        activeVelocities[index] = 0;
    }
}
