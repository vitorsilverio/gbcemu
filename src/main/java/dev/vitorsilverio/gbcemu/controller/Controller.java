package dev.vitorsilverio.gbcemu.controller;

import java.util.function.Consumer;

public interface Controller {

    boolean isButtonA_Pressed();
    boolean isButtonB_Pressed();
    boolean isButtonStart_Pressed();
    boolean isButtonSelect_Pressed();
    boolean isButtonUp_Pressed();
    boolean isButtonDown_Pressed();
    boolean isButtonLeft_Pressed();
    boolean isButtonRight_Pressed();

    void eventEmitter(Consumer<ButtonType> onButtonPress);
}
