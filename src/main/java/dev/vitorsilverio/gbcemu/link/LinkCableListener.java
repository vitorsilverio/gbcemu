package dev.vitorsilverio.gbcemu.link;

import java.util.OptionalInt;

public interface LinkCableListener {
    OptionalInt onExternalClockedByte(int value);

    void onInternalClockResult(int value);

    default void onLinkDisconnected() {
    }
}
