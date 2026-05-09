package dev.vitorsilverio.gbcemu.audio;

final class ApuFrameSequencer {
    private static final int CPU_CLOCK_HZ = 4_194_304;
    private static final int FRAME_SEQUENCER_CYCLES = CPU_CLOCK_HZ / 512;

    private int cycles;
    private int step;

    void load(int cycles, int step) {
        this.cycles = cycles;
        this.step = step & 0x07;
    }

    void reset() {
        cycles = 0;
        step = 0;
    }

    int cycles() {
        return cycles;
    }

    int step() {
        return step;
    }

    void tick(PulseChannel channel1, PulseChannel channel2, WaveChannel channel3, NoiseChannel channel4) {
        cycles++;
        if (cycles < FRAME_SEQUENCER_CYCLES) {
            return;
        }
        cycles = 0;
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
