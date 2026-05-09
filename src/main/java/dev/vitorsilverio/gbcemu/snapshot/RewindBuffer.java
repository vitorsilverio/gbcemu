package dev.vitorsilverio.gbcemu.snapshot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class RewindBuffer {

    private final int capacity;
    private final Deque<SaveStateFile> states = new ArrayDeque<>();

    public RewindBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized void add(SaveStateFile state) {
        if (state == null) {
            return;
        }
        while (states.size() >= capacity) {
            states.removeFirst();
        }
        states.addLast(state);
    }

    public synchronized Optional<SaveStateFile> popLatest() {
        if (states.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(states.removeLast());
    }

    public synchronized void clear() {
        states.clear();
    }

    public synchronized int size() {
        return states.size();
    }

    public int capacity() {
        return capacity;
    }
}
