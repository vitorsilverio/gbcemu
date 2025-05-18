package dev.vitorsilverio.gbcemu.interrupt;

public enum Interrupt {
    VBLANK(0x01, 0x0040),
    LCD_STAT(0x02, 0x0048),
    TIMER(0x04, 0x0050),
    SERIAL(0x08, 0x0058),
    JOYPAD(0x10, 0x0060);

    private final int mask;
    private final int vectorAddress;

    Interrupt(int mask, int vectorAddress) {
        this.mask = mask;
        this.vectorAddress = vectorAddress;
    }

    public int getMask() {
        return mask;
    }

    public int getVectorAddress() {
        return vectorAddress;
    }
}
