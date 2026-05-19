package dev.vitorsilverio.gbcemu.controller;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CompositeController implements Controller, AutoCloseable {

    private final List<Controller> controllers;

    public CompositeController(Controller... controllers) {
        this.controllers = Arrays.stream(controllers)
                .filter(controller -> controller != null)
                .toList();
    }

    @Override
    public boolean isButtonA_Pressed() {
        return controllers.stream().anyMatch(Controller::isButtonA_Pressed);
    }

    @Override
    public boolean isButtonB_Pressed() {
        return controllers.stream().anyMatch(Controller::isButtonB_Pressed);
    }

    @Override
    public boolean isButtonStart_Pressed() {
        return controllers.stream().anyMatch(Controller::isButtonStart_Pressed);
    }

    @Override
    public boolean isButtonSelect_Pressed() {
        return controllers.stream().anyMatch(Controller::isButtonSelect_Pressed);
    }

    @Override
    public boolean isButtonUp_Pressed() {
        return controllers.stream().anyMatch(Controller::isButtonUp_Pressed);
    }

    @Override
    public boolean isButtonDown_Pressed() {
        return controllers.stream().anyMatch(Controller::isButtonDown_Pressed);
    }

    @Override
    public boolean isButtonLeft_Pressed() {
        return controllers.stream().anyMatch(Controller::isButtonLeft_Pressed);
    }

    @Override
    public boolean isButtonRight_Pressed() {
        return controllers.stream().anyMatch(Controller::isButtonRight_Pressed);
    }

    @Override
    public void eventEmitter(Consumer<ButtonType> onButtonPress) {
        controllers.forEach(controller -> controller.eventEmitter(onButtonPress));
    }

    @Override
    public void close() {
        for (Controller controller : controllers) {
            if (controller instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
