package dev.vitorsilverio.gbcemu.ppu;

public class Stat {

    private boolean LycInterrupt;
    private boolean OamInterrupt;
    private boolean VBlankInterrupt;
    private boolean HBlankInterrupt;

    public void setData(byte value) {
        this.LycInterrupt = (value & 0x40) != 0;
        this.OamInterrupt = (value & 0x20) != 0;
        this.VBlankInterrupt = (value & 0x10) != 0;
        this.HBlankInterrupt = (value & 0x08) != 0;
    }

    public byte getData() {
        return (byte) ((LycInterrupt ? 0x40 : 0) | (OamInterrupt ? 0x20 : 0) | (VBlankInterrupt ? 0x10 : 0) | (HBlankInterrupt ? 0x08 : 0));
    }

    public boolean isLycInterrupt() {
        return LycInterrupt;
    }

    public boolean isOamInterrupt() {
        return OamInterrupt;
    }

    public boolean isVBlankInterrupt() {
        return VBlankInterrupt;
    }

    public boolean isHBlankInterrupt() {
        return HBlankInterrupt;
    }


}
