package dev.vitorsilverio.gbcemu.audio;

final class ApuFrameSequencer {
    private int step = 7;

    void load(int step) {
        this.step = step & 0x07;
    }

    void reset() {
        step = 7;
    }

    int step() {
        return step;
    }

    void clock(PulseChannel channel1, PulseChannel channel2, WaveChannel channel3, NoiseChannel channel4) {
        step = (step + 1) & 0x07;

        if ((step & 1) == 0) {
            channel1.tickLength();
            channel2.tickLength();
            channel3.tickLength();
            channel4.tickLength();
        }
        if (step == 2 || step == 6) {
            channel1.tickSweep();
        }
        if (step == 7) {
            channel1.tickEnvelope();
            channel2.tickEnvelope();
            channel4.tickEnvelope();
        }
    }
}
