package dev.vitorsilverio.gbcemu.android;

import dev.vitorsilverio.gbcemu.core.ConsoleInput;
import dev.vitorsilverio.gbcemu.controller.ButtonMaskProvider;

public class AndroidInputState implements ConsoleInput, ButtonMaskProvider {
    public static final int BUTTON_RIGHT = 0;
    public static final int BUTTON_LEFT = 1;
    public static final int BUTTON_UP = 2;
    public static final int BUTTON_DOWN = 3;
    public static final int BUTTON_A = 4;
    public static final int BUTTON_B = 5;
    public static final int BUTTON_SELECT = 6;
    public static final int BUTTON_START = 7;

    private volatile int pressedMask;

    public void setButtonState(int button, boolean down) {
        if (button < 0 || button > BUTTON_START) {
            return;
        }
        int bit = 1 << button;
        int mask = pressedMask;
        pressedMask = down ? mask | bit : mask & ~bit;
    }

    public boolean isPressed(int button) {
        return button >= 0 && button <= BUTTON_START && (pressedMask & (1 << button)) != 0;
    }

    @Override
    public boolean isButtonA_Pressed() {
        return isPressed(BUTTON_A);
    }

    @Override
    public boolean isButtonB_Pressed() {
        return isPressed(BUTTON_B);
    }

    @Override
    public boolean isButtonStart_Pressed() {
        return isPressed(BUTTON_START);
    }

    @Override
    public boolean isButtonSelect_Pressed() {
        return isPressed(BUTTON_SELECT);
    }

    @Override
    public boolean isButtonUp_Pressed() {
        return isPressed(BUTTON_UP);
    }

    @Override
    public boolean isButtonDown_Pressed() {
        return isPressed(BUTTON_DOWN);
    }

    @Override
    public boolean isButtonLeft_Pressed() {
        return isPressed(BUTTON_LEFT);
    }

    @Override
    public boolean isButtonRight_Pressed() {
        return isPressed(BUTTON_RIGHT);
    }

    public int buttonMask() {
        return pressedButtonMask();
    }

    @Override
    public int pressedButtonMask() {
        return pressedMask;
    }

    public void clear() {
        pressedMask = 0;
    }
}
