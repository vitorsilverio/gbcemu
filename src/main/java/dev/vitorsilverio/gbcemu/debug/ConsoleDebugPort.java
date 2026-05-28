package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.link.LinkCable;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.misc.GameSharkDevice;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoy;

import java.util.function.BooleanSupplier;

public record ConsoleDebugPort(
        Cpu cpu,
        Bus bus,
        DebugMemoryInterface memory,
        Ppu ppu,
        DebugAudioInterface audio,
        DebugCartInterface cart,
        GameSharkDevice gameSharkDevice,
        DebuggerInterface debugger,
        LinkCable linkCable,
        SuperGameBoy superGameBoy,
        BooleanSupplier pausedSupplier
) {
}
