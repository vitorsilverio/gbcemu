package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.memory.MemoryAccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class DebugController {

    private final TreeSet<Integer> pcBreakpoints = new TreeSet<>();
    private final List<Watchpoint> watchpoints = new ArrayList<>();
    private final MemoryAccessListener memoryAccessListener = new MemoryAccessListener() {
        @Override
        public void onRead(int address, byte value) {
            recordMemoryAccess(AccessType.READ, address, value);
        }

        @Override
        public void onWrite(int address, byte value) {
            recordMemoryAccess(AccessType.WRITE, address, value);
        }
    };
    private int ignoredBreakpointPc = -1;
    private String breakReason = "";
    private boolean memoryBreakPending;
    private Runnable watchpointsChanged = () -> {
    };

    public synchronized void addPcBreakpoint(int pc) {
        pcBreakpoints.add(pc & 0xFFFF);
    }

    public synchronized void removePcBreakpoint(int pc) {
        pcBreakpoints.remove(pc & 0xFFFF);
    }

    public synchronized void togglePcBreakpoint(int pc) {
        pc &= 0xFFFF;
        if (pcBreakpoints.contains(pc)) {
            pcBreakpoints.remove(pc);
            return;
        }
        pcBreakpoints.add(pc);
    }

    public synchronized boolean hasPcBreakpoint(int pc) {
        return pcBreakpoints.contains(pc & 0xFFFF);
    }

    public synchronized List<Integer> pcBreakpoints() {
        return new ArrayList<>(pcBreakpoints);
    }

    public synchronized void addWatchpoint(AccessType accessType, int address, Integer value) {
        watchpoints.add(new Watchpoint(accessType, address & 0xFFFF, value == null ? null : value & 0xFF));
        watchpointsChanged.run();
    }

    public synchronized void clearWatchpoints() {
        watchpoints.clear();
        memoryBreakPending = false;
        watchpointsChanged.run();
    }

    public synchronized List<Watchpoint> watchpoints() {
        return new ArrayList<>(watchpoints);
    }

    public synchronized boolean hasWatchpoints() {
        return !watchpoints.isEmpty();
    }

    public MemoryAccessListener memoryAccessListener() {
        return memoryAccessListener;
    }

    public synchronized void setWatchpointsChangedListener(Runnable watchpointsChanged) {
        this.watchpointsChanged = watchpointsChanged == null ? () -> {
        } : watchpointsChanged;
    }

    public synchronized boolean shouldBreakAtPc(int pc) {
        pc &= 0xFFFF;
        if (pc == ignoredBreakpointPc) {
            ignoredBreakpointPc = -1;
            return false;
        }
        if (!pcBreakpoints.contains(pc)) {
            return false;
        }
        breakReason = String.format("PC breakpoint hit at %04X", pc);
        return true;
    }

    public synchronized boolean shouldBreakOnMemoryAccess() {
        if (!memoryBreakPending) {
            return false;
        }
        memoryBreakPending = false;
        return true;
    }

    public synchronized void ignorePcBreakpointOnce(int pc) {
        ignoredBreakpointPc = pc & 0xFFFF;
    }

    public synchronized String breakReason() {
        return breakReason;
    }

    private synchronized void recordMemoryAccess(AccessType accessType, int address, byte value) {
        if (memoryBreakPending) {
            return;
        }
        int normalizedAddress = address & 0xFFFF;
        int normalizedValue = value & 0xFF;
        for (Watchpoint watchpoint : watchpoints) {
            if (!watchpoint.matches(accessType, normalizedAddress, normalizedValue)) {
                continue;
            }
            breakReason = String.format(
                    "%s watchpoint hit at %04X value=%02X",
                    accessType.label(),
                    normalizedAddress,
                    normalizedValue
            );
            memoryBreakPending = true;
            return;
        }
    }

    public enum AccessType {
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

    public record Watchpoint(AccessType accessType, int address, Integer value) {
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
