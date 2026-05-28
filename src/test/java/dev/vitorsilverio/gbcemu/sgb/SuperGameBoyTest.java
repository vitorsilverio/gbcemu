package dev.vitorsilverio.gbcemu.sgb;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.ppu.PpuState;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.util.RawImage;
import org.junit.jupiter.api.Test;

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
        consumeTransferWindow(sgb);

        renderPictureTransfer(ppu);
        sendPacket(sgb, packet(0x14, 1, 0x00));
        consumeTransferWindow(sgb);

        assertTrue(sgb.hasBorder());
        assertEquals("PCT_TRN", sgb.lastCommandName());
        RawImage image = sgb.copyBorderImage();
        assertNotNull(image);
        assertEquals(0xFFFF0000, image.getArgb(0, 0));
    }

    private void fillTileOneWithColorOne(Ppu ppu) {
        byte[] transfer = new byte[0x1000];
        for (int row = 0; row < 8; row++) {
            transfer[32 + row * 2] = (byte) 0xFF;
        }
        prepareRenderedTransfer(ppu, transfer);
    }

    private void renderPictureTransfer(Ppu ppu) {
        byte[] transfer = new byte[0x1000];
        int entry = 1 | (4 << 10);
        transfer[0] = (byte) (entry & 0xFF);
        transfer[1] = (byte) ((entry >> 8) & 0xFF);
        transfer[0x802] = (byte) 0x1F;
        prepareRenderedTransfer(ppu, transfer);
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

    private void consumeTransferWindow(SuperGameBoy sgb) {
        while (sgb.pendingTransferCount() > 0) {
            sgb.consumeTransferFrame();
        }
    }

    private void prepareRenderedTransfer(Ppu ppu, byte[] transfer) {
        PpuState state = ppu.saveState();
        int[][] bgColorIndexes = new int[160][144];
        for (int y = 0; y < 144; y++) {
            int rowOffset = 2 * ((y & 0x07) + (y >> 3) * 160);
            for (int x = 0; x < 160; x += 8) {
                int byteOffset = rowOffset + (x << 1);
                if (byteOffset + 1 >= transfer.length) {
                    continue;
                }
                int low = transfer[byteOffset] & 0xFF;
                int high = transfer[byteOffset + 1] & 0xFF;
                for (int bit = 0; bit < 8; bit++) {
                    int shift = 7 - bit;
                    bgColorIndexes[x + bit][y] = ((low >> shift) & 0x01) | (((high >> shift) & 0x01) << 1);
                }
            }
        }
        ppu.loadState(new PpuState(
                state.control(),
                state.stat(),
                state.bgPalette(),
                state.bgPaletteIndex(),
                state.objPalette(),
                state.objPaletteIndex(),
                state.bgPaletteDmg(),
                state.obj0PaletteDmg(),
                state.obj1PaletteDmg(),
                state.frameBuffer(),
                bgColorIndexes,
                state.resolvedColorIndexes(),
                state.bgPriorities(),
                state.cgbMode(),
                state.cycles(),
                state.mode(),
                state.currentLine(),
                state.currentColumn(),
                state.scrollX(),
                state.scrollY(),
                state.penaltyDelay(),
                state.hBlankCycles(),
                state.objectPriorityMode(),
                state.lineCompare(),
                state.windowX(),
                state.windowY(),
                state.previousStatSignal(),
                state.frameReady(),
                state.windowYCondition(),
                state.windowLineCounter(),
                state.windowStartedOnLine(),
                state.spriteCandidateCount()
        ));
    }
}
