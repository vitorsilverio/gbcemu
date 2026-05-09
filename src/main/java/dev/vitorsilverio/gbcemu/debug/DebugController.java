package dev.vitorsilverio.gbcemu.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class DebugController {

    private final TreeSet<Integer> pcBreakpoints = new TreeSet<>();
    private int ignoredBreakpointPc = -1;
    private String breakReason = "";

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

    public synchronized void ignorePcBreakpointOnce(int pc) {
        ignoredBreakpointPc = pc & 0xFFFF;
    }

    public synchronized String breakReason() {
        return breakReason;
    }
}
