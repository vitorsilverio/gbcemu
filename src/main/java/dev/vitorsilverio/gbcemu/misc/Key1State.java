package dev.vitorsilverio.gbcemu.misc;

import java.io.Serializable;

public record Key1State(boolean prepareSpeedSwitch, boolean doubleSpeed) implements Serializable {
}
