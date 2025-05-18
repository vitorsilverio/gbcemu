package dev.vitorsilverio.gbcemu.controller;

import org.slf4j.Logger;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

public class KeyboardController implements Controller, KeyListener {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(KeyboardController.class);

    private Consumer<ButtonType> onButtonPress;
    private boolean buttonA_Pressed = false;
    private boolean buttonB_Pressed = false;
    private boolean buttonStart_Pressed = false;
    private boolean buttonSelect_Pressed = false;
    private boolean buttonUp_Pressed = false;
    private boolean buttonDown_Pressed = false;
    private boolean buttonLeft_Pressed = false;
    private boolean buttonRight_Pressed = false;

    public KeyboardController() {

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
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Z -> {
                buttonA_Pressed = true;
                onButtonPress.accept(ButtonType.ACTION);
            }
            case KeyEvent.VK_X -> {
                buttonB_Pressed = true;
                onButtonPress.accept(ButtonType.ACTION);
            }
            case KeyEvent.VK_ENTER -> {
                buttonStart_Pressed = true;
                onButtonPress.accept(ButtonType.ACTION);
            }
            case KeyEvent.VK_SPACE -> {
                buttonSelect_Pressed = true;
                onButtonPress.accept(ButtonType.ACTION);
            }
            case KeyEvent.VK_UP -> {
                buttonUp_Pressed = true;
                onButtonPress.accept(ButtonType.DIRECTIONAL);
            }
            case KeyEvent.VK_DOWN -> {
                buttonDown_Pressed = true;
                onButtonPress.accept(ButtonType.DIRECTIONAL);
            }
            case KeyEvent.VK_LEFT -> {
                buttonLeft_Pressed = true;
                onButtonPress.accept(ButtonType.DIRECTIONAL);
            }
            case KeyEvent.VK_RIGHT -> {
                buttonRight_Pressed = true;
                onButtonPress.accept(ButtonType.DIRECTIONAL);
            }
            default -> {
                // Do nothing
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Z -> buttonA_Pressed = false;
            case KeyEvent.VK_X -> buttonB_Pressed = false;
            case KeyEvent.VK_ENTER -> buttonStart_Pressed = false;
            case KeyEvent.VK_SPACE -> buttonSelect_Pressed = false;
            case KeyEvent.VK_UP -> buttonUp_Pressed = false;
            case KeyEvent.VK_DOWN -> buttonDown_Pressed = false;
            case KeyEvent.VK_LEFT -> buttonLeft_Pressed = false;
            case KeyEvent.VK_RIGHT -> buttonRight_Pressed = false;
        }
    }
}
