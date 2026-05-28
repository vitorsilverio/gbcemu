package dev.vitorsilverio.gbcemu.core;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.controller.ButtonType;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.controller.IdleController;
import dev.vitorsilverio.gbcemu.controller.RumbleSink;

import java.util.function.Consumer;

public interface ConsoleInput extends Controller, RumbleSink, AutoCloseable {
    ConsoleInput IDLE = new ConsoleInput() {
        private final IdleController idleController = new IdleController();

        @Override
        public boolean isButtonA_Pressed() {
            return idleController.isButtonA_Pressed();
        }

        @Override
        public boolean isButtonB_Pressed() {
            return idleController.isButtonB_Pressed();
        }

        @Override
        public boolean isButtonStart_Pressed() {
            return idleController.isButtonStart_Pressed();
        }

        @Override
        public boolean isButtonSelect_Pressed() {
            return idleController.isButtonSelect_Pressed();
        }

        @Override
        public boolean isButtonUp_Pressed() {
            return idleController.isButtonUp_Pressed();
        }

        @Override
        public boolean isButtonDown_Pressed() {
            return idleController.isButtonDown_Pressed();
        }

        @Override
        public boolean isButtonLeft_Pressed() {
            return idleController.isButtonLeft_Pressed();
        }

        @Override
        public boolean isButtonRight_Pressed() {
            return idleController.isButtonRight_Pressed();
        }
    };

    default boolean isTurboPressed() {
        return false;
    }

    @Override
    default void eventEmitter(Consumer<ButtonType> onButtonPress) {
    }

    default void applySettings(AppSettings settings, int playerIndex) {
    }

    @Override
    default void setRumble(boolean active) {
    }

    @Override
    default void close() {
    }
}
