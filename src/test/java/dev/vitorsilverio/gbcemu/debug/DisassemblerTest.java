package dev.vitorsilverio.gbcemu.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisassemblerTest {

    @Test
    void decodesImmediateInstructionLengths() {
        byte[] memory = {
                (byte) 0x21, 0x34, 0x12,
                (byte) 0xCB, 0x7C,
                (byte) 0xE0, 0x50,
                0x00
        };

        Disassembler.Decoded first = Disassembler.decode(0, address -> memory[address] & 0xFF);
        Disassembler.Decoded second = Disassembler.decode(3, address -> memory[address] & 0xFF);
        Disassembler.Decoded third = Disassembler.decode(5, address -> memory[address] & 0xFF);

        assertEquals(3, first.length());
        assertTrue(first.text().contains("LD HL,$1234"));
        assertEquals(2, second.length());
        assertTrue(second.text().contains("BIT 7,H"));
        assertEquals(2, third.length());
        assertTrue(third.text().contains("LDH ($FF50),A"));
    }

    @Test
    void decodesRegularOpcodeFamilies() {
        assertTrue(Disassembler.decode(0, address -> 0x78).text().contains("LD A,B"));
        assertTrue(Disassembler.decode(0, address -> 0x86).text().contains("ADD A,(HL)"));
        assertTrue(Disassembler.decode(0, address -> 0xC7).text().contains("RST $00"));
    }
}
