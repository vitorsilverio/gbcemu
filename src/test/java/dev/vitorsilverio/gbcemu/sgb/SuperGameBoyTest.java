package dev.vitorsilverio.gbcemu.sgb;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperGameBoyTest {

    @Test
    void disabledSgbIgnoresJoypPackets() {
        Ppu ppu = new Ppu(new Bus());
        SuperGameBoy sgb = new SuperGameBoy(false, ppu);

        sendPacket(sgb, packet(0x11, 1, 0x01));

        assertFalse(sgb.isEnabled());
        assertEquals(1, sgb.joypadCount());
        assertEquals(0x0F, sgb.joypadIdNibble());
    }

    @Test
    void multiplayerRequestSelectsTwoJoypads() {
        Ppu ppu = new Ppu(new Bus());
        SuperGameBoy sgb = new SuperGameBoy(true, ppu);

        sendPacket(sgb, packet(0x11, 1, 0x01));
        sendPacket(sgb, packet(0x17, 1, 0x03));

        assertEquals("MASK_EN", sgb.lastCommandName());
        assertEquals(2, sgb.joypadCount());
        assertEquals(3, sgb.maskMode());
        assertEquals(0, sgb.selectedJoypad());
        assertEquals(0x0F, sgb.joypadIdNibble());

        sgb.writeJoypad(0x30);

        assertEquals(1, sgb.selectedJoypad());
        assertEquals(0x0E, sgb.joypadIdNibble());
    }

    @Test
    void paletteCommandIsNamedAndDoesNotBreakPacketAlignment() {
        Ppu ppu = new Ppu(new Bus());
        SuperGameBoy sgb = new SuperGameBoy(true, ppu);

        sendPacket(sgb, packet(0x00, 1, 0x00));
        sendPacket(sgb, packet(0x11, 1, 0x01));

        assertEquals("MLT_REQ", sgb.lastCommandName());
        assertEquals(2, sgb.packetsReceived());
        assertEquals(0, sgb.invalidPackets());
        assertEquals(2, sgb.joypadCount());
    }

    @Test
    void ordinaryJoypadPollingDoesNotCreateInvalidPackets() {
        Ppu ppu = new Ppu(new Bus());
        SuperGameBoy sgb = new SuperGameBoy(true, ppu);

        sgb.writeJoypad(0x30);
        sgb.writeJoypad(0x20);
        sgb.writeJoypad(0x30);
        sgb.writeJoypad(0x10);
        sgb.writeJoypad(0x30);

        assertEquals(0, sgb.packetsReceived());
        assertEquals(0, sgb.invalidPackets());
        assertFalse(sgb.isReceivingPacket());
    }

    @Test
    void allZeroPacketIsIgnoredAsJoypadNoise() {
        Ppu ppu = new Ppu(new Bus());
        SuperGameBoy sgb = new SuperGameBoy(true, ppu);

        sendPacket(sgb, new byte[16]);

        assertEquals(0, sgb.packetsReceived());
        assertEquals(0, sgb.invalidPackets());
        assertEquals(1, sgb.ignoredPackets());
        assertEquals("", sgb.lastCommandName());
    }

    @Test
    void characterAndPictureTransfersBuildBorderImage() {
        Ppu ppu = new Ppu(new Bus());
        SuperGameBoy sgb = new SuperGameBoy(true, ppu);
        fillTileOneWithColorOne(ppu);
        sendPacket(sgb, packet(0x13, 1, 0x00));

        writePictureTransfer(ppu);
        sendPacket(sgb, packet(0x14, 1, 0x00));

        assertTrue(sgb.hasBorder());
        assertEquals("PCT_TRN", sgb.lastCommandName());
        BufferedImage image = sgb.copyBorderImage();
        assertNotNull(image);
        assertEquals(0xFFFF0000, image.getRGB(0, 0));
    }

    private void fillTileOneWithColorOne(Ppu ppu) {
        int tileOneOffset = 32;
        for (int row = 0; row < 8; row++) {
            ppu.getVideoRam().writeBank(0, tileOneOffset + row * 2, (byte) 0xFF);
            ppu.getVideoRam().writeBank(0, tileOneOffset + row * 2 + 1, (byte) 0x00);
            ppu.getVideoRam().writeBank(0, tileOneOffset + 16 + row * 2, (byte) 0x00);
            ppu.getVideoRam().writeBank(0, tileOneOffset + 16 + row * 2 + 1, (byte) 0x00);
        }
    }

    private void writePictureTransfer(Ppu ppu) {
        int entry = 1 | (4 << 10);
        ppu.getVideoRam().writeBank(0, 0, (byte) (entry & 0xFF));
        ppu.getVideoRam().writeBank(0, 1, (byte) ((entry >> 8) & 0xFF));
        ppu.getVideoRam().writeBank(0, 0x802, (byte) 0x1F);
        ppu.getVideoRam().writeBank(0, 0x803, (byte) 0x00);
    }

    private byte[] packet(int command, int length, int firstArgument) {
        byte[] packet = new byte[16];
        packet[0] = (byte) ((command << 3) | length);
        packet[1] = (byte) firstArgument;
        return packet;
    }

    private void sendPacket(SuperGameBoy sgb, byte[] packet) {
        sgb.writeJoypad(0x00);
        for (byte value : packet) {
            for (int bit = 0; bit < 8; bit++) {
                sgb.writeJoypad(0x30);
                sgb.writeJoypad(((value >> bit) & 1) == 0 ? 0x20 : 0x10);
            }
        }
        sgb.writeJoypad(0x30);
        sgb.writeJoypad(0x20);
    }
}
