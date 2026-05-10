package dev.vitorsilverio.gbcemu.multiplayer;

public interface Multiplayer {

    void send(byte data);

    byte read();

    void close();

}
