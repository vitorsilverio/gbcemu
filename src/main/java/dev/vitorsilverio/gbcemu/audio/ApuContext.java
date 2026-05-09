package dev.vitorsilverio.gbcemu.audio;

interface ApuContext {
    byte register(int address);

    void setRegister(int address, byte value);

    byte wavePatternRam(int offset);

    int period(int lowAddress, int highAddress);

    int frameSequencerStep();
}
