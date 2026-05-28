package dev.vitorsilverio.gbcemu.debug;

public class Disassembler {

    private static final String[] R = {"B", "C", "D", "E", "H", "L", "(HL)", "A"};
    private static final String[] RP = {"BC", "DE", "HL", "SP"};
    private static final String[] RP2 = {"BC", "DE", "HL", "AF"};
    private static final String[] CC = {"NZ", "Z", "NC", "C"};
    private static final String[] ALU = {"ADD A", "ADC A", "SUB", "SBC A", "AND", "XOR", "OR", "CP"};
    private static final String[] ROT = {"RLC", "RRC", "RL", "RR", "SLA", "SRA", "SWAP", "SRL"};

    private Disassembler() {
    }

    public static Decoded decode(int address, MemoryReader memory) {
        int opcode = memory.read(address);
        int b1 = memory.read(address + 1);
        int b2 = memory.read(address + 2);
        if (opcode == 0xCB) {
            return new Decoded(address, 2, String.format("%02X %02X", opcode, b1), decodeCb(b1));
        }
        int length = length(opcode);
        String bytes = switch (length) {
            case 1 -> String.format("%02X", opcode);
            case 2 -> String.format("%02X %02X", opcode, b1);
            case 3 -> String.format("%02X %02X %02X", opcode, b1, b2);
            default -> String.format("%02X", opcode);
        };
        return new Decoded(address, length, bytes, decodeBase(address, opcode, b1, b2));
    }

    public static int length(int opcode) {
        opcode &= 0xFF;
        if (opcode == 0xCB) {
            return 2;
        }
        return switch (opcode) {
            case 0x01, 0x08, 0x11, 0x21, 0x31, 0xC2, 0xC3, 0xC4, 0xCA, 0xCC, 0xCD, 0xD2, 0xD4, 0xDA, 0xDC, 0xEA, 0xFA -> 3;
            case 0x06, 0x0E, 0x10, 0x16, 0x18, 0x1E, 0x20, 0x26, 0x28, 0x2E, 0x30, 0x36, 0x38, 0x3E,
                 0xC6, 0xCE, 0xD6, 0xDE, 0xE0, 0xE6, 0xE8, 0xEE, 0xF0, 0xF6, 0xF8, 0xFE -> 2;
            default -> 1;
        };
    }

    private static String decodeBase(int address, int opcode, int b1, int b2) {
        opcode &= 0xFF;
        if (opcode >= 0x40 && opcode <= 0x7F) {
            if (opcode == 0x76) {
                return "HALT";
            }
            return "LD " + R[(opcode >> 3) & 0x07] + "," + R[opcode & 0x07];
        }
        if (opcode >= 0x80 && opcode <= 0xBF) {
            return ALU[(opcode >> 3) & 0x07] + "," + R[opcode & 0x07];
        }
        if ((opcode & 0xC7) == 0x04) {
            return "INC " + R[(opcode >> 3) & 0x07];
        }
        if ((opcode & 0xC7) == 0x05) {
            return "DEC " + R[(opcode >> 3) & 0x07];
        }
        if ((opcode & 0xC7) == 0x06) {
            return String.format("LD %s,$%02X", R[(opcode >> 3) & 0x07], b1);
        }
        if ((opcode & 0xCF) == 0x01) {
            return String.format("LD %s,$%04X", RP[(opcode >> 4) & 0x03], word(b1, b2));
        }
        if ((opcode & 0xCF) == 0x03) {
            return "INC " + RP[(opcode >> 4) & 0x03];
        }
        if ((opcode & 0xCF) == 0x0B) {
            return "DEC " + RP[(opcode >> 4) & 0x03];
        }
        if ((opcode & 0xCF) == 0x09) {
            return "ADD HL," + RP[(opcode >> 4) & 0x03];
        }
        if ((opcode & 0xCF) == 0xC1) {
            return "POP " + RP2[(opcode >> 4) & 0x03];
        }
        if ((opcode & 0xCF) == 0xC5) {
            return "PUSH " + RP2[(opcode >> 4) & 0x03];
        }
        if ((opcode & 0xE7) == 0xC0) {
            return "RET " + CC[(opcode >> 3) & 0x03];
        }
        if ((opcode & 0xE7) == 0xC2) {
            return String.format("JP %s,$%04X", CC[(opcode >> 3) & 0x03], absolute(b1, b2));
        }
        if ((opcode & 0xE7) == 0xC4) {
            return String.format("CALL %s,$%04X", CC[(opcode >> 3) & 0x03], absolute(b1, b2));
        }
        if ((opcode & 0xC7) == 0xC7) {
            return String.format("RST $%04X", opcode & 0x38);
        }
        if ((opcode & 0xE7) == 0x20) {
            return String.format("JR %s,$%04X ; %+d", CC[(opcode >> 3) & 0x03], relativeTarget(address, b1), (byte) b1);
        }

        return switch (opcode) {
            case 0x00 -> "NOP";
            case 0x02 -> "LD (BC),A";
            case 0x07 -> "RLCA";
            case 0x08 -> String.format("LD ($%04X),SP", absolute(b1, b2));
            case 0x0A -> "LD A,(BC)";
            case 0x0F -> "RRCA";
            case 0x10 -> String.format("STOP $%02X", b1);
            case 0x12 -> "LD (DE),A";
            case 0x17 -> "RLA";
            case 0x18 -> String.format("JR $%04X ; %+d", relativeTarget(address, b1), (byte) b1);
            case 0x1A -> "LD A,(DE)";
            case 0x1F -> "RRA";
            case 0x22 -> "LD (HL+),A";
            case 0x27 -> "DAA";
            case 0x2A -> "LD A,(HL+)";
            case 0x2F -> "CPL";
            case 0x32 -> "LD (HL-),A";
            case 0x37 -> "SCF";
            case 0x3A -> "LD A,(HL-)";
            case 0x3F -> "CCF";
            case 0xC3 -> String.format("JP $%04X", absolute(b1, b2));
            case 0xC6 -> String.format("ADD A,$%02X", b1);
            case 0xC9 -> "RET";
            case 0xCD -> String.format("CALL $%04X", absolute(b1, b2));
            case 0xCE -> String.format("ADC A,$%02X", b1);
            case 0xD6 -> String.format("SUB $%02X", b1);
            case 0xD9 -> "RETI";
            case 0xDE -> String.format("SBC A,$%02X", b1);
            case 0xE0 -> String.format("LDH ($FF%02X),A", b1);
            case 0xE2 -> "LD (C),A";
            case 0xE6 -> String.format("AND $%02X", b1);
            case 0xE8 -> String.format("ADD SP,%+d", (byte) b1);
            case 0xE9 -> "JP HL";
            case 0xEA -> String.format("LD ($%04X),A", absolute(b1, b2));
            case 0xEE -> String.format("XOR $%02X", b1);
            case 0xF0 -> String.format("LDH A,($FF%02X)", b1);
            case 0xF2 -> "LD A,(C)";
            case 0xF3 -> "DI";
            case 0xF6 -> String.format("OR $%02X", b1);
            case 0xF8 -> String.format("LD HL,SP%+d", (byte) b1);
            case 0xF9 -> "LD SP,HL";
            case 0xFA -> String.format("LD A,($%04X)", absolute(b1, b2));
            case 0xFB -> "EI";
            case 0xFE -> String.format("CP $%02X", b1);
            case 0xD3, 0xDB, 0xDD, 0xE3, 0xE4, 0xEB, 0xEC, 0xED, 0xF4, 0xFC, 0xFD -> "ILLEGAL";
            default -> String.format("OP %02X", opcode);
        };
    }

    private static String decodeCb(int opcode) {
        opcode &= 0xFF;
        int x = opcode >> 6;
        int y = (opcode >> 3) & 0x07;
        int z = opcode & 0x07;
        return switch (x) {
            case 0 -> ROT[y] + " " + R[z];
            case 1 -> "BIT " + y + "," + R[z];
            case 2 -> "RES " + y + "," + R[z];
            case 3 -> "SET " + y + "," + R[z];
            default -> String.format("CB %02X", opcode);
        };
    }

    private static int word(int low, int high) {
        return (low & 0xFF) | ((high & 0xFF) << 8);
    }

    private static int absolute(int low, int high) {
        return word(low, high) & 0xFFFF;
    }

    private static int relativeTarget(int address, int offset) {
        return (address + 2 + (byte) offset) & 0xFFFF;
    }

    public interface MemoryReader {
        int read(int address);
    }

    public record Decoded(int address, int length, String bytes, String instruction) {

        public String text(){
            return String.format("%s%s", fillRight(this.bytes, 11, ' ' ), this.instruction);
        }

        private String fillRight(String string, int size, char placeHolder) {
            var builder = new StringBuilder(string);
            int padding = Math.max(0, size - string.length() + 1);
            for (int i = 0; i < padding; i++) {
                builder.append(placeHolder);
            }
            return builder.toString();
        }
    }
}
