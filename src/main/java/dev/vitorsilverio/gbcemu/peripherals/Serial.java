package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Serial implements MemorySpace {

    private final Logger logger = LoggerFactory.getLogger(Serial.class);

    private final int SB_REGISTER = 0xFF01;
    private final int SC_REGISTER = 0xFF02;
    private final List<Integer> registers = List.of(SB_REGISTER, SC_REGISTER);
    private final List<Byte> data = new ArrayList<>();


    @Override
    public boolean contains(int address) {
        return registers.contains(address);
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        if (address == SB_REGISTER) {
            data.add(value);
        } else if (address == SC_REGISTER) {
            if ((value & 0x80) != 0) {
                // Start transmission
                //Print the data to the console
                logger.info("Serial data: " + Arrays.toString(data.toArray()));
                System.exit(0);
            }
        }

    }
}
