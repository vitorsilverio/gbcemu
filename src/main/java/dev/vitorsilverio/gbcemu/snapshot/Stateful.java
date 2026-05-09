package dev.vitorsilverio.gbcemu.snapshot;

import java.io.Serializable;

public interface Stateful<S extends Serializable> {
    S saveState();

    void loadState(S state);
}
