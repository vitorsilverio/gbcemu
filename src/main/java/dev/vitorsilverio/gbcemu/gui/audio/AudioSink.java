package dev.vitorsilverio.gbcemu.gui.audio;

interface AudioSink {
    void write(byte[] buffer, int length);

    default void close() {
    }

    default String debugDescription() {
        return "Muted";
    }
}
