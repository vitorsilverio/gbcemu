package dev.vitorsilverio.gbcemu.controller;

import dev.vitorsilverio.gbcemu.AppSettings;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

public class KeyboardController implements Controller, KeyListener {

    private Consumer<ButtonType> onButtonPress;
    private int[] keyCodes;
    private boolean buttonA_Pressed = false;
    private boolean buttonB_Pressed = false;
    private boolean buttonStart_Pressed = false;
    private boolean buttonSelect_Pressed = false;
    private boolean buttonUp_Pressed = false;
    private boolean buttonDown_Pressed = false;
    private boolean buttonLeft_Pressed = false;
    private boolean buttonRight_Pressed = false;

    public KeyboardController(AppSettings settings) {
        applySettings(settings);
    }

    public void applySettings(AppSettings settings) {
        int[] updated = new int[AppSettings.CONTROLLER_BUTTON_NAMES.length];
        for (int i = 0; i < updated.length; i++) {
            updated[i] = settings.controllerKeyCode(i);
        }
        keyCodes = updated;
        releaseAll();
    }

    @Override
    public boolean isButtonA_Pressed() {
        return buttonA_Pressed;
    }

    @Override
    public boolean isButtonB_Pressed() {
        return buttonB_Pressed;
    }

    @Override
    public boolean isButtonStart_Pressed() {
        return buttonStart_Pressed;
    }

    @Override
    public boolean isButtonSelect_Pressed() {
        return buttonSelect_Pressed;
    }

    @Override
    public boolean isButtonUp_Pressed() {
        return buttonUp_Pressed;
    }

    @Override
    public boolean isButtonDown_Pressed() {
        return buttonDown_Pressed;
    }

    @Override
    public boolean isButtonLeft_Pressed() {
        return buttonLeft_Pressed;
    }

    @Override
    public boolean isButtonRight_Pressed() {
        return buttonRight_Pressed;
    }

    @Override
    public void eventEmitter(Consumer<ButtonType> onButtonPress) {
        this.onButtonPress = onButtonPress;
    }
    

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == keyCodes[0]) {
                buttonA_Pressed = true;
                emit(ButtonType.ACTION);
        } else if (keyCode == keyCodes[1]) {
                buttonB_Pressed = true;
                emit(ButtonType.ACTION);
        } else if (keyCode == keyCodes[2]) {
                buttonStart_Pressed = true;
                emit(ButtonType.ACTION);
        } else if (keyCode == keyCodes[3]) {
                buttonSelect_Pressed = true;
                emit(ButtonType.ACTION);
        } else if (keyCode == keyCodes[4]) {
                buttonUp_Pressed = true;
                emit(ButtonType.DIRECTIONAL);
        } else if (keyCode == keyCodes[5]) {
                buttonDown_Pressed = true;
                emit(ButtonType.DIRECTIONAL);
        } else if (keyCode == keyCodes[6]) {
                buttonLeft_Pressed = true;
                emit(ButtonType.DIRECTIONAL);
        } else if (keyCode == keyCodes[7]) {
                buttonRight_Pressed = true;
                emit(ButtonType.DIRECTIONAL);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == keyCodes[0]) {
            buttonA_Pressed = false;
        } else if (keyCode == keyCodes[1]) {
            buttonB_Pressed = false;
        } else if (keyCode == keyCodes[2]) {
            buttonStart_Pressed = false;
        } else if (keyCode == keyCodes[3]) {
            buttonSelect_Pressed = false;
        } else if (keyCode == keyCodes[4]) {
            buttonUp_Pressed = false;
        } else if (keyCode == keyCodes[5]) {
            buttonDown_Pressed = false;
        } else if (keyCode == keyCodes[6]) {
            buttonLeft_Pressed = false;
        } else if (keyCode == keyCodes[7]) {
            buttonRight_Pressed = false;
        }
    }

    private void releaseAll() {
        buttonA_Pressed = false;
        buttonB_Pressed = false;
        buttonStart_Pressed = false;
        buttonSelect_Pressed = false;
        buttonUp_Pressed = false;
        buttonDown_Pressed = false;
        buttonLeft_Pressed = false;
        buttonRight_Pressed = false;
    }

    private void emit(ButtonType buttonType) {
        if (onButtonPress != null) {
            onButtonPress.accept(buttonType);
        }
    }
}
