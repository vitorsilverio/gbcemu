package dev.vitorsilverio.gbcemu.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApuTest {

    @Test
    void ownsAudioRegistersAndWaveRam() {
        Apu apu = newApu();

        assertTrue(apu.contains(0xFF10));
        assertTrue(apu.contains(0xFF26));
        assertTrue(apu.contains(0xFF27));
        assertTrue(apu.contains(0xFF2F));
        assertTrue(apu.contains(0xFF30));
        assertTrue(apu.contains(0xFF3F));
        assertTrue(apu.contains(0xFF76));
        assertTrue(apu.contains(0xFF77));
    }

    @Test
    void unusedAudioRegistersReadAsFfAndIgnoreWrites() {
        Apu apu = newApu();

        apu.write(0xFF27, (byte) 0x00);
        apu.write(0xFF2F, (byte) 0x55);

        assertEquals(0xFF, apu.read(0xFF27) & 0xFF);
        assertEquals(0xFF, apu.read(0xFF2F) & 0xFF);
    }

    @Test
    void wavePatternRamIsReadableAndWritable() {
        Apu apu = newApu();

        apu.write(0xFF30, (byte) 0xAB);
        apu.write(0xFF3F, (byte) 0xCD);

        assertEquals(0xAB, apu.read(0xFF30) & 0xFF);
        assertEquals(0xCD, apu.read(0xFF3F) & 0xFF);
    }

    @Test
    void triggeringPulseChannelSetsNr52StatusBitAndProducesSamples() {
        CapturingAudioOutput output = new CapturingAudioOutput();
        Apu apu = newApu(output);

        apu.write(0xFF12, (byte) 0xF0);
        apu.write(0xFF13, (byte) 0x00);
        apu.write(0xFF14, (byte) 0x87);

        assertEquals(0xF1, apu.read(0xFF26) & 0xF1);

        tickUntilSamplesAreBuffered(apu);

        assertTrue(output.bufferedSampleBytes() > 0);
    }

    @Test
    void disablingMasterAudioClearsChannelStatus() {
        Apu apu = newApu();

        apu.write(0xFF12, (byte) 0xF0);
        apu.write(0xFF14, (byte) 0x80);
        apu.write(0xFF26, (byte) 0x00);

        assertEquals(0x70, apu.read(0xFF26) & 0xFF);
    }

    @Test
    void disabledMasterAudioKeepsProducingSilentSamples() {
        CapturingAudioOutput output = new CapturingAudioOutput();
        Apu apu = newApu(output);

        apu.write(0xFF26, (byte) 0x00);
        tickUntilSamplesAreBuffered(apu);

        assertTrue(output.bufferedSampleBytes() > 0);
    }

    @Test
    void noiseChannelTriggerSetsNr52StatusBit() {
        Apu apu = newApu();

        apu.write(0xFF21, (byte) 0xF0);
        apu.write(0xFF22, (byte) 0x00);
        apu.write(0xFF23, (byte) 0x80);

        assertEquals(0x88, apu.read(0xFF26) & 0x88);
    }

    @Test
    void sweepOverflowOnPulseTriggerDisablesChannel() {
        Apu apu = newApu();

        apu.write(0xFF10, (byte) 0x01);
        apu.write(0xFF12, (byte) 0xF0);
        apu.write(0xFF13, (byte) 0xFF);
        apu.write(0xFF14, (byte) 0x87);

        assertEquals(0xF0, apu.read(0xFF26) & 0xF1);
    }

    @Test
    void cgbPcmRegistersExposeDigitalChannelOutputs() {
        Apu apu = newApu();

        apu.write(0xFF11, (byte) 0x80);
        apu.write(0xFF12, (byte) 0xF0);
        apu.write(0xFF13, (byte) 0x00);
        apu.write(0xFF14, (byte) 0x80);

        assertEquals(0x0F, apu.read(0xFF76) & 0x0F);
        apu.write(0xFF76, (byte) 0x00);
        assertEquals(0x0F, apu.read(0xFF76) & 0x0F);
    }

    @Test
    void activeWaveChannelExposesCurrentWaveRamByteOnCgb() {
        Apu apu = newApu();

        apu.write(0xFF30, (byte) 0x12);
        apu.write(0xFF31, (byte) 0x34);
        apu.write(0xFF1A, (byte) 0x80);
        apu.write(0xFF1C, (byte) 0x20);
        apu.write(0xFF1D, (byte) 0xFF);
        apu.write(0xFF1E, (byte) 0x87);
        tick(apu, 2);

        assertEquals(0x12, apu.read(0xFF3F) & 0xFF);

        apu.write(0xFF3F, (byte) 0x56);

        assertEquals(0x56, apu.read(0xFF30) & 0xFF);
        apu.write(0xFF1A, (byte) 0x00);
        assertEquals(0x34, apu.read(0xFF31) & 0xFF);
    }

    @Test
    void disablingMasterAudioClearsWaveDigitalOutput() {
        Apu apu = newApu();

        apu.write(0xFF30, (byte) 0xF0);
        apu.write(0xFF1A, (byte) 0x80);
        apu.write(0xFF1C, (byte) 0x20);
        apu.write(0xFF1D, (byte) 0xFF);
        apu.write(0xFF1E, (byte) 0x87);
        tickUntilWaveDigitalOutputIsVisible(apu);

        assertEquals(0x0F, apu.read(0xFF77) & 0x0F);

        apu.write(0xFF26, (byte) 0x00);

        assertEquals(0x00, apu.read(0xFF77) & 0x0F);
    }

    private void tickUntilSamplesAreBuffered(Apu apu) {
        tick(apu, 200);
    }

    private Apu newApu() {
        return newApu(new CapturingAudioOutput());
    }

    private Apu newApu(AudioSampleOutput output) {
        return new Apu(output);
    }

    private void tickUntilWaveDigitalOutputIsVisible(Apu apu) {
        for (int i = 0; i < 128 && (apu.read(0xFF77) & 0x0F) == 0; i++) {
            apu.tick();
        }
    }

    private void tick(Apu apu, int ticks) {
        for (int i = 0; i < ticks; i++) {
            apu.tick();
        }
    }

    private static final class CapturingAudioOutput implements AudioSampleOutput {
        private int sampleBytes;
        private int previousLeft;
        private int previousRight;

        @Override
        public void writeStereoSample(int left, int right) {
            previousLeft = left;
            previousRight = right;
            sampleBytes += 4;
        }

        @Override
        public void writeSilentSample() {
            writeStereoSample(0, 0);
        }

        @Override
        public void restoreHighPassFilter(int leftCapacitor, int rightCapacitor) {
            previousLeft = leftCapacitor;
            previousRight = rightCapacitor;
            sampleBytes = 0;
        }

        @Override
        public int previousLeftSample() {
            return previousLeft;
        }

        @Override
        public int previousRightSample() {
            return previousRight;
        }

        @Override
        public int bufferedSampleBytes() {
            return sampleBytes;
        }
    }
}
