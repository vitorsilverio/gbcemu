package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.controller.ButtonType;
import dev.vitorsilverio.gbcemu.controller.CompositeController;
import dev.vitorsilverio.gbcemu.core.ConsoleInput;

import java.awt.event.KeyListener;
import java.util.function.Consumer;

public final class DesktopConsoleInput implements ConsoleInput {
    private final KeyboardController keyboardController;
    private final GamepadController gamepadController;
    private final CompositeController controller;
    private final int playerIndex;

    public DesktopConsoleInput(AppSettings settings, int playerIndex) {
        this.keyboardController = playerIndex == 0
                ? new KeyboardController(settings)
                : new KeyboardController(playerKeyCodes(settings, playerIndex), 0, false);
        this.gamepadController = new GamepadController(settings.gamepadConfig(playerIndex));
        this.controller = new CompositeController(keyboardController, gamepadController);
        this.playerIndex = playerIndex;
    }

    public KeyListener keyListener() {
        return keyboardController;
    }

    @Override
    public boolean isButtonA_Pressed() {
        return controller.isButtonA_Pressed();
    }

    @Override
    public boolean isButtonB_Pressed() {
        return controller.isButtonB_Pressed();
    }

    @Override
    public boolean isButtonStart_Pressed() {
        return controller.isButtonStart_Pressed();
    }

    @Override
    public boolean isButtonSelect_Pressed() {
        return controller.isButtonSelect_Pressed();
    }

    @Override
    public boolean isButtonUp_Pressed() {
        return controller.isButtonUp_Pressed();
    }

    @Override
    public boolean isButtonDown_Pressed() {
        return controller.isButtonDown_Pressed();
    }

    @Override
    public boolean isButtonLeft_Pressed() {
        return controller.isButtonLeft_Pressed();
    }

    @Override
    public boolean isButtonRight_Pressed() {
        return controller.isButtonRight_Pressed();
    }

    @Override
    public boolean isTurboPressed() {
        return keyboardController.isTurboPressed();
    }

    @Override
    public void eventEmitter(Consumer<ButtonType> onButtonPress) {
        controller.eventEmitter(onButtonPress);
    }

    @Override
    public void applySettings(AppSettings settings, int playerIndex) {
        if (this.playerIndex == 0) {
            keyboardController.applySettings(settings);
        } else {
            keyboardController.applyKeyCodes(playerKeyCodes(settings, this.playerIndex), 0, false);
        }
        gamepadController.applySettings(settings.gamepadConfig(this.playerIndex));
    }

    @Override
    public void setRumble(boolean active) {
        gamepadController.setRumble(active);
    }

    @Override
    public void close() {
        gamepadController.close();
    }

    private static int[] playerKeyCodes(AppSettings settings, int playerIndex) {
        int[] keyCodes = new int[AppSettings.CONTROLLER_BUTTON_NAMES.length];
        for (int i = 0; i < keyCodes.length; i++) {
            keyCodes[i] = playerIndex == 1
                    ? settings.player2ControllerKeyCode(i)
                    : settings.controllerKeyCode(i);
        }
        return keyCodes;
    }
}
