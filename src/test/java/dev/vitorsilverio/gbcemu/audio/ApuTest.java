package dev.vitorsilverio.gbcemu.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApuTest {

    @Test
    void ownsAudioRegistersAndWaveRam() {
        Apu apu = new Apu((buffer, length) -> {
        });

        assertTrue(apu.contains(0xFF10));
        assertTrue(apu.contains(0xFF26));
        assertTrue(apu.contains(0xFF30));
        assertTrue(apu.contains(0xFF3F));
    }

    @Test
    void wavePatternRamIsReadableAndWritable() {
        Apu apu = new Apu((buffer, length) -> {
        });

        apu.write(0xFF30, (byte) 0xAB);
        apu.write(0xFF3F, (byte) 0xCD);

        assertEquals(0xAB, apu.read(0xFF30) & 0xFF);
        assertEquals(0xCD, apu.read(0xFF3F) & 0xFF);
    }

    @Test
    void triggeringPulseChannelSetsNr52StatusBitAndProducesSamples() {
        Apu apu = new Apu((buffer, length) -> {
        });

        apu.write(0xFF12, (byte) 0xF0);
        apu.write(0xFF13, (byte) 0x00);
        apu.write(0xFF14, (byte) 0x87);

        assertEquals(0xF1, apu.read(0xFF26) & 0xF1);

        tickUntilSamplesAreBuffered(apu);

        assertTrue(apu.bufferedSampleBytes() > 0);
    }

    @Test
    void disablingMasterAudioClearsChannelStatus() {
        Apu apu = new Apu((buffer, length) -> {
        });

        apu.write(0xFF12, (byte) 0xF0);
        apu.write(0xFF14, (byte) 0x80);
        apu.write(0xFF26, (byte) 0x00);

        assertEquals(0x70, apu.read(0xFF26) & 0xFF);
    }

    @Test
    void noiseChannelTriggerSetsNr52StatusBit() {
        Apu apu = new Apu((buffer, length) -> {
        });

        apu.write(0xFF21, (byte) 0xF0);
        apu.write(0xFF22, (byte) 0x00);
        apu.write(0xFF23, (byte) 0x80);

        assertEquals(0x88, apu.read(0xFF26) & 0x88);
    }

    private void tickUntilSamplesAreBuffered(Apu apu) {
        for (int i = 0; i < 200; i++) {
            apu.tick();
        }
    }
}
