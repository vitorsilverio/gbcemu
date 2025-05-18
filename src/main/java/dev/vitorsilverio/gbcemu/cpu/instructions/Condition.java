package dev.vitorsilverio.gbcemu.cpu.instructions;

public enum Condition {
    CARRY("C"),
    NOT_CARRY("NC"),
    ZERO("Z"),
    NOT_ZERO("NZ");

    private final String mnemonic;

    Condition(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    @Override
    public String toString() {
        return mnemonic;
    }
}
