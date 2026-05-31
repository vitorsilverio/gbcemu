package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.controller.ButtonMaskProvider;
import dev.vitorsilverio.gbcemu.controller.ButtonType;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JoypadTest {

    @Test
    void readsActionButtonsWhenBitFiveIsLow() {
        Bus bus = new Bus();
        TestController controller = new TestController();
        Joypad joypad = new Joypad(bus, controller);

        joypad.write(0xFF00, (byte) 0x10);
        controller.a = true;
        controller.start = true;

        assertEquals(0xD6, joypad.read(0xFF00) & 0xFF);
    }

    @Test
    void readsDirectionalButtonsWhenBitFourIsLow() {
        Bus bus = new Bus();
        TestController controller = new TestController();
        Joypad joypad = new Joypad(bus, controller);

        joypad.write(0xFF00, (byte) 0x20);
        controller.right = true;
        controller.down = true;

        assertEquals(0xE6, joypad.read(0xFF00) & 0xFF);
    }

    @Test
    void readsButtonMaskProviderWithoutPerButtonPolling() {
        Bus bus = new Bus();
        TestMaskController controller = new TestMaskController();
        Joypad joypad = new Joypad(bus, controller);

        joypad.write(0xFF00, (byte) 0x10);
        controller.mask = ButtonMaskProvider.A | ButtonMaskProvider.START;

        assertEquals(0xD6, joypad.read(0xFF00) & 0xFF);

        joypad.write(0xFF00, (byte) 0x20);
        controller.mask = ButtonMaskProvider.RIGHT | ButtonMaskProvider.DOWN;

        assertEquals(0xE6, joypad.read(0xFF00) & 0xFF);
        assertEquals(0, controller.perButtonReads);
    }

    @Test
    void requestsJoypadInterruptOnlyForSelectedLine() {
        Bus bus = new Bus();
        TestController controller = new TestController();
        Joypad joypad = new Joypad(bus, controller);

        joypad.write(0xFF00, (byte) 0x10);
        controller.press(ButtonType.DIRECTIONAL);
        assertEquals(0x00, bus.read(0xFF0F) & 0x10);

        controller.press(ButtonType.ACTION);
        assertEquals(0x10, bus.read(0xFF0F) & 0x10);
    }

    private static class TestController implements Controller {
        private Consumer<ButtonType> listener;
        private boolean a;
        private boolean start;
        private boolean down;
        private boolean right;

        @Override
        public boolean isButtonA_Pressed() {
            return a;
        }

        @Override
        public boolean isButtonB_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonStart_Pressed() {
            return start;
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
            return down;
        }

        @Override
        public boolean isButtonLeft_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonRight_Pressed() {
            return right;
        }

        @Override
        public void eventEmitter(Consumer<ButtonType> onButtonPress) {
            listener = onButtonPress;
        }

        private void press(ButtonType buttonType) {
            listener.accept(buttonType);
        }
    }

    private static class TestMaskController extends TestController implements ButtonMaskProvider {
        private int mask;
        private int perButtonReads;

        @Override
        public int pressedButtonMask() {
            return mask;
        }

        @Override
        public boolean isButtonA_Pressed() {
            perButtonReads++;
            return super.isButtonA_Pressed();
        }

        @Override
        public boolean isButtonB_Pressed() {
            perButtonReads++;
            return super.isButtonB_Pressed();
        }

        @Override
        public boolean isButtonStart_Pressed() {
            perButtonReads++;
            return super.isButtonStart_Pressed();
        }

        @Override
        public boolean isButtonSelect_Pressed() {
            perButtonReads++;
            return super.isButtonSelect_Pressed();
        }

        @Override
        public boolean isButtonUp_Pressed() {
            perButtonReads++;
            return super.isButtonUp_Pressed();
        }

        @Override
        public boolean isButtonDown_Pressed() {
            perButtonReads++;
            return super.isButtonDown_Pressed();
        }

        @Override
        public boolean isButtonLeft_Pressed() {
            perButtonReads++;
            return super.isButtonLeft_Pressed();
        }

        @Override
        public boolean isButtonRight_Pressed() {
            perButtonReads++;
            return super.isButtonRight_Pressed();
        }
    }
}
