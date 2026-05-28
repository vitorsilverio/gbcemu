package dev.vitorsilverio.gbcemu.config;

import java.util.Arrays;

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
        AudioEnhancementConfig audioEnhancement,
        int[] controllerKeyCodes,
        int[] player2ControllerKeyCodes,
        GamepadConfig[] gamepadConfigs,
        int turboMultiplier,
        int turboKeyCode,
        boolean turboToggleMode,
        boolean xBrzFiltering,
        boolean superGameBoyBordersEnabled,
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
    public record GamepadConfig(int deviceIndex, String deviceName, int deadzonePercent, String[] mappings) {
    }
    public record AudioEnhancementConfig(
            String dspPreset,
            int dspIntensity,
            int chorusAmount,
            int reverbAmount,
            String soundFontMode,
            String soundFontPath,
            String outputDeviceName,
            int outputBufferMillis
    ) {
    }
    private static final int KEY_TAB = 9;
    private static final int KEY_ENTER = 10;
    private static final int KEY_SPACE = 32;
    private static final int KEY_LEFT = 37;
    private static final int KEY_UP = 38;
    private static final int KEY_RIGHT = 39;
    private static final int KEY_DOWN = 40;
    private static final int KEY_A = 65;
    private static final int KEY_D = 68;
    private static final int KEY_S = 83;
    private static final int KEY_W = 87;
    private static final int KEY_X = 88;
    private static final int KEY_Z = 90;
    private static final int KEY_NUMPAD0 = 96;
    private static final int KEY_NUMPAD1 = 97;
    private static final int KEY_NUMPAD2 = 98;
    private static final int KEY_NUMPAD3 = 99;

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
                defaultAudioEnhancement(),
                defaultControllerKeyCodes(),
                defaultPlayer2ControllerKeyCodes(),
                defaultGamepadConfigs(),
                3,
                KEY_TAB,
                false,
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

    public AudioEnhancementConfig normalizedAudioEnhancement() {
        return normalizedAudioEnhancement(audioEnhancement);
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
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, clampPercent(value), audioLeftVolume, audioRightVolume, audioChannelVolumes, audioChannelMuted, audioEnhancement, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, superGameBoyBordersEnabled, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, multiplayerTcpMode, multiplayerHostMode, multiplayerLocalPath, multiplayerTcpHost, multiplayerTcpPort);
    }

    public AppSettings withAudioLeftVolume(int value) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, clampPercent(value), audioRightVolume, audioChannelVolumes, audioChannelMuted, audioEnhancement, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, superGameBoyBordersEnabled, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, multiplayerTcpMode, multiplayerHostMode, multiplayerLocalPath, multiplayerTcpHost, multiplayerTcpPort);
    }

    public AppSettings withAudioRightVolume(int value) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, audioLeftVolume, clampPercent(value), audioChannelVolumes, audioChannelMuted, audioEnhancement, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, superGameBoyBordersEnabled, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, multiplayerTcpMode, multiplayerHostMode, multiplayerLocalPath, multiplayerTcpHost, multiplayerTcpPort);
    }

    public AppSettings withAudioChannelVolume(int channel, int value) {
        int[] copy = audioChannelVolumes.clone();
        copy[channel - 1] = clampPercent(value);
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, audioLeftVolume, audioRightVolume, copy, audioChannelMuted, audioEnhancement, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, superGameBoyBordersEnabled, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, multiplayerTcpMode, multiplayerHostMode, multiplayerLocalPath, multiplayerTcpHost, multiplayerTcpPort);
    }

    public AppSettings withMultiplayerConfig(
            boolean tcpMode,
            boolean hostMode,
            String localPath,
            String tcpHost,
            int tcpPort
    ) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, audioLeftVolume, audioRightVolume, audioChannelVolumes, audioChannelMuted, audioEnhancement, controllerKeyCodes, player2ControllerKeyCodes, gamepadConfigs, turboMultiplier, turboKeyCode, turboToggleMode, xBrzFiltering, superGameBoyBordersEnabled, defaultBiosPath, rtcOffsetHours, rtcOffsetMinutes, rtcOffsetSeconds, tcpMode, hostMode, localPath, tcpHost, tcpPort);
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
                normalizedAudioEnhancement(audioEnhancement),
                keys,
                player2Keys,
                normalizedGamepads,
                clamp(turboMultiplier, 1, 10),
                turboKeyCode <= 0 ? KEY_TAB : turboKeyCode,
                turboToggleMode,
                xBrzFiltering,
                superGameBoyBordersEnabled,
                trim(defaultBiosPath),
                clamp(rtcOffsetHours, -9999, 9999),
                clamp(rtcOffsetMinutes, -59, 59),
                clamp(rtcOffsetSeconds, -59, 59),
                multiplayerTcpMode,
                multiplayerHostMode,
                isBlank(multiplayerLocalPath) ? "gbcemu.sock" : multiplayerLocalPath.trim(),
                isBlank(multiplayerTcpHost) ? "localhost" : multiplayerTcpHost.trim(),
                clamp(multiplayerTcpPort, 1, 65535)
        );
    }

    private static int[] defaultControllerKeyCodes() {
        return new int[]{
                KEY_Z,
                KEY_X,
                KEY_ENTER,
                KEY_SPACE,
                KEY_UP,
                KEY_DOWN,
                KEY_LEFT,
                KEY_RIGHT
        };
    }

    private static int[] defaultPlayer2ControllerKeyCodes() {
        return new int[]{
                KEY_NUMPAD1,
                KEY_NUMPAD2,
                KEY_NUMPAD3,
                KEY_NUMPAD0,
                KEY_W,
                KEY_S,
                KEY_A,
                KEY_D
        };
    }

    private static GamepadConfig[] defaultGamepadConfigs() {
        return new GamepadConfig[]{
                new GamepadConfig(0, "", 35, defaultGamepadMappings()),
                new GamepadConfig(1, "", 35, defaultGamepadMappings())
        };
    }

    private static AudioEnhancementConfig defaultAudioEnhancement() {
        return new AudioEnhancementConfig("Raw", 35, 20, 15, "Off", "", "", 120);
    }

    private static AudioEnhancementConfig normalizedAudioEnhancement(AudioEnhancementConfig config) {
        AudioEnhancementConfig fallback = defaultAudioEnhancement();
        if (config == null) {
            return fallback;
        }
        String preset = normalizeChoice(config.dspPreset(), new String[]{"Raw", "Warm", "Wide", "Room", "Toy Synth"}, fallback.dspPreset());
        String soundFontMode = normalizeChoice(config.soundFontMode(), new String[]{"Off", "Overlay", "Replace original", "Percussion overlay"}, fallback.soundFontMode());
        return new AudioEnhancementConfig(
                preset,
                clamp(config.dspIntensity(), 0, 100),
                clamp(config.chorusAmount(), 0, 100),
                clamp(config.reverbAmount(), 0, 100),
                soundFontMode,
                trim(config.soundFontPath()),
                trim(config.outputDeviceName()),
                clamp(config.outputBufferMillis(), 20, 500)
        );
    }

    private static String normalizeChoice(String value, String[] allowed, String fallback) {
        if (value != null) {
            for (String candidate : allowed) {
                if (candidate.equals(value)) {
                    return candidate;
                }
            }
        }
        return fallback;
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
            if (isBlank(mappings[i])) {
                mappings[i] = fallback.mappings()[i];
            } else {
                mappings[i] = mappings[i].trim();
            }
        }
        String deviceName = trim(config.deviceName());
        return new GamepadConfig(clamp(config.deviceIndex(), -1, 15), deviceName, clamp(config.deadzonePercent(), 0, 95), mappings);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static int clampPercent(int value) {
        return clamp(value, 0, 100);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
