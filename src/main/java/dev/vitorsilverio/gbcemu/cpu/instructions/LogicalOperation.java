package dev.vitorsilverio.gbcemu.cpu.instructions;

public enum LogicalOperation {
    AND,
    OR,
    XOR;

    public byte apply(byte a, int b) {
        switch (this) {
            case AND:
                return (byte) (a & b);
            case OR:
                return (byte) (a | b);
            case XOR:
                return (byte) (a ^ b);
            default:
                throw new UnsupportedOperationException("Unsupported logical operation: " + this);
        }
    }
}
