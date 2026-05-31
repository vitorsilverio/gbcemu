package dev.vitorsilverio.gbcemu.controller;

public interface ButtonMaskProvider {
    int RIGHT = 1;
    int LEFT = 1 << 1;
    int UP = 1 << 2;
    int DOWN = 1 << 3;
    int A = 1 << 4;
    int B = 1 << 5;
    int SELECT = 1 << 6;
    int START = 1 << 7;

    int pressedButtonMask();
}
