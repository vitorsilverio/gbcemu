package dev.vitorsilverio.gbcemu.cpu;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Cpu implements MachineCycle, Snapshottable, Stateful<CpuState> {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Cpu.class);
    private static final int BUS_CYCLE_TICKS = 4;
    private static final int INTERRUPT_TICKS = 20;

    private final Bus bus;
    private final Decoder decoder;

    @Savable
    private int instructionTicks;
    private Runnable cycleCallback = () -> {
    };

    @Savable
    private int speedRate = 1;


    @Savable private int pc = 0x0000; // Program Counter
    @Savable private int sp = 0xFFFE; // Stack Pointer
    @Savable private byte a = 0; // Accumulator
    @Savable private byte b = 0; // Register B
    @Savable private byte c = 0; // Register C
    @Savable private byte d = 0; // Register D
    @Savable private byte e = 0; // Register E
    @Savable private byte h = 0; // Register H
    @Savable private byte l = 0; // Register L

    @Savable private boolean zeroFlag = false; // Zero Flag
    @Savable private boolean negativeFlag = false; // Subtract Flag
    @Savable private boolean halfCarryFlag = false; // Half Carry Flag
    @Savable private boolean carryFlag = false; // Carry Flag

    @Savable private boolean halted = false;
    @Savable private boolean stopped = false;
    @Savable private boolean ime = false; // Interrupt Master Enable
    @Savable private int imeEnableDelay = 0;
    @Savable private boolean haltBug = false; // Halt Bug


    public Cpu(Bus bus) {
        this.bus = bus;
        this.decoder = new Decoder();
    }

    @Override
    public CpuState saveState() {
        return new CpuState(
                instructionTicks,
                speedRate,
                pc,
                sp,
                a,
                b,
                c,
                d,
                e,
                h,
                l,
                zeroFlag,
                negativeFlag,
                halfCarryFlag,
                carryFlag,
                halted,
                stopped,
                ime,
                imeEnableDelay,
                haltBug
        );
    }

    @Override
    public void loadState(CpuState state) {
        instructionTicks = state.instructionTicks();
        speedRate = state.speedRate();
        pc = state.pc() & 0xFFFF;
        sp = state.sp() & 0xFFFF;
        a = state.a();
        b = state.b();
        c = state.c();
        d = state.d();
        e = state.e();
        h = state.h();
        l = state.l();
        zeroFlag = state.zeroFlag();
        negativeFlag = state.negativeFlag();
        halfCarryFlag = state.halfCarryFlag();
        carryFlag = state.carryFlag();
        halted = state.halted();
        stopped = state.stopped();
        ime = state.ime();
        imeEnableDelay = state.imeEnableDelay();
        haltBug = state.haltBug();
    }

    @Override
    public Snapshot createSnapshot(int version) {
        Map<String, Object> state = new HashMap<>();
        state.put("state", saveState());
        return new Snapshot(getClass().getName(), version, state);
    }

    @Override
    public void restoreSnapshot(Snapshot snapshot) {
        Object state = snapshot.state().get("state");
        if (state instanceof CpuState cpuState) {
            loadState(cpuState);
            return;
        }
        Snapshottable.super.restoreSnapshot(snapshot);
    }

    public void setCycleCallback(Runnable cycleCallback) {
        this.cycleCallback = cycleCallback == null ? () -> {
        } : cycleCallback;
    }

    public void setSpeedRate(int speedRate) {
        if (speedRate != 1 && speedRate != 2) {
            throw new IllegalArgumentException("Speed rate must be 1 or 2");
        }
        this.speedRate = speedRate;
    }

    public int getSpeedRate() {
        return speedRate;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc & 0xFFFF; // Ensure PC wraps around
    }

    public int getSp() {
        return sp;
    }

    public void setSp(int sp) {
        this.sp = sp & 0xFFFF; // Ensure SP wraps around
    }

    public byte getA() {
        return a;
    }

    public void setA(byte a) {
        this.a = a;
    }

    public byte getB() {
        return b;
    }

    public void setB(byte b) {
        this.b = b;
    }

    public byte getC() {
        return c;
    }

    public void setC(byte c) {
        this.c = c;
    }

    public byte getD() {
        return d;
    }

    public void setD(byte d) {
        this.d = d;
    }

    public byte getE() {
        return e;
    }

    public void setE(byte e) {
        this.e = e;
    }

    public byte getH() {
        return h;
    }

    public void setH(byte h) {
        this.h = h;
    }

    public byte getL() {
        return l;
    }

    public void setL(byte l) {
        this.l = l;
    }

    public boolean isZeroFlag() {
        return zeroFlag;
    }

    public void setZeroFlag(boolean zeroFlag) {
        this.zeroFlag = zeroFlag;
    }

    public boolean isNegativeFlag() {
        return negativeFlag;
    }

    public void setNegativeFlag(boolean negativeFlag) {
        this.negativeFlag = negativeFlag;
    }

    public boolean isHalfCarryFlag() {
        return halfCarryFlag;
    }

    public void setHalfCarryFlag(boolean halfCarryFlag) {
        this.halfCarryFlag = halfCarryFlag;
    }

    public boolean isCarryFlag() {
        return carryFlag;
    }

    public void setCarryFlag(boolean carryFlag) {
        this.carryFlag = carryFlag;
    }

    public boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean halted) {
        this.halted = halted;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public boolean isIme() {
        return ime;
    }

    public void setIme(boolean ime) {
        this.ime = ime;
        imeEnableDelay = 0;
    }

    public Bus getBus() {
        return bus;
    }

    @Override
    public void tick() {
        Optional<Interrupt> pendingInterrupt = bus.getPendingInterrupt();
        if (halted) {
            if (pendingInterrupt.isPresent()) {
                halted = false;
            } else {
                waitTicks(1);
                return;
            }
        }
        if (pendingInterrupt.isPresent() && ime) {
            handleInterrupt(pendingInterrupt.get());
            return;
        }
        if (stopped) {
            waitTicks(1);
            return;
        }

        int interruptEnableDelayAtInstructionStart = imeEnableDelay;
        instructionTicks = 0;
        int opcode = readByte(pc);

        if (opcode == 0xCB) {
            opcode = 0xCB00 | readByte(pc + 1);
            pc++;
        }

        Optional<Instruction> instruction = decoder.decode(opcode);
        if (instruction.isEmpty()) {
            logger.error(Integer.toHexString(pc) + " Invalid opcode: " + Integer.toHexString(opcode));
            throw new IllegalStateException("Invalid opcode: " + Integer.toHexString(opcode));
        }

        if (haltBug) {
            pc--;
            pc &= 0xFFFF;
            haltBug = false;
        }
        int totalTicks = instruction.get().execute(this) / speedRate;
        waitTicks(Math.max(totalTicks - instructionTicks, 0));

        updateImeDelay(interruptEnableDelayAtInstructionStart);
    }

    private void handleInterrupt(Interrupt interrupt) {
        instructionTicks = 0;
        bus.clearInterrupt(interrupt);
        waitTicks(8 / speedRate);
        instructionTicks += 8 / speedRate;
        pushStack(pc);
        pc = interrupt.getVectorAddress();
        waitTicks(Math.max((INTERRUPT_TICKS / speedRate) - instructionTicks, 0));
        ime = false;
    }

    public int readByte(int address) {
        waitTicks(BUS_CYCLE_TICKS / speedRate);
        instructionTicks += BUS_CYCLE_TICKS / speedRate;
        return bus.read(address) & 0xFF;
    }

    public int readWord(int address) {
        int lowByte = readByte(address);
        int highByte = readByte(address + 1);
        return ((highByte << 8) | lowByte) & 0xFFFF;
    }

    public void writeByte(int address, int value) {
        waitTicks(BUS_CYCLE_TICKS / speedRate);
        instructionTicks += BUS_CYCLE_TICKS / speedRate;
        bus.write(address, (byte) value);
    }

    public void writeWord(int address, int value) {
        value &= 0xFFFF;
        writeByte(address, value & 0xFF);
        writeByte(address + 1, (value >> 8) & 0xFF);
    }

    private void waitTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            cycleCallback.run();
        }
    }

    public void incrementProgramCounter(int i) {
        pc += i;
        pc &= 0xFFFF; // Ensure PC wraps around
    }

    public int getBc() {
        return ((b & 0xFF) << 8) | (c & 0xFF);
    }

    public void setBc(int value) {
        b = (byte) ((value >> 8) & 0xFF);
        c = (byte) (value & 0xFF);
    }

    public int getDe() {
        return ((d & 0xFF) << 8) | (e & 0xFF);
    }

    public void setDe(int value) {
        d = (byte) ((value >> 8) & 0xFF);
        e = (byte) (value & 0xFF);
    }

    public int getHl() {
        return ((h & 0xFF) << 8) | (l & 0xFF);
    }

    public void setHl(int value) {
        h = (byte) ((value >> 8) & 0xFF);
        l = (byte) (value & 0xFF);
    }

    public int getAf() {
        return ((a & 0xFF) << 8) | (zeroFlag ? 0x80 : 0) | (negativeFlag ? 0x40 : 0) | (halfCarryFlag ? 0x20 : 0) | (carryFlag ? 0x10 : 0);
    }

    public void setAf(int value) {
        a = (byte) ((value >> 8) & 0xFF);
        zeroFlag = (value & 0x80) != 0;
        negativeFlag = (value & 0x40) != 0;
        halfCarryFlag = (value & 0x20) != 0;
        carryFlag = (value & 0x10) != 0;
    }

    public void setHaltBug(boolean haltBug) {
        this.haltBug = haltBug;
    }

    public void enableInterruptsAfterNextInstruction() {
        imeEnableDelay = 1;
    }

    private void updateImeDelay(int interruptEnableDelayAtInstructionStart) {
        if (interruptEnableDelayAtInstructionStart <= 0 || imeEnableDelay <= 0) {
            return;
        }
        imeEnableDelay--;
        if (imeEnableDelay == 0) {
            ime = true;
            if (haltBug) {
                pc--;
                pc &= 0xFFFF;
                haltBug = false;
            }
        }
    }

    public int popStack() {
        int value = readWord(sp);
        sp += 2;
        sp &= 0xFFFF; // Ensure SP wraps around
        return value;
    }

    public void pushStack(int value) {
        sp -= 2;
        writeWord(sp, value);
    }

    public CpuSnapshot snapshot() {
        return new CpuSnapshot(
                pc, sp,
                a & 0xFF, b & 0xFF, c & 0xFF, d & 0xFF, e & 0xFF, h & 0xFF, l & 0xFF,
                getAf(), getBc(), getDe(), getHl(),
                zeroFlag, negativeFlag, halfCarryFlag, carryFlag,
                halted, stopped, ime, imeEnableDelay, haltBug, speedRate
        );
    }

    @Override
    public String toString() {
        return String.format("PC: %04X SP: %04X AF: %04X BC: %04X DE: %04X HL: %04X",
                pc, sp, getAf(), getBc(), getDe(), getHl());
    }

    public record CpuSnapshot(
            int pc,
            int sp,
            int a,
            int b,
            int c,
            int d,
            int e,
            int h,
            int l,
            int af,
            int bc,
            int de,
            int hl,
            boolean zeroFlag,
            boolean negativeFlag,
            boolean halfCarryFlag,
            boolean carryFlag,
            boolean halted,
            boolean stopped,
            boolean ime,
            int imeEnableDelay,
            boolean haltBug,
            int speedRate
    ) {
    }
}
