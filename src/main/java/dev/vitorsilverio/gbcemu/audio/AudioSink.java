package dev.vitorsilverio.gbcemu.audio;

interface AudioSink {
    void write(byte[] buffer, int length);

    default void close() {
    }

    default String debugDescription() {
        return "Muted";
    }
}
