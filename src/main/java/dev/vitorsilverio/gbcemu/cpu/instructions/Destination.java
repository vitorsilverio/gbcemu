package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;

public interface Destination {

    void setValue(Cpu cpu, int value);

}
