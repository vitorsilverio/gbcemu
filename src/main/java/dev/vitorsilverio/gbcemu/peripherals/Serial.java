package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class Serial implements MemorySpace {

    private final Logger logger = LoggerFactory.getLogger(Serial.class);

    private final int SB_REGISTER = 0xFF01;
    private final int SC_REGISTER = 0xFF02;
    private final List<Integer> registers = List.of(SB_REGISTER, SC_REGISTER);
    private final List<Byte> data = new ArrayList<>();
    private final Bus bus;
    private final StringBuilder text = new StringBuilder();
    private int SB = 0;
    private int SC = 0;

    public Serial(Bus bus) {
        this.bus = bus;
    }


    @Override
    public boolean contains(int address) {
        return registers.contains(address);
    }

    @Override
    public byte read(int address) {
        return (byte) switch (address) {
            case (SB_REGISTER) -> SB;
            case (SC_REGISTER) -> SC;
            default -> 0;
        };
    }

    @Override
    public void write(int address, byte value) {
        if (address == SB_REGISTER) {
            SB = value;
        } else if (address == SC_REGISTER) {
            SC= value & 0b1000_0011;
            if ((value & 0x80) != 0) {
                data.add((byte) SB);
                byte[] primitiveData = new byte[]{(byte) SB};
                logger.info("Serial data: " + HexFormat.of().formatHex(primitiveData));
                appendText((byte) SB);
                SC &= 0x7F;
                bus.requestInterrupt(Interrupt.SERIAL);
                data.clear();
            }
        }

    }

    private void appendText(byte value) {
        int unsignedValue = value & 0xFF;
        if (unsignedValue == '\n') {
            logger.info("Serial text: {}", text);
            text.setLength(0);
        } else if (unsignedValue >= 0x20 && unsignedValue <= 0x7E) {
            text.append((char) unsignedValue);
        }
    }
}
