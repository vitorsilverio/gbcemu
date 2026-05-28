package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.memory.MemoryAccessListener;

import java.util.List;

public interface DebuggerInterface {
    void addPcBreakpoint(int pc);

    void removePcBreakpoint(int pc);

    void togglePcBreakpoint(int pc);

    boolean hasPcBreakpoint(int pc);

    List<Integer> pcBreakpoints();

    void addWatchpoint(AccessType accessType, int address, Integer value);

    void clearWatchpoints();

    List<Watchpoint> watchpoints();

    boolean hasWatchpoints();

    MemoryAccessListener memoryAccessListener();

    void setWatchpointsChangedListener(Runnable watchpointsChanged);

    void requestPause(String reason);

    boolean consumePauseRequest();

    boolean shouldBreakAtPc(int pc);

    boolean shouldBreakOnMemoryAccess();

    void requestInstructionStep();

    boolean consumeInstructionStep();

    void requestFrameStep();

    boolean consumeFrameStep();

    void requestScanlineStep();

    boolean consumeScanlineStep();

    void requestRunUntilHBlank();

    boolean consumeRunUntilHBlank();

    void requestRunUntilVBlank();

    boolean consumeRunUntilVBlank();

    void ignorePcBreakpointOnce(int pc);

    String breakReason();

    enum AccessType {
        READ("Read"),
        WRITE("Write");

        private final String label;

        AccessType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    record Watchpoint(AccessType accessType, int address, Integer value) {
        public boolean matches(AccessType candidateAccessType, int candidateAddress, int candidateValue) {
            return accessType == candidateAccessType
                    && address == (candidateAddress & 0xFFFF)
                    && (value == null || value == (candidateValue & 0xFF));
        }

        public String description() {
            if (value == null) {
                return String.format("%s %04X", accessType.label(), address);
            }
            return String.format("%s %04X == %02X", accessType.label(), address, value);
        }
    }
}
