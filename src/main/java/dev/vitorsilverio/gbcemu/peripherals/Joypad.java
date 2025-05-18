package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.controller.ButtonType;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;

public class Joypad implements MemorySpace {

    private static final int JOYPAD_REG = 0xFF00;

    private final Bus bus;
    private final Controller controller;

    private byte requestedButtons;

    public Joypad(Bus bus, Controller controller) {
        this.bus = bus;
        this.controller = controller;
        this.controller.eventEmitter(this::onButtonPress);
    }

    @Override
    public boolean contains(int address) {
        return JOYPAD_REG == address;
    }

    @Override
    public byte read(int address) {
        if (address != JOYPAD_REG) {
            throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        }
        // Read the current state of the joypad
        // We need to know which buttons are requested (Action or Directional Buttons)
        // and return the corresponding state

        switch (requestedButtons) {
            // No buttons are requested, return 0xFF
            case 3 -> {return (byte) 0x3F;}
            case 2 -> {
                // Only action buttons are requested
                return getActionButtonsState(controller);
            }
            case 1 -> {
                // Only directional buttons are requested
                return getDirectionalButtonsState(controller);
            }
        }
        return (byte) 0x3F; // Default case, no buttons are requested
    }

    private byte getActionButtonsState(Controller controller) {
        int state = 0b0010_1111;
        state &= controller.isButtonA_Pressed() ? 0b1111_1110 : 0b1111_1111;
        state &= controller.isButtonB_Pressed() ? 0b1111_1101 : 0b1111_1111;
        state &= controller.isButtonStart_Pressed() ? 0b1111_1011 : 0b1111_1111;
        state &= controller.isButtonSelect_Pressed() ? 0b1111_0111 : 0b1111_1111;
        return (byte)state;
    }

    private byte getDirectionalButtonsState(Controller controller) {
        int state = 0b0001_1111;
        state &= controller.isButtonUp_Pressed() ? 0b1111_1110 : 0b1111_1111;
        state &= controller.isButtonDown_Pressed() ? 0b1111_1101 : 0b1111_1111;
        state &= controller.isButtonLeft_Pressed() ? 0b1111_1011 : 0b1111_1111;
        state &= controller.isButtonRight_Pressed() ? 0b1111_0111 : 0b1111_1111;
        return (byte)state;
    }

    @Override
    public void write(int address, byte value) {
        if (address == JOYPAD_REG) {
            requestedButtons = (byte) (value >> 4); // The requested buttons are in the upper nibble
        } else {
            throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        }
    }

    private void onButtonPress(ButtonType buttonType) {
        // Notify the bus that the joypad state has changed
        if(ButtonType.ACTION.equals(buttonType) && requestedButtons == 2) {
            bus.requestInterrupt(Interrupt.JOYPAD);
        }
        if(ButtonType.DIRECTIONAL.equals(buttonType) && requestedButtons == 1) {
            bus.requestInterrupt(Interrupt.JOYPAD);
        }
    }
}
