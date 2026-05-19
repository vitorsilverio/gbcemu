package dev.vitorsilverio.gbcemu.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;

public class GamepadController implements Controller, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GamepadController.class);
    private static final float DEADZONE = 0.35f;
    private static final long POLL_INTERVAL_MILLIS = 4L;
    private static volatile Object inputDevices;
    private static volatile boolean input4jUnavailable;

    private final int playerIndex;
    private volatile boolean running;
    private Thread pollThread;
    private Consumer<ButtonType> onButtonPress;
    private boolean buttonA;
    private boolean buttonB;
    private boolean buttonStart;
    private boolean buttonSelect;
    private boolean buttonUp;
    private boolean buttonDown;
    private boolean buttonLeft;
    private boolean buttonRight;

    public GamepadController(int playerIndex) {
        this.playerIndex = Math.max(0, playerIndex);
        start();
    }

    private void start() {
        Object devices = inputDevices();
        if (devices == null) {
            return;
        }
        running = true;
        pollThread = new Thread(this::pollLoop, "gbcemu-gamepad-" + (playerIndex + 1));
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private static Object inputDevices() {
        if (input4jUnavailable) {
            return null;
        }
        Object current = inputDevices;
        if (current != null) {
            return current;
        }
        synchronized (GamepadController.class) {
            if (inputDevices != null) {
                return inputDevices;
            }
            try {
                Class<?> inputDevicesClass = Class.forName("de.gurkenlabs.input4j.InputDevices");
                inputDevices = inputDevicesClass.getMethod("init").invoke(null);
                if (inputDevices == null) {
                    input4jUnavailable = true;
                    return null;
                }
                logger.info("input4j gamepad support initialized");
                return inputDevices;
            } catch (Throwable e) {
                input4jUnavailable = true;
                logger.info("input4j gamepad support unavailable: {}", e.toString());
                return null;
            }
        }
    }

    private void pollLoop() {
        while (running) {
            try {
                pollOnce();
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable e) {
                logger.debug("Failed to poll gamepad", e);
                releaseAll();
                sleepAfterFailure();
            }
        }
    }

    private void pollOnce() throws ReflectiveOperationException {
        Object device = deviceForPlayer();
        if (device == null) {
            releaseAll();
            return;
        }
        device.getClass().getMethod("poll").invoke(device);
        Collection<?> components = components(device);
        GamepadState state = readState(components);
        applyState(state);
    }

    private Object deviceForPlayer() throws ReflectiveOperationException {
        Object devices = inputDevices();
        if (devices == null) {
            return null;
        }
        Collection<?> all = allDevices(devices);
        if (all.size() <= playerIndex) {
            return null;
        }
        return new ArrayList<>(all).get(playerIndex);
    }

    private Collection<?> allDevices(Object devices) throws ReflectiveOperationException {
        Object result = devices.getClass().getMethod("getAll").invoke(devices);
        return result instanceof Collection<?> collection ? collection : java.util.List.of();
    }

    private Collection<?> components(Object device) throws ReflectiveOperationException {
        Object result = device.getClass().getMethod("getComponents").invoke(device);
        return result instanceof Collection<?> collection ? collection : java.util.List.of();
    }

    private GamepadState readState(Collection<?> components) {
        boolean a = false;
        boolean b = false;
        boolean start = false;
        boolean select = false;
        boolean up = false;
        boolean down = false;
        boolean left = false;
        boolean right = false;
        for (Object component : components) {
            String name = componentName(component);
            float value = componentValue(component);
            boolean pressed = value > 0.5f;
            a |= pressed && matches(name, "A", "CROSS", "BUTTON_0");
            b |= pressed && matches(name, "B", "CIRCLE", "BUTTON_1");
            start |= pressed && matches(name, "START", "OPTIONS", "BUTTON_7", "BUTTON_9");
            select |= pressed && matches(name, "BACK", "SELECT", "SHARE", "BUTTON_6", "BUTTON_8");
            up |= pressed && matches(name, "DPAD_UP");
            down |= pressed && matches(name, "DPAD_DOWN");
            left |= pressed && matches(name, "DPAD_LEFT");
            right |= pressed && matches(name, "DPAD_RIGHT");
            if (matches(name, "LEFT_THUMB_X", "LEFT_AXIS_X", "AXIS_X")) {
                left |= value < -DEADZONE;
                right |= value > DEADZONE;
            } else if (matches(name, "LEFT_THUMB_Y", "LEFT_AXIS_Y", "AXIS_Y")) {
                up |= value < -DEADZONE;
                down |= value > DEADZONE;
            } else if (matches(name, "DPAD", "DPAD_AXIS")) {
                up |= value == 0.25f || value == 0.125f || value == 0.375f;
                right |= value == 0.5f || value == 0.375f || value == 0.625f;
                down |= value == 0.75f || value == 0.625f || value == 0.875f;
                left |= value == 1.0f || value == 0.875f || value == 0.125f;
            }
        }
        return new GamepadState(a, b, start, select, up, down, left, right);
    }

    private String componentName(Object component) {
        Object id = invokeOptional(component, "getId", "id", "getName", "name");
        Object value = id == null ? component : id;
        String name = value.toString();
        return name == null ? "" : name.toUpperCase(Locale.ROOT);
    }

    private float componentValue(Object component) {
        Object value = invokeOptional(component, "getData", "data", "getValue", "value", "isPressed", "pressed");
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof Boolean pressed) {
            return pressed ? 1.0f : 0.0f;
        }
        return 0.0f;
    }

    private Object invokeOptional(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private boolean matches(String actual, String... expected) {
        for (String candidate : expected) {
            if (actual.equals(candidate) || actual.endsWith("_" + candidate)) {
                return true;
            }
        }
        return false;
    }

    private void applyState(GamepadState state) {
        emitPress(state.a && !buttonA, ButtonType.ACTION);
        emitPress(state.b && !buttonB, ButtonType.ACTION);
        emitPress(state.start && !buttonStart, ButtonType.ACTION);
        emitPress(state.select && !buttonSelect, ButtonType.ACTION);
        emitPress(state.up && !buttonUp, ButtonType.DIRECTIONAL);
        emitPress(state.down && !buttonDown, ButtonType.DIRECTIONAL);
        emitPress(state.left && !buttonLeft, ButtonType.DIRECTIONAL);
        emitPress(state.right && !buttonRight, ButtonType.DIRECTIONAL);
        buttonA = state.a;
        buttonB = state.b;
        buttonStart = state.start;
        buttonSelect = state.select;
        buttonUp = state.up;
        buttonDown = state.down;
        buttonLeft = state.left;
        buttonRight = state.right;
    }

    private void emitPress(boolean pressed, ButtonType type) {
        Consumer<ButtonType> listener = onButtonPress;
        if (pressed && listener != null) {
            listener.accept(type);
        }
    }

    private void sleepAfterFailure() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseAll() {
        buttonA = false;
        buttonB = false;
        buttonStart = false;
        buttonSelect = false;
        buttonUp = false;
        buttonDown = false;
        buttonLeft = false;
        buttonRight = false;
    }

    @Override
    public boolean isButtonA_Pressed() {
        return buttonA;
    }

    @Override
    public boolean isButtonB_Pressed() {
        return buttonB;
    }

    @Override
    public boolean isButtonStart_Pressed() {
        return buttonStart;
    }

    @Override
    public boolean isButtonSelect_Pressed() {
        return buttonSelect;
    }

    @Override
    public boolean isButtonUp_Pressed() {
        return buttonUp;
    }

    @Override
    public boolean isButtonDown_Pressed() {
        return buttonDown;
    }

    @Override
    public boolean isButtonLeft_Pressed() {
        return buttonLeft;
    }

    @Override
    public boolean isButtonRight_Pressed() {
        return buttonRight;
    }

    @Override
    public void eventEmitter(Consumer<ButtonType> onButtonPress) {
        this.onButtonPress = onButtonPress;
    }

    @Override
    public void close() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
        }
        releaseAll();
    }

    private record GamepadState(
            boolean a,
            boolean b,
            boolean start,
            boolean select,
            boolean up,
            boolean down,
            boolean left,
            boolean right
    ) {
    }
}
