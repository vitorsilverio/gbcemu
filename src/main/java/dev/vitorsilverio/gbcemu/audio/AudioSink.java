package dev.vitorsilverio.gbcemu.audio;

interface AudioSink {
    void write(byte[] buffer, int length);
}
