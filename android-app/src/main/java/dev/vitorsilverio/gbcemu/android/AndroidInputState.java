package dev.vitorsilverio.gbcemu.android;

import dev.vitorsilverio.gbcemu.core.ConsoleInput;

public class AndroidInputState implements ConsoleInput {
    public static final int BUTTON_RIGHT = 0;
    public static final int BUTTON_LEFT = 1;
    public static final int BUTTON_UP = 2;
    public static final int BUTTON_DOWN = 3;
    public static final int BUTTON_A = 4;
    public static final int BUTTON_B = 5;
    public static final int BUTTON_SELECT = 6;
    public static final int BUTTON_START = 7;

    private final boolean[] pressed = new boolean[8];

    public synchronized void setButtonState(int button, boolean down) {
        if (button < 0 || button >= pressed.length) {
            return;
        }
        pressed[button] = down;
    }

    public synchronized boolean isPressed(int button) {
        return button >= 0 && button < pressed.length && pressed[button];
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

    public synchronized int buttonMask() {
        int mask = 0;
        for (int button = 0; button < pressed.length; button++) {
            if (pressed[button]) {
                mask |= 1 << button;
            }
        }
        return mask;
    }

    public synchronized void clear() {
        for (int button = 0; button < pressed.length; button++) {
            pressed[button] = false;
        }
    }
}
