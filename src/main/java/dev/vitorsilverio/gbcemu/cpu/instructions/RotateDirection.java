package dev.vitorsilverio.gbcemu.cpu.instructions;

public enum RotateDirection {
    LEFT("L"),
    RIGHT("R");

    private final String direction;

    RotateDirection(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }
}
