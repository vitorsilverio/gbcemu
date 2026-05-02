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
            data.add(value);
        } else if (address == SC_REGISTER) {
            if ((value & 0x80) != 0) {
                // Start transmission
                //Print the data to the console
                if ((SC & 0x80) != 0 && ((value & 0x80) ==0)) { // Transfer complete
                    logger.info("Serial transfer data complete");
                    bus.requestInterrupt(Interrupt.SERIAL);
                }
                SC= value & 0b1000_0011;
                byte[] primitiveData = new byte[data.size()];
                for (int i = 0; i < primitiveData.length; i++) {
                    primitiveData[i] = data.get(i);
                }
                logger.info("Serial data: " + HexFormat.of().formatHex(primitiveData));
                data.clear();
            }
        }

    }
}
