package dev.vitorsilverio.gbcemu.controller;

import java.util.function.Consumer;

public class IdleController implements Controller {

    @Override
    public boolean isButtonA_Pressed() {
        return false;
    }

    @Override
    public boolean isButtonB_Pressed() {
        return false;
    }

    @Override
    public boolean isButtonStart_Pressed() {
        return false;
    }

    @Override
    public boolean isButtonSelect_Pressed() {
        return false;
    }

    @Override
    public boolean isButtonUp_Pressed() {
        return false;
    }

    @Override
    public boolean isButtonDown_Pressed() {
        return false;
    }

    @Override
    public boolean isButtonLeft_Pressed() {
        return false;
    }

    @Override
    public boolean isButtonRight_Pressed() {
        return false;
    }

    @Override
    public void eventEmitter(Consumer<ButtonType> onButtonPress) {
    }
}
