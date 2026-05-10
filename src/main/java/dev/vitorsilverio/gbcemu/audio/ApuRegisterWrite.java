package dev.vitorsilverio.gbcemu.audio;

public record ApuRegisterWrite(
        long sequence,
        int address,
        int value,
        String detail
) {
}
