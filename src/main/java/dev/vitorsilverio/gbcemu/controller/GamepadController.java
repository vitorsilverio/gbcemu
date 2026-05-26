package dev.vitorsilverio.gbcemu.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vitorsilverio.gbcemu.config.AppSettings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GamepadController implements Controller, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GamepadController.class);
    private static final long POLL_INTERVAL_MILLIS = 4L;
    private static final long FAILURE_RETRY_MILLIS = 16L;
    private static final long DIGITAL_RELEASE_GRACE_NANOS = 72_000_000L;
    private static final long ANALOG_RELEASE_GRACE_NANOS = 24_000_000L;
    private static final int MAX_TRANSIENT_FAILURES = 8;
    private static volatile Object inputDevices;
    private static volatile boolean input4jUnavailable;

    private volatile AppSettings.GamepadConfig config;
    private volatile boolean running;
    private Thread pollThread;
    private int consecutivePollFailures;
    private volatile Consumer<ButtonType> onButtonPress;
    private volatile boolean buttonA;
    private volatile boolean buttonB;
    private volatile boolean buttonStart;
    private volatile boolean buttonSelect;
    private volatile boolean buttonUp;
    private volatile boolean buttonDown;
    private volatile boolean buttonLeft;
    private volatile boolean buttonRight;
    private long buttonAReleaseAt;
    private long buttonBReleaseAt;
    private long buttonStartReleaseAt;
    private long buttonSelectReleaseAt;
    private long buttonUpReleaseAt;
    private long buttonDownReleaseAt;
    private long buttonLeftReleaseAt;
    private long buttonRightReleaseAt;

    public GamepadController(int playerIndex) {
        this(AppSettings.defaults().gamepadConfig(playerIndex));
    }

    public GamepadController(AppSettings.GamepadConfig config) {
        this.config = config;
        start();
    }

    public void applySettings(AppSettings.GamepadConfig config) {
        this.config = config;
        releaseAll();
    }

    private void start() {
        Object devices = inputDevices();
        if (devices == null) {
            return;
        }
        running = true;
        pollThread = new Thread(this::pollLoop, "gbcemu-gamepad");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    public static List<String> deviceNames() {
        Object devices = inputDevices();
        if (devices == null) {
            return List.of();
        }
        try {
            Object result = devices.getClass().getMethod("getAll").invoke(devices);
            if (!(result instanceof Collection<?> collection)) {
                return List.of();
            }
            List<String> names = new ArrayList<>();
            int index = 1;
            for (Object device : collection) {
                names.add("#" + index + " " + deviceName(device));
                index++;
            }
            return names;
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.debug("Failed to list gamepads", e);
            return List.of();
        }
    }

    public static String deviceProfileKey(int deviceIndex) {
        if (deviceIndex < 0) {
            return "";
        }
        Object devices = inputDevices();
        if (devices == null) {
            return "";
        }
        try {
            Object result = devices.getClass().getMethod("getAll").invoke(devices);
            if (!(result instanceof Collection<?> collection) || collection.size() <= deviceIndex) {
                return "";
            }
            Object device = new ArrayList<>(collection).get(deviceIndex);
            return deviceName(device);
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.debug("Failed to identify gamepad", e);
            return "";
        }
    }

    public static String componentSnapshot(int deviceIndex) {
        if (deviceIndex < 0) {
            return "Gamepad disabled.";
        }
        Object devices = inputDevices();
        if (devices == null) {
            return "input4j unavailable.";
        }
        try {
            Object result = devices.getClass().getMethod("getAll").invoke(devices);
            if (!(result instanceof Collection<?> collection) || collection.size() <= deviceIndex) {
                return "Device not found.";
            }
            Object device = new ArrayList<>(collection).get(deviceIndex);
            device.getClass().getMethod("poll").invoke(device);
            Object componentsResult = device.getClass().getMethod("getComponents").invoke(device);
            if (!(componentsResult instanceof Collection<?> components)) {
                return "No components.";
            }
            StringBuilder builder = new StringBuilder();
            for (Object component : components) {
                String name = componentNameStatic(component);
                float value = componentValueStatic(component);
                builder.append(name).append(" = ").append(String.format(Locale.ROOT, "%.3f", value)).append('\n');
            }
            return builder.isEmpty() ? "No components." : builder.toString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.debug("Failed to read gamepad components", e);
            return "Failed to read components: " + e.getMessage();
        }
    }

    public static String captureNextMapping(int deviceIndex, int deadzonePercent, long timeoutMillis) {
        if (deviceIndex < 0) {
            return "";
        }
        Object devices = inputDevices();
        if (devices == null) {
            return "";
        }
        try {
            Object result = devices.getClass().getMethod("getAll").invoke(devices);
            if (!(result instanceof Collection<?> collection) || collection.size() <= deviceIndex) {
                return "";
            }
            Object device = new ArrayList<>(collection).get(deviceIndex);
            float threshold = Math.max(0.05f, Math.min(0.95f, deadzonePercent / 100.0f));
            List<ComponentReading> baseline = componentReadings(device);
            long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
            while (System.nanoTime() < deadline && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(20L);
                List<ComponentReading> current = componentReadings(device);
                String token = changedComponentToken(baseline, current, threshold);
                if (!token.isBlank()) {
                    return token;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.debug("Failed to capture gamepad input", e);
        }
        return "";
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
                consecutivePollFailures = 0;
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable e) {
                logger.debug("Failed to poll gamepad", e);
                consecutivePollFailures++;
                if (consecutivePollFailures >= MAX_TRANSIENT_FAILURES) {
                    applyState(GamepadState.released());
                }
                sleepAfterFailure();
            }
        }
    }

    private void pollOnce() throws ReflectiveOperationException {
        Object device = deviceForPlayer();
        if (device == null) {
            applyState(GamepadState.released());
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
        int deviceIndex = config == null ? -1 : config.deviceIndex();
        if (deviceIndex < 0) {
            return null;
        }
        List<?> all = new ArrayList<>(allDevices(devices));
        String deviceName = config == null || config.deviceName() == null ? "" : config.deviceName();
        if (!deviceName.isBlank()) {
            if (deviceIndex < all.size() && deviceName.equals(deviceName(all.get(deviceIndex)))) {
                return all.get(deviceIndex);
            }
            for (Object device : all) {
                if (deviceName.equals(deviceName(device))) {
                    return device;
                }
            }
        }
        if (all.size() <= deviceIndex) {
            return null;
        }
        return all.get(deviceIndex);
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
        boolean analogDirection = false;
        for (Object component : components) {
            String name = componentName(component);
            float value = componentValue(component);
            AppSettings.GamepadConfig currentConfig = config;
            String[] mappings = currentConfig == null ? AppSettings.defaults().gamepadConfig(0).mappings() : currentConfig.mappings();
            float deadzone = currentConfig == null ? 0.35f : currentConfig.deadzonePercent() / 100.0f;
            a |= matchesMapping(name, value, mappings[0], deadzone);
            b |= matchesMapping(name, value, mappings[1], deadzone);
            start |= matchesMapping(name, value, mappings[2], deadzone);
            select |= matchesMapping(name, value, mappings[3], deadzone);
            up |= matchesMapping(name, value, mappings[4], deadzone);
            down |= matchesMapping(name, value, mappings[5], deadzone);
            left |= matchesMapping(name, value, mappings[6], deadzone);
            right |= matchesMapping(name, value, mappings[7], deadzone);
            boolean analogLeft = false;
            boolean analogRight = false;
            boolean analogUp = false;
            boolean analogDown = false;
            if (matches(name, "LEFT_THUMB_X", "LEFT_AXIS_X", "AXIS_X")) {
                analogLeft = value < -deadzone;
                analogRight = value > deadzone;
                left |= analogLeft;
                right |= analogRight;
            } else if (matches(name, "LEFT_THUMB_Y", "LEFT_AXIS_Y", "AXIS_Y")) {
                analogUp = value < -deadzone;
                analogDown = value > deadzone;
                up |= analogUp;
                down |= analogDown;
            } else if (matches(name, "DPAD", "DPAD_AXIS")) {
                analogUp = value == 0.25f || value == 0.125f || value == 0.375f;
                analogRight = value == 0.5f || value == 0.375f || value == 0.625f;
                analogDown = value == 0.75f || value == 0.625f || value == 0.875f;
                analogLeft = value == 1.0f || value == 0.875f || value == 0.125f;
                up |= analogUp;
                right |= analogRight;
                down |= analogDown;
                left |= analogLeft;
            }
            analogDirection |= analogUp || analogDown || analogLeft || analogRight;
        }
        return new GamepadState(a, b, start, select, up, down, left, right, analogDirection);
    }

    private String componentName(Object component) {
        return componentNameStatic(component);
    }

    private static String componentNameStatic(Object component) {
        Object id = invokeOptionalStatic(component, "getId", "id", "getName", "name");
        Object value = id == null ? component : id;
        String name = value.toString();
        return name == null ? "" : name.toUpperCase(Locale.ROOT);
    }

    private static String deviceName(Object device) {
        Object name = invokeOptionalStatic(device, "getName", "name", "getProductName", "productName");
        return name == null ? device.toString() : name.toString().strip();
    }

    private float componentValue(Object component) {
        return componentValueStatic(component);
    }

    private static float componentValueStatic(Object component) {
        Object value = invokeOptionalStatic(component, "getData", "data", "getValue", "value", "isPressed", "pressed");
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof Boolean pressed) {
            return pressed ? 1.0f : 0.0f;
        }
        return 0.0f;
    }

    private static List<ComponentReading> componentReadings(Object device) throws ReflectiveOperationException {
        device.getClass().getMethod("poll").invoke(device);
        Object componentsResult = device.getClass().getMethod("getComponents").invoke(device);
        if (!(componentsResult instanceof Collection<?> components)) {
            return List.of();
        }
        List<ComponentReading> readings = new ArrayList<>();
        for (Object component : components) {
            readings.add(new ComponentReading(componentNameStatic(component), componentValueStatic(component)));
        }
        return readings;
    }

    private static String changedComponentToken(List<ComponentReading> baseline, List<ComponentReading> current, float threshold) {
        for (ComponentReading reading : current) {
            float previous = baselineValue(baseline, reading.name());
            float delta = reading.value() - previous;
            if (Math.abs(delta) < threshold || Math.abs(reading.value()) < threshold) {
                continue;
            }
            if (isAxisLike(reading.name())) {
                return reading.name() + (reading.value() < 0 ? "-" : "+");
            }
            return reading.name();
        }
        return "";
    }

    private static float baselineValue(List<ComponentReading> baseline, String name) {
        for (ComponentReading reading : baseline) {
            if (reading.name().equals(name)) {
                return reading.value();
            }
        }
        return 0.0f;
    }

    private static boolean isAxisLike(String name) {
        return name.contains("AXIS")
                || name.contains("THUMB")
                || name.contains("STICK")
                || name.contains("TRIGGER")
                || name.endsWith("_X")
                || name.endsWith("_Y");
    }

    private Object invokeOptional(Object target, String... methodNames) {
        return invokeOptionalStatic(target, methodNames);
    }

    private static Object invokeOptionalStatic(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private boolean matchesMapping(String actual, float value, String mapping, float deadzone) {
        if (mapping == null || mapping.isBlank()) {
            return false;
        }
        return Arrays.stream(mapping.split(","))
                .map(String::strip)
                .filter(token -> !token.isBlank())
                .anyMatch(token -> matchesToken(actual, value, token.toUpperCase(Locale.ROOT), deadzone));
    }

    private boolean matchesToken(String actual, float value, String token, float deadzone) {
        if (token.endsWith("+")) {
            return matches(actual, token.substring(0, token.length() - 1)) && value > deadzone;
        }
        if (token.endsWith("-")) {
            return matches(actual, token.substring(0, token.length() - 1)) && value < -deadzone;
        }
        return matches(actual, token) && value > 0.5f;
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
        long now = System.nanoTime();
        if (state.a) {
            buttonAReleaseAt = now + DIGITAL_RELEASE_GRACE_NANOS;
        }
        if (state.b) {
            buttonBReleaseAt = now + DIGITAL_RELEASE_GRACE_NANOS;
        }
        if (state.start) {
            buttonStartReleaseAt = now + DIGITAL_RELEASE_GRACE_NANOS;
        }
        if (state.select) {
            buttonSelectReleaseAt = now + DIGITAL_RELEASE_GRACE_NANOS;
        }
        if (state.up) {
            buttonUpReleaseAt = now + directionalReleaseGrace(state);
        }
        if (state.down) {
            buttonDownReleaseAt = now + directionalReleaseGrace(state);
        }
        if (state.left) {
            buttonLeftReleaseAt = now + directionalReleaseGrace(state);
        }
        if (state.right) {
            buttonRightReleaseAt = now + directionalReleaseGrace(state);
        }

        boolean nextA = state.a || (buttonA && now < buttonAReleaseAt);
        boolean nextB = state.b || (buttonB && now < buttonBReleaseAt);
        boolean nextStart = state.start || (buttonStart && now < buttonStartReleaseAt);
        boolean nextSelect = state.select || (buttonSelect && now < buttonSelectReleaseAt);
        boolean nextUp = state.up || (buttonUp && now < buttonUpReleaseAt);
        boolean nextDown = state.down || (buttonDown && now < buttonDownReleaseAt);
        boolean nextLeft = state.left || (buttonLeft && now < buttonLeftReleaseAt);
        boolean nextRight = state.right || (buttonRight && now < buttonRightReleaseAt);

        emitPress(nextA && !buttonA, ButtonType.ACTION);
        emitPress(nextB && !buttonB, ButtonType.ACTION);
        emitPress(nextStart && !buttonStart, ButtonType.ACTION);
        emitPress(nextSelect && !buttonSelect, ButtonType.ACTION);
        emitPress(nextUp && !buttonUp, ButtonType.DIRECTIONAL);
        emitPress(nextDown && !buttonDown, ButtonType.DIRECTIONAL);
        emitPress(nextLeft && !buttonLeft, ButtonType.DIRECTIONAL);
        emitPress(nextRight && !buttonRight, ButtonType.DIRECTIONAL);
        buttonA = nextA;
        buttonB = nextB;
        buttonStart = nextStart;
        buttonSelect = nextSelect;
        buttonUp = nextUp;
        buttonDown = nextDown;
        buttonLeft = nextLeft;
        buttonRight = nextRight;
    }

    private long directionalReleaseGrace(GamepadState state) {
        return state.anyAnalogDirection() ? ANALOG_RELEASE_GRACE_NANOS : DIGITAL_RELEASE_GRACE_NANOS;
    }

    private void emitPress(boolean pressed, ButtonType type) {
        Consumer<ButtonType> listener = onButtonPress;
        if (pressed && listener != null) {
            listener.accept(type);
        }
    }

    private void sleepAfterFailure() {
        try {
            Thread.sleep(FAILURE_RETRY_MILLIS);
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
        buttonAReleaseAt = 0L;
        buttonBReleaseAt = 0L;
        buttonStartReleaseAt = 0L;
        buttonSelectReleaseAt = 0L;
        buttonUpReleaseAt = 0L;
        buttonDownReleaseAt = 0L;
        buttonLeftReleaseAt = 0L;
        buttonRightReleaseAt = 0L;
        consecutivePollFailures = 0;
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
            boolean right,
            boolean anyAnalogDirection
    ) {
        private static GamepadState released() {
            return new GamepadState(false, false, false, false, false, false, false, false, false);
        }
    }

    private record ComponentReading(String name, float value) {
    }
}
