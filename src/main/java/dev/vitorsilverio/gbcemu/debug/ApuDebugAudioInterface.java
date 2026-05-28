package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.audio.Apu;
import dev.vitorsilverio.gbcemu.audio.ApuDebugSnapshot;

public class ApuDebugAudioInterface implements DebugAudioInterface {
    private final Apu apu;

    public ApuDebugAudioInterface(Apu apu) {
        this.apu = apu;
    }

    @Override
    public void setWriteTraceEnabled(boolean enabled) {
        apu.setDebugWriteTraceEnabled(enabled);
    }

    @Override
    public ApuDebugSnapshot snapshot() {
        return apu.debugSnapshot();
    }

    @Override
    public int masterVolume() {
        return apu.debugMasterVolume();
    }

    @Override
    public void setMasterVolume(int volume) {
        apu.setDebugMasterVolume(volume);
    }

    @Override
    public int leftVolume() {
        return apu.debugLeftVolume();
    }

    @Override
    public void setLeftVolume(int volume) {
        apu.setDebugLeftVolume(volume);
    }

    @Override
    public int rightVolume() {
        return apu.debugRightVolume();
    }

    @Override
    public void setRightVolume(int volume) {
        apu.setDebugRightVolume(volume);
    }

    @Override
    public int lowPassAlpha() {
        return apu.debugLowPassAlpha();
    }

    @Override
    public void setLowPassAlpha(int alpha) {
        apu.setDebugLowPassAlpha(alpha);
    }

    @Override
    public int channelVolume(int channel) {
        return apu.debugChannelVolume(channel);
    }

    @Override
    public void setChannelVolume(int channel, int volume) {
        apu.setDebugChannelVolume(channel, volume);
    }

    @Override
    public boolean channelMuted(int channel) {
        return apu.debugChannelMuted(channel);
    }

    @Override
    public void setChannelMuted(int channel, boolean muted) {
        apu.setDebugChannelMuted(channel, muted);
    }
}
