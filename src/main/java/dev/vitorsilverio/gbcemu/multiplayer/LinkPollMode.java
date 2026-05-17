package dev.vitorsilverio.gbcemu.multiplayer;

public enum LinkPollMode {
    IDLE(4096),
    CONNECTED(1024),
    TRANSFER(64);

    private final int interval;

    LinkPollMode(int interval) {
        this.interval = interval;
    }

    public int interval() {
        return interval;
    }
}
