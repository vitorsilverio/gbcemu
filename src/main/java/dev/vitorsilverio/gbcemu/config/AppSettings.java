package dev.vitorsilverio.gbcemu.config;

import java.util.Arrays;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

public record AppSettings(
        int screenScale,
        boolean smoothScaling,
        boolean fullscreen,
        int rewindSeconds,
        int rewindCaptureIntervalFrames,
        int audioMasterVolume,
        int audioLeftVolume,
        int audioRightVolume,
        int[] audioChannelVolumes,
        boolean[] audioChannelMuted,
        int[] controllerKeyCodes,
        int[] player2ControllerKeyCodes,
        GamepadConfig[] gamepadConfigs,
        int turboMultiplier,
        int turboKeyCode,
        boolean turboToggleMode,
        boolean xBrzFiltering,
        String defaultBiosPath,
        int rtcOffsetHours,
        int rtcOffsetMinutes,
        int rtcOffsetSeconds,
        boolean multiplayerTcpMode,
        boolean multiplayerHostMode,
        String multiplayerLocalPath,
        String multiplayerTcpHost,
        int multiplayerTcpPort
) {
    public static final String[] CONTROLLER_BUTTON_NAMES = {"A", "B", "Start", "Select", "Up", "Down", "Left", "Right"};
    public record GamepadConfig(int deviceIndex, int deadzonePercent, String[] mappings) {
    }
    private static final String SCREEN_SCALE = "screenScale";
    private static final String SMOOTH_SCALING = "smoothScaling";
    private static final String FULLSCREEN = "fullscreen";
    private static final String REWIND_SECONDS = "rewindSeconds";
    private static final String REWIND_CAPTURE_INTERVAL_FRAMES = "rewindCaptureIntervalFrames";
    private static final String AUDIO_MASTER_VOLUME = "audioMasterVolume";
    private static final String AUDIO_LEFT_VOLUME = "audioLeftVolume";
    private static final String AUDIO_RIGHT_VOLUME = "audioRightVolume";
    private static final String AUDIO_CHANNEL_VOLUME_PREFIX = "audioChannelVolume";
    private static final String AUDIO_CHANNEL_MUTED_PREFIX = "audioChannelMuted";
    private static final String CONTROLLER_KEY_PREFIX = "controllerKey";
    private static final String PLAYER2_CONTROLLER_KEY_PREFIX = "player2ControllerKey";
    private static final String GAMEPAD_DEVICE_PREFIX = "gamepadDevice";
    private static final String GAMEPAD_DEADZONE_PREFIX = "gamepadDeadzone";
    private static final String GAMEPAD_MAPPING_PREFIX = "gamepadMapping";
    private static final String TURBO_MULTIPLIER = "turboMultiplier";
    private static final String TURBO_KEY = "turboKey";
    private static final String TURBO_TOGGLE_MODE = "turboToggleMode";
    private static final String XBRZ_FILTERING = "xbrzFiltering";
    private static final String DEFAULT_BIOS_PATH = "defaultBios";
    private static final String RTC_OFFSET_HOURS = "rtcOffsetHours";
    private static final String RTC_OFFSET_MINUTES = "rtcOffsetMinutes";
    private static final String RTC_OFFSET_SECONDS = "rtcOffsetSeconds";
    private static final String MULTIPLAYER_TCP_MODE = "multiplayerTcpMode";
    private static final String MULTIPLAYER_HOST_MODE = "multiplayerHostMode";
    private static final String MULTIPLAYER_LOCAL_PATH = "multiplayerLocalPath";
    private static final String MULTIPLAYER_TCP_HOST = "multiplayerTcpHost";
    private static final String MULTIPLAYER_TCP_PORT = "multiplayerTcpPort";

    public static AppSettings defaults() {
        return new AppSettings(
                4,
                false,
                false,
                0,
                30,
                100,
                100,
                100,
                new int[]{100, 100, 100, 100},
                new boolean[4],
                defaultControllerKeyCodes(),
                defaultPlayer2ControllerKeyCodes(),
                defaultGamepadConfigs(),
                3,
                KeyEvent.VK_TAB,
                false,
                false,
                "",
                0,
                0,
                0,
                false,
                true,
                "gbcemu.sock",
                "localhost",
                26803
        );
    }

    public static AppSettings load(Preferences preferences) {
        AppSettings defaults = defaults();
        int[] channelVolumes = new int[4];
        boolean[] channelMuted = new boolean[4];
        int[] controllerKeyCodes = new int[CONTROLLER_BUTTON_NAMES.length];
        int[] player2ControllerKeyCodes = new int[CONTROLLER_BUTTON_NAMES.length];
        GamepadConfig[] gamepadConfigs = new GamepadConfig[2];
        for (int i = 0; i < channelVolumes.length; i++) {
            channelVolumes[i] = clampPercent(preferences.getInt(AUDIO_CHANNEL_VOLUME_PREFIX + (i + 1), defaults.audioChannelVolumes[i]));
            channelMuted[i] = preferences.getBoolean(AUDIO_CHANNEL_MUTED_PREFIX + (i + 1), defaults.audioChannelMuted[i]);
        }
        for (int i = 0; i < controllerKeyCodes.length; i++) {
            controllerKeyCodes[i] = preferences.getInt(CONTROLLER_KEY_PREFIX + CONTROLLER_BUTTON_NAMES[i], defaults.controllerKeyCodes[i]);
            player2ControllerKeyCodes[i] = preferences.getInt(PLAYER2_CONTROLLER_KEY_PREFIX + CONTROLLER_BUTTON_NAMES[i], defaults.player2ControllerKeyCodes[i]);
        }
        for (int player = 0; player < gamepadConfigs.length; player++) {
            String[] mappings = new String[CONTROLLER_BUTTON_NAMES.length];
            GamepadConfig defaultConfig = defaults.gamepadConfig(player);
            for (int i = 0; i < mappings.length; i++) {
                mappings[i] = preferences.get(GAMEPAD_MAPPING_PREFIX + (player + 1) + CONTROLLER_BUTTON_NAMES[i], defaultConfig.mappings()[i]);
            }
            gamepadConfigs[player] = new GamepadConfig(
                    preferences.getInt(GAMEPAD_DEVICE_PREFIX + (player + 1), defaultConfig.deviceIndex()),
                    clamp(preferences.getInt(GAMEPAD_DEADZONE_PREFIX + (player + 1), defaultConfig.deadzonePercent()), 0, 95),
                    mappings
            );
        }
        return new AppSettings(
                clamp(preferences.getInt(SCREEN_SCALE, defaults.screenScale), 1, 8),
                preferences.getBoolean(SMOOTH_SCALING, defaults.smoothScaling),
                preferences.getBoolean(FULLSCREEN, defaults.fullscreen),
                clamp(preferences.getInt(REWIND_SECONDS, defaults.rewindSeconds), 0, 120),
                clamp(preferences.getInt(REWIND_CAPTURE_INTERVAL_FRAMES, defaults.rewindCaptureIntervalFrames), 1, 60),
                clampPercent(preferences.getInt(AUDIO_MASTER_VOLUME, defaults.audioMasterVolume)),
                clampPercent(preferences.getInt(AUDIO_LEFT_VOLUME, defaults.audioLeftVolume)),
                clampPercent(preferences.getInt(AUDIO_RIGHT_VOLUME, defaults.audioRightVolume)),
                channelVolumes,
                channelMuted,
                controllerKeyCodes,
                player2ControllerKeyCodes,
                gamepadConfigs,
                clamp(preferences.getInt(TURBO_MULTIPLIER, defaults.turboMultiplier), 1, 10),
                preferences.getInt(TURBO_KEY, defaults.turboKeyCode),
                preferences.getBoolean(TURBO_TOGGLE_MODE, defaults.turboToggleMode),
                preferences.getBoolean(XBRZ_FILTERING, defaults.xBrzFiltering),
                preferences.get(DEFAULT_BIOS_PATH, defaults.defaultBiosPath),
                clamp(preferences.getInt(RTC_OFFSET_HOURS, defaults.rtcOffsetHours), -9999, 9999),
                clamp(preferences.getInt(RTC_OFFSET_MINUTES, defaults.rtcOffsetMinutes), -59, 59),
                clamp(preferences.getInt(RTC_OFFSET_SECONDS, defaults.rtcOffsetSeconds), -59, 59),
                preferences.getBoolean(MULTIPLAYER_TCP_MODE, defaults.multiplayerTcpMode),
                preferences.getBoolean(MULTIPLAYER_HOST_MODE, defaults.multiplayerHostMode),
                preferences.get(MULTIPLAYER_LOCAL_PATH, defaults.multiplayerLocalPath),
                preferences.get(MULTIPLAYER_TCP_HOST, defaults.multiplayerTcpHost),
                clamp(preferences.getInt(MULTIPLAYER_TCP_PORT, defaults.multiplayerTcpPort), 1, 65535)
        );
    }

    public void save(Preferences preferences) {
        preferences.putInt(SCREEN_SCALE, screenScale);
        preferences.putBoolean(SMOOTH_SCALING, smoothScaling);
        preferences.putBoolean(XBRZ_FILTERING, xBrzFiltering);
        preferences.putBoolean(FULLSCREEN, fullscreen);
        preferences.putInt(REWIND_SECONDS, rewindSeconds);
        preferences.putInt(REWIND_CAPTURE_INTERVAL_FRAMES, rewindCaptureIntervalFrames);
        preferences.putInt(AUDIO_MASTER_VOLUME, audioMasterVolume);
        preferences.putInt(AUDIO_LEFT_VOLUME, audioLeftVolume);
        preferences.putInt(AUDIO_RIGHT_VOLUME, audioRightVolume);
        for (int i = 0; i < audioChannelVolumes.length; i++) {
            preferences.putInt(AUDIO_CHANNEL_VOLUME_PREFIX + (i + 1), audioChannelVolumes[i]);
            preferences.putBoolean(AUDIO_CHANNEL_MUTED_PREFIX + (i + 1), audioChannelMuted[i]);
        }
        for (int i = 0; i < controllerKeyCodes.length; i++) {
            preferences.putInt(CONTROLLER_KEY_PREFIX + CONTROLLER_BUTTON_NAMES[i], controllerKeyCodes[i]);
            preferences.putInt(PLAYER2_CONTROLLER_KEY_PREFIX + CONTROLLER_BUTTON_NAMES[i], player2ControllerKeyCodes[i]);
        }
        for (int player = 0; player < 2; player++) {
            GamepadConfig config = gamepadConfig(player);
            preferences.putInt(GAMEPAD_DEVICE_PREFIX + (player + 1), config.deviceIndex());
            preferences.putInt(GAMEPAD_DEADZONE_PREFIX + (player + 1), config.deadzonePercent());
            for (int i = 0; i < CONTROLLER_BUTTON_NAMES.length; i++) {
                preferences.put(GAMEPAD_MAPPING_PREFIX + (player + 1) + CONTROLLER_BUTTON_NAMES[i], config.mappings()[i]);
            }
        }
        preferences.putInt(TURBO_MULTIPLIER, turboMultiplier);
        preferences.putInt(TURBO_KEY, turboKeyCode);
        preferences.putBoolean(TURBO_TOGGLE_MODE, turboToggleMode);
        preferences.putInt(RTC_OFFSET_HOURS, rtcOffsetHours);
        preferences.putInt(RTC_OFFSET_MINUTES, rtcOffsetMinutes);
        preferences.putInt(RTC_OFFSET_SECONDS, rtcOffsetSeconds);
        preferences.putBoolean(MULTIPLAYER_TCP_MODE, multiplayerTcpMode);
        preferences.putBoolean(MULTIPLAYER_HOST_MODE, multiplayerHostMode);
        preferences.put(MULTIPLAYER_LOCAL_PATH, multiplayerLocalPath);
        preferences.put(MULTIPLAYER_TCP_HOST, multiplayerTcpHost);
        preferences.putInt(MULTIPLAYER_TCP_PORT, multiplayerTcpPort);
        if (defaultBiosPath == null || defaultBiosPath.isBlank()) {
            preferences.remove(DEFAULT_BIOS_PATH);
        } else {
            preferences.put(DEFAULT_BIOS_PATH, defaultBiosPath);
        }
    }

    public int rewindCapacity() {
        if (rewindSeconds <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(60.0 / rewindCaptureIntervalFrames) * rewindSeconds);
    }

    public int audioChannelVolume(int channel) {
        return audioChannelVolumes[channel - 1];
    }

    public boolean audioChannelMuted(int channel) {
        return audioChannelMuted[channel - 1];
    }

    public int controllerKeyCode(int index) {
        return controllerKeyCodes[index];
    }

    public int player2ControllerKeyCode(int index) {
        return player2ControllerKeyCodes[index];
    }

    public GamepadConfig gamepadConfig(int playerIndex) {
        GamepadConfig[] configs = gamepadConfigs == null ? defaultGamepadConfigs() : gamepadConfigs;
        if (configs.length == 0) {
            return normalizeGamepadConfig(null, playerIndex);
        }
        int index = clamp(playerIndex, 0, configs.length - 1);
        return normalizeGamepadConfig(configs[index], index);
    }

    public long rtcOffsetTotalSeconds() {
        return (rtcOffsetHours * 3600L) + (rtcOffsetMinutes * 60L) + rtcOffsetSeconds;
    }

    public AppSettings withAudioMasterVolume(int value) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, clampPercent(value), audioLeftVolume, audioRightVolume, audioChannelVolumes, audioChannelMuted, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, multiplayerTcpMode, multiplayerHostMode, multiplayerLocalPath, multiplayerTcpHost, multiplayerTcpPort);
    }

    public AppSettings withAudioLeftVolume(int value) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, clampPercent(value), audioRightVolume, audioChannelVolumes, audioChannelMuted, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, multiplayerTcpMode, multiplayerHostMode, multiplayerLocalPath, multiplayerTcpHost, multiplayerTcpPort);
    }

    public AppSettings withAudioRightVolume(int value) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, audioLeftVolume, clampPercent(value), audioChannelVolumes, audioChannelMuted, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, multiplayerTcpMode, multiplayerHostMode, multiplayerLocalPath, multiplayerTcpHost, multiplayerTcpPort);
    }

    public AppSettings withAudioChannelVolume(int channel, int value) {
        int[] copy = audioChannelVolumes.clone();
        copy[channel - 1] = clampPercent(value);
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, audioLeftVolume, audioRightVolume, copy, audioChannelMuted, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, multiplayerTcpMode, multiplayerHostMode, multiplayerLocalPath, multiplayerTcpHost, multiplayerTcpPort);
    }

    public AppSettings withMultiplayerConfig(
            boolean tcpMode,
            boolean hostMode,
            String localPath,
            String tcpHost,
            int tcpPort
    ) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, audioLeftVolume, audioRightVolume, audioChannelVolumes, audioChannelMuted, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, tcpMode, hostMode, localPath, tcpHost, tcpPort);
    }

    public AppSettings normalized() {
        int[] volumes = Arrays.copyOf(audioChannelVolumes, 4);
        boolean[] muted = Arrays.copyOf(audioChannelMuted, 4);
        int[] keys = Arrays.copyOf(controllerKeyCodes, CONTROLLER_BUTTON_NAMES.length);
        int[] player2Keys = Arrays.copyOf(player2ControllerKeyCodes, CONTROLLER_BUTTON_NAMES.length);
        GamepadConfig[] normalizedGamepads = new GamepadConfig[2];
        for (int i = 0; i < volumes.length; i++) {
            volumes[i] = clampPercent(volumes[i]);
        }
        int[] defaults = defaultControllerKeyCodes();
        int[] player2Defaults = defaultPlayer2ControllerKeyCodes();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] <= 0) {
                keys[i] = defaults[i];
            }
            if (player2Keys[i] <= 0) {
                player2Keys[i] = player2Defaults[i];
            }
        }
        for (int player = 0; player < normalizedGamepads.length; player++) {
            normalizedGamepads[player] = gamepadConfig(player);
        }
        return new AppSettings(
                clamp(screenScale, 1, 8),
                smoothScaling,
                fullscreen,
                clamp(rewindSeconds, 0, 120),
                clamp(rewindCaptureIntervalFrames, 1, 60),
                clampPercent(audioMasterVolume),
                clampPercent(audioLeftVolume),
                clampPercent(audioRightVolume),
                volumes,
                muted,
                keys,
                player2Keys,
                normalizedGamepads,
                clamp(turboMultiplier, 1, 10),
                turboKeyCode <= 0 ? KeyEvent.VK_TAB : turboKeyCode,
                turboToggleMode,
                xBrzFiltering,
                defaultBiosPath == null ? "" : defaultBiosPath.strip(),
                clamp(rtcOffsetHours, -9999, 9999),
                clamp(rtcOffsetMinutes, -59, 59),
                clamp(rtcOffsetSeconds, -59, 59),
                multiplayerTcpMode,
                multiplayerHostMode,
                multiplayerLocalPath == null || multiplayerLocalPath.isBlank() ? "gbcemu.sock" : multiplayerLocalPath.strip(),
                multiplayerTcpHost == null || multiplayerTcpHost.isBlank() ? "localhost" : multiplayerTcpHost.strip(),
                clamp(multiplayerTcpPort, 1, 65535)
        );
    }

    private static int[] defaultControllerKeyCodes() {
        return new int[]{
                KeyEvent.VK_Z,
                KeyEvent.VK_X,
                KeyEvent.VK_ENTER,
                KeyEvent.VK_SPACE,
                KeyEvent.VK_UP,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT
        };
    }

    private static int[] defaultPlayer2ControllerKeyCodes() {
        return new int[]{
                KeyEvent.VK_NUMPAD1,
                KeyEvent.VK_NUMPAD2,
                KeyEvent.VK_NUMPAD3,
                KeyEvent.VK_NUMPAD0,
                KeyEvent.VK_W,
                KeyEvent.VK_S,
                KeyEvent.VK_A,
                KeyEvent.VK_D
        };
    }

    private static GamepadConfig[] defaultGamepadConfigs() {
        return new GamepadConfig[]{
                new GamepadConfig(0, 35, defaultGamepadMappings()),
                new GamepadConfig(1, 35, defaultGamepadMappings())
        };
    }

    private static String[] defaultGamepadMappings() {
        return new String[]{
                "A,CROSS,BUTTON_0",
                "B,CIRCLE,BUTTON_1",
                "START,OPTIONS,BUTTON_7,BUTTON_9",
                "BACK,SELECT,SHARE,BUTTON_6,BUTTON_8",
                "DPAD_UP,LEFT_THUMB_Y-,LEFT_AXIS_Y-,AXIS_Y-",
                "DPAD_DOWN,LEFT_THUMB_Y+,LEFT_AXIS_Y+,AXIS_Y+",
                "DPAD_LEFT,LEFT_THUMB_X-,LEFT_AXIS_X-,AXIS_X-",
                "DPAD_RIGHT,LEFT_THUMB_X+,LEFT_AXIS_X+,AXIS_X+"
        };
    }

    private static GamepadConfig normalizeGamepadConfig(GamepadConfig config, int playerIndex) {
        GamepadConfig fallback = defaultGamepadConfigs()[clamp(playerIndex, 0, 1)];
        if (config == null) {
            config = fallback;
        }
        String[] mappings = Arrays.copyOf(config.mappings() == null ? fallback.mappings() : config.mappings(), CONTROLLER_BUTTON_NAMES.length);
        for (int i = 0; i < mappings.length; i++) {
            if (mappings[i] == null || mappings[i].isBlank()) {
                mappings[i] = fallback.mappings()[i];
            } else {
                mappings[i] = mappings[i].strip();
            }
        }
        return new GamepadConfig(clamp(config.deviceIndex(), -1, 15), clamp(config.deadzonePercent(), 0, 95), mappings);
    }

    private static int clampPercent(int value) {
        return clamp(value, 0, 100);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
