package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.audio.ApuDebugSnapshot;

public interface DebugAudioInterface {
    void setWriteTraceEnabled(boolean enabled);

    ApuDebugSnapshot snapshot();

    int masterVolume();

    void setMasterVolume(int volume);

    int leftVolume();

    void setLeftVolume(int volume);

    int rightVolume();

    void setRightVolume(int volume);

    int lowPassAlpha();

    void setLowPassAlpha(int alpha);

    int channelVolume(int channel);

    void setChannelVolume(int channel, int volume);

    boolean channelMuted(int channel);

    void setChannelMuted(int channel, boolean muted);
}
