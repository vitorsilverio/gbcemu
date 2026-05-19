package dev.vitorsilverio.gbcemu.multiplayer;

public enum LinkPollMode {
    IDLE(2048),
    CONNECTED(128),
    TRANSFER(8);

    private final int interval;

    LinkPollMode(int interval) {
        this.interval = interval;
    }

    public int interval() {
        return interval;
    }
}
