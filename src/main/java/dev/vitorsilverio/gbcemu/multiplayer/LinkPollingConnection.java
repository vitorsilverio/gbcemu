package dev.vitorsilverio.gbcemu.multiplayer;

import dev.vitorsilverio.gbcemu.connection.PhysicalConnection;

public interface LinkPollingConnection extends PhysicalConnection {

    void setLinkPollMode(LinkPollMode mode);
}
