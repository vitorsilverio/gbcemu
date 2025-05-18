package dev.vitorsilverio.gbcemu.cpu;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.slf4j.Logger;

import java.util.Optional;

public class Cpu implements MachineCycle {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Cpu.class);

    private final Bus bus;
    private final Decoder decoder;

    private int cycles = 0;
    private int speedRate = 1;

    private int pc = 0x0000; // Program Counter
    private int sp = 0xFFFE; // Stack Pointer
    private byte a = 0; // Accumulator
    private byte b = 0; // Register B
    private byte c = 0; // Register C
    private byte d = 0; // Register D
    private byte e = 0; // Register E
    private byte h = 0; // Register H
    private byte l = 0; // Register L

    private boolean zeroFlag = false; // Zero Flag
    private boolean negativeFlag = false; // Subtract Flag
    private boolean halfCarryFlag = false; // Half Carry Flag
    private boolean carryFlag = false; // Carry Flag

    private boolean halted = false;
    private boolean stopped = false;
    private boolean ime = false; // Interrupt Master Enable
    private boolean haltBug = false; // Halt Bug


    public Cpu(Bus bus) {
        this.bus = bus;
        this.decoder = new Decoder();
    }

    public void setSpeedRate(int speedRate) {
        this.speedRate = speedRate;
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
    }

    public Bus getBus() {
        return bus;
    }

    @Override
    public void tick() {
        if (cycles > 0) {
            cycles--;
            return;
        }
        Optional<Interrupt> pendingInterrupt = bus.getPendingInterrupt();
        if (halted) {
            if ( pendingInterrupt.isPresent() ) {
                halted = false;
            } else {
                // Continue halted state
                return;
            }
        }
        if ( pendingInterrupt.isPresent() && ime ) {
            // Handle the interrupt
            handleInterrupt(pendingInterrupt.get());
        }
        if (stopped) {
            // Handle stopped state
            return;
        }

        // Fetch the next instruction
        int opcode = bus.read(pc) & 0xFF;

        if (opcode == 0xcb) {
            // Handle CB-prefixed instructions
            opcode = (0xcb00 | (bus.read(pc + 1) & 0xFF));
            pc++;
        }

        logger.debug("CPU State: " + this);

        if (pc == 0x0606){
            logger.debug("PC: " + Integer.toHexString(pc));
            logger.debug("Opcode: " + Integer.toHexString(peekStack()));

        }

        // Decode and execute the instruction
        Optional<Instruction> instruction = decoder.decode(opcode);
        if (instruction.isEmpty()) {
            logger.error(Integer.toHexString(pc) + " Invalid opcode: " + Integer.toHexString(opcode));
            throw new IllegalStateException("Invalid opcode: " + Integer.toHexString(opcode));
        }

        cycles = instruction.get().execute(this) * speedRate;

        if (haltBug && opcode != 0x76) {
            pc --;
            haltBug = false;
        }


    }

    private void handleInterrupt(Interrupt interrupt) {
        // Clear the interrupt flag
        bus.clearInterrupt(interrupt);

        // Push the current program counter to the stack
        pushStack(pc);

        // Set the program counter to the interrupt vector address
        pc = interrupt.getVectorAddress();

        // set cycles to 20
        cycles = 20 * speedRate;

        // Disable interrupts
        ime = false;
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

    public int popStack() {
        int value = bus.readWord(sp);
        sp += 2;
        sp &= 0xFFFF; // Ensure SP wraps around
        if (value == 0x00f9) {
            logger.debug("Popped value: " + Integer.toHexString(value));
        }
        return value;
    }

    public int peekStack() {
        return bus.readWord(sp);
    }

    public void pushStack(int value) {
        sp -= 2;
        if ((value & 0xffff) == 0x00f9) {
            logger.debug("Pushed value: " + Integer.toHexString(value));
        }
        bus.writeWord(sp, value);
    }

    @Override
    public String toString() {
        return String.format("PC: %04X SP: %04X AF: %04X BC: %04X DE: %04X HL: %04X",
                pc, sp, getAf(), getBc(), getDe(), getHl());
    }
}
