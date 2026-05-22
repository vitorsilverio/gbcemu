package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.controller.ButtonType;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoy;

public class Joypad implements MemorySpace {

    private static final int JOYPAD_REG = 0xFF00;
    private static final int SELECT_DPAD = 0x10;
    private static final int SELECT_BUTTONS = 0x20;

    private final Bus bus;
    private final Controller controller;
    private final SuperGameBoy superGameBoy;

    private int selectedLines = SELECT_DPAD | SELECT_BUTTONS;

    public Joypad(Bus bus, Controller controller) {
        this(bus, controller, null);
    }

    public Joypad(Bus bus, Controller controller, SuperGameBoy superGameBoy) {
        this.bus = bus;
        this.controller = controller;
        this.superGameBoy = superGameBoy;
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

        int state = 0x0F;
        if (selectedLines == (SELECT_DPAD | SELECT_BUTTONS) && superGameBoy != null) {
            state = superGameBoy.joypadIdNibble();
        }
        if ((selectedLines & SELECT_BUTTONS) == 0) {
            state &= getActionButtonsState(controller);
        }
        if ((selectedLines & SELECT_DPAD) == 0) {
            state &= getDirectionalButtonsState(controller);
        }
        return (byte) (0xC0 | selectedLines | state);
    }

    private int getActionButtonsState(Controller controller) {
        int state = 0x0F;
        state &= controller.isButtonA_Pressed() ? 0b1110 : 0x0F;
        state &= controller.isButtonB_Pressed() ? 0b1101 : 0x0F;
        state &= controller.isButtonSelect_Pressed() ? 0b1011 : 0x0F;
        state &= controller.isButtonStart_Pressed() ? 0b0111 : 0x0F;
        return state;
    }

    private int getDirectionalButtonsState(Controller controller) {
        int state = 0x0F;
        state &= controller.isButtonRight_Pressed() ? 0b1110 : 0x0F;
        state &= controller.isButtonLeft_Pressed() ? 0b1101 : 0x0F;
        state &= controller.isButtonUp_Pressed() ? 0b1011 : 0x0F;
        state &= controller.isButtonDown_Pressed() ? 0b0111 : 0x0F;
        return state;
    }

    @Override
    public void write(int address, byte value) {
        if (address == JOYPAD_REG) {
            selectedLines = value & (SELECT_DPAD | SELECT_BUTTONS);
            if (superGameBoy != null) {
                superGameBoy.writeJoypad(selectedLines);
            }
        } else {
            throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        }
    }

    private void onButtonPress(ButtonType buttonType) {
        // Notify the bus that the joypad state has changed
        if(ButtonType.ACTION.equals(buttonType) && (selectedLines & SELECT_BUTTONS) == 0) {
            bus.requestInterrupt(Interrupt.JOYPAD);
        }
        if(ButtonType.DIRECTIONAL.equals(buttonType) && (selectedLines & SELECT_DPAD) == 0) {
            bus.requestInterrupt(Interrupt.JOYPAD);
        }
    }
}
