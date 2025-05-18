package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;

public interface Source {

    int getValue(Cpu cpu);

}
