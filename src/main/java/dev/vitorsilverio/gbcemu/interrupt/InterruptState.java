package dev.vitorsilverio.gbcemu.interrupt;

import java.io.Serializable;

public record InterruptState(byte ieReg, byte ifReg) implements Serializable {
}
