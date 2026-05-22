package dev.vitorsilverio.gbcemu.snapshot;

import dev.vitorsilverio.gbcemu.cartridge.CartState;
import dev.vitorsilverio.gbcemu.audio.ApuState;
import dev.vitorsilverio.gbcemu.cpu.CpuState;
import dev.vitorsilverio.gbcemu.interrupt.InterruptState;
import dev.vitorsilverio.gbcemu.memory.BiosState;
import dev.vitorsilverio.gbcemu.memory.WorkRamState;
import dev.vitorsilverio.gbcemu.memory.ZeroPageState;
import dev.vitorsilverio.gbcemu.misc.DmaState;
import dev.vitorsilverio.gbcemu.misc.CgbUndocumentedRegistersState;
import dev.vitorsilverio.gbcemu.misc.HdmaState;
import dev.vitorsilverio.gbcemu.misc.InfraredState;
import dev.vitorsilverio.gbcemu.misc.Key0State;
import dev.vitorsilverio.gbcemu.misc.Key1State;
import dev.vitorsilverio.gbcemu.peripherals.SerialState;
import dev.vitorsilverio.gbcemu.peripherals.TimerState;
import dev.vitorsilverio.gbcemu.ppu.OamState;
import dev.vitorsilverio.gbcemu.ppu.PpuState;
import dev.vitorsilverio.gbcemu.ppu.VideoRamState;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoyState;

import java.io.Serializable;

public record EmulatorState(
        int version,
        InterruptState interruptManager,
        BiosState bios,
        CpuState cpu,
        TimerState timer,
        PpuState ppu,
        VideoRamState videoRam,
        OamState oam,
        ApuState apu,
        SerialState serial,
        HdmaState hdma,
        DmaState dma,
        WorkRamState workRam,
        ZeroPageState zeroPage,
        CartState cart,
        Key0State key0,
        Key1State key1,
        InfraredState infrared,
        CgbUndocumentedRegistersState cgbUndocumentedRegisters,
        SuperGameBoyState superGameBoy
) implements Serializable {
    public static final int CURRENT_VERSION = 2;
}
