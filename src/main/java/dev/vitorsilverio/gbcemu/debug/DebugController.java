package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.memory.MemoryAccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class DebugController implements DebuggerInterface {

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
    private int requestedInstructionSteps;
    private int requestedFrameSteps;
    private int requestedScanlineSteps;
    private int requestedRunUntilHBlank;
    private int requestedRunUntilVBlank;
    private boolean pauseRequested;
    private Runnable watchpointsChanged = () -> {
    };

    @Override
    public synchronized void addPcBreakpoint(int pc) {
        pcBreakpoints.add(pc & 0xFFFF);
    }

    @Override
    public synchronized void removePcBreakpoint(int pc) {
        pcBreakpoints.remove(pc & 0xFFFF);
    }

    @Override
    public synchronized void togglePcBreakpoint(int pc) {
        pc &= 0xFFFF;
        if (pcBreakpoints.contains(pc)) {
            pcBreakpoints.remove(pc);
            return;
        }
        pcBreakpoints.add(pc);
    }

    @Override
    public synchronized boolean hasPcBreakpoint(int pc) {
        return pcBreakpoints.contains(pc & 0xFFFF);
    }

    @Override
    public synchronized List<Integer> pcBreakpoints() {
        return new ArrayList<>(pcBreakpoints);
    }

    @Override
    public synchronized void addWatchpoint(AccessType accessType, int address, Integer value) {
        watchpoints.add(new Watchpoint(accessType, address & 0xFFFF, value == null ? null : value & 0xFF));
        watchpointsChanged.run();
    }

    @Override
    public synchronized void clearWatchpoints() {
        watchpoints.clear();
        memoryBreakPending = false;
        watchpointsChanged.run();
    }

    @Override
    public synchronized List<Watchpoint> watchpoints() {
        return new ArrayList<>(watchpoints);
    }

    @Override
    public synchronized boolean hasWatchpoints() {
        return !watchpoints.isEmpty();
    }

    @Override
    public MemoryAccessListener memoryAccessListener() {
        return memoryAccessListener;
    }

    @Override
    public synchronized void setWatchpointsChangedListener(Runnable watchpointsChanged) {
        this.watchpointsChanged = watchpointsChanged == null ? () -> {
        } : watchpointsChanged;
    }

    @Override
    public synchronized void requestPause(String reason) {
        breakReason = reason == null || reason.trim().isEmpty() ? "Manual debug pause" : reason;
        pauseRequested = true;
    }

    @Override
    public synchronized boolean consumePauseRequest() {
        if (!pauseRequested) {
            return false;
        }
        pauseRequested = false;
        return true;
    }

    @Override
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

    @Override
    public synchronized boolean shouldBreakOnMemoryAccess() {
        if (!memoryBreakPending) {
            return false;
        }
        memoryBreakPending = false;
        return true;
    }

    @Override
    public synchronized void requestInstructionStep() {
        requestedInstructionSteps++;
        breakReason = "Step instruction";
    }

    @Override
    public synchronized boolean consumeInstructionStep() {
        if (requestedInstructionSteps <= 0) {
            return false;
        }
        requestedInstructionSteps--;
        return true;
    }

    @Override
    public synchronized void requestFrameStep() {
        requestedFrameSteps++;
        breakReason = "Step frame";
    }

    @Override
    public synchronized boolean consumeFrameStep() {
        if (requestedFrameSteps <= 0) {
            return false;
        }
        requestedFrameSteps--;
        return true;
    }

    @Override
    public synchronized void requestScanlineStep() {
        requestedScanlineSteps++;
        breakReason = "Step scanline";
    }

    @Override
    public synchronized boolean consumeScanlineStep() {
        if (requestedScanlineSteps <= 0) {
            return false;
        }
        requestedScanlineSteps--;
        return true;
    }

    @Override
    public synchronized void requestRunUntilHBlank() {
        requestedRunUntilHBlank++;
        breakReason = "Run until HBlank";
    }

    @Override
    public synchronized boolean consumeRunUntilHBlank() {
        if (requestedRunUntilHBlank <= 0) {
            return false;
        }
        requestedRunUntilHBlank--;
        return true;
    }

    @Override
    public synchronized void requestRunUntilVBlank() {
        requestedRunUntilVBlank++;
        breakReason = "Run until VBlank";
    }

    @Override
    public synchronized boolean consumeRunUntilVBlank() {
        if (requestedRunUntilVBlank <= 0) {
            return false;
        }
        requestedRunUntilVBlank--;
        return true;
    }

    @Override
    public synchronized void ignorePcBreakpointOnce(int pc) {
        ignoredBreakpointPc = pc & 0xFFFF;
    }

    @Override
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

}
