package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.config.AppSettings;

import java.util.prefs.Preferences;

public final class DesktopAppSettingsStore {
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
    private static final String AUDIO_DSP_PRESET = "audioDspPreset";
    private static final String AUDIO_DSP_INTENSITY = "audioDspIntensity";
    private static final String AUDIO_CHORUS_AMOUNT = "audioChorusAmount";
    private static final String AUDIO_REVERB_AMOUNT = "audioReverbAmount";
    private static final String AUDIO_SOUNDFONT_MODE = "audioSoundFontMode";
    private static final String AUDIO_SOUNDFONT_PATH = "audioSoundFontPath";
    private static final String AUDIO_OUTPUT_DEVICE = "audioOutputDevice";
    private static final String AUDIO_OUTPUT_BUFFER_MILLIS = "audioOutputBufferMillis";
    private static final String CONTROLLER_KEY_PREFIX = "controllerKey";
    private static final String PLAYER2_CONTROLLER_KEY_PREFIX = "player2ControllerKey";
    private static final String GAMEPAD_DEVICE_PREFIX = "gamepadDevice";
    private static final String GAMEPAD_DEVICE_NAME_PREFIX = "gamepadDeviceName";
    private static final String GAMEPAD_DEADZONE_PREFIX = "gamepadDeadzone";
    private static final String GAMEPAD_MAPPING_PREFIX = "gamepadMapping";
    private static final String GAMEPAD_PROFILE_PREFIX = "gamepadProfile.";
    private static final String TURBO_MULTIPLIER = "turboMultiplier";
    private static final String TURBO_KEY = "turboKey";
    private static final String TURBO_TOGGLE_MODE = "turboToggleMode";
    private static final String XBRZ_FILTERING = "xBrzFiltering";
    private static final String SUPER_GAME_BOY_BORDERS_ENABLED = "superGameBoyBordersEnabled";
    private static final String DEFAULT_BIOS_PATH = "defaultBios";
    private static final String RTC_OFFSET_HOURS = "rtcOffsetHours";
    private static final String RTC_OFFSET_MINUTES = "rtcOffsetMinutes";
    private static final String RTC_OFFSET_SECONDS = "rtcOffsetSeconds";
    private static final String MULTIPLAYER_TCP_MODE = "multiplayerTcpMode";
    private static final String MULTIPLAYER_HOST_MODE = "multiplayerHostMode";
    private static final String MULTIPLAYER_LOCAL_PATH = "multiplayerLocalPath";
    private static final String MULTIPLAYER_TCP_HOST = "multiplayerTcpHost";
    private static final String MULTIPLAYER_TCP_PORT = "multiplayerTcpPort";

    private DesktopAppSettingsStore() {
    }

    public static AppSettings load(Preferences preferences) {
        AppSettings defaults = AppSettings.defaults();
        int[] channelVolumes = new int[4];
        boolean[] channelMuted = new boolean[4];
        int[] controllerKeyCodes = new int[AppSettings.CONTROLLER_BUTTON_NAMES.length];
        int[] player2ControllerKeyCodes = new int[AppSettings.CONTROLLER_BUTTON_NAMES.length];
        AppSettings.GamepadConfig[] gamepadConfigs = new AppSettings.GamepadConfig[2];
        for (int i = 0; i < channelVolumes.length; i++) {
            channelVolumes[i] = clampPercent(preferences.getInt(AUDIO_CHANNEL_VOLUME_PREFIX + (i + 1), defaults.audioChannelVolumes()[i]));
            channelMuted[i] = preferences.getBoolean(AUDIO_CHANNEL_MUTED_PREFIX + (i + 1), defaults.audioChannelMuted()[i]);
        }
        for (int i = 0; i < controllerKeyCodes.length; i++) {
            controllerKeyCodes[i] = preferences.getInt(CONTROLLER_KEY_PREFIX + AppSettings.CONTROLLER_BUTTON_NAMES[i], defaults.controllerKeyCodes()[i]);
            player2ControllerKeyCodes[i] = preferences.getInt(PLAYER2_CONTROLLER_KEY_PREFIX + AppSettings.CONTROLLER_BUTTON_NAMES[i], defaults.player2ControllerKeyCodes()[i]);
        }
        AppSettings.AudioEnhancementConfig defaultAudio = defaults.audioEnhancement();
        AppSettings.AudioEnhancementConfig audioEnhancement = new AppSettings.AudioEnhancementConfig(
                preferences.get(AUDIO_DSP_PRESET, defaultAudio.dspPreset()),
                clamp(preferences.getInt(AUDIO_DSP_INTENSITY, defaultAudio.dspIntensity()), 0, 100),
                clamp(preferences.getInt(AUDIO_CHORUS_AMOUNT, defaultAudio.chorusAmount()), 0, 100),
                clamp(preferences.getInt(AUDIO_REVERB_AMOUNT, defaultAudio.reverbAmount()), 0, 100),
                preferences.get(AUDIO_SOUNDFONT_MODE, defaultAudio.soundFontMode()),
                preferences.get(AUDIO_SOUNDFONT_PATH, defaultAudio.soundFontPath()),
                preferences.get(AUDIO_OUTPUT_DEVICE, defaultAudio.outputDeviceName()),
                clamp(preferences.getInt(AUDIO_OUTPUT_BUFFER_MILLIS, defaultAudio.outputBufferMillis()), 20, 500)
        );
        for (int player = 0; player < gamepadConfigs.length; player++) {
            String[] mappings = new String[AppSettings.CONTROLLER_BUTTON_NAMES.length];
            AppSettings.GamepadConfig defaultConfig = defaults.gamepadConfig(player);
            for (int i = 0; i < mappings.length; i++) {
                mappings[i] = preferences.get(GAMEPAD_MAPPING_PREFIX + (player + 1) + AppSettings.CONTROLLER_BUTTON_NAMES[i], defaultConfig.mappings()[i]);
            }
            String deviceName = preferences.get(GAMEPAD_DEVICE_NAME_PREFIX + (player + 1), defaultConfig.deviceName());
            int deadzone = clamp(preferences.getInt(GAMEPAD_DEADZONE_PREFIX + (player + 1), defaultConfig.deadzonePercent()), 0, 95);
            if (!deviceName.isBlank()) {
                String profileKey = gamepadProfileKey(deviceName);
                deadzone = clamp(preferences.getInt(profileKey + ".deadzone", deadzone), 0, 95);
                for (int i = 0; i < mappings.length; i++) {
                    mappings[i] = preferences.get(profileKey + ".mapping." + AppSettings.CONTROLLER_BUTTON_NAMES[i], mappings[i]);
                }
            }
            gamepadConfigs[player] = new AppSettings.GamepadConfig(
                    preferences.getInt(GAMEPAD_DEVICE_PREFIX + (player + 1), defaultConfig.deviceIndex()),
                    deviceName,
                    deadzone,
                    mappings
            );
        }
        return new AppSettings(
                clamp(preferences.getInt(SCREEN_SCALE, defaults.screenScale()), 1, 8),
                preferences.getBoolean(SMOOTH_SCALING, defaults.smoothScaling()),
                preferences.getBoolean(FULLSCREEN, defaults.fullscreen()),
                clamp(preferences.getInt(REWIND_SECONDS, defaults.rewindSeconds()), 0, 120),
                clamp(preferences.getInt(REWIND_CAPTURE_INTERVAL_FRAMES, defaults.rewindCaptureIntervalFrames()), 1, 60),
                clampPercent(preferences.getInt(AUDIO_MASTER_VOLUME, defaults.audioMasterVolume())),
                clampPercent(preferences.getInt(AUDIO_LEFT_VOLUME, defaults.audioLeftVolume())),
                clampPercent(preferences.getInt(AUDIO_RIGHT_VOLUME, defaults.audioRightVolume())),
                channelVolumes,
                channelMuted,
                audioEnhancement,
                controllerKeyCodes,
                player2ControllerKeyCodes,
                gamepadConfigs,
                clamp(preferences.getInt(TURBO_MULTIPLIER, defaults.turboMultiplier()), 1, 10),
                preferences.getInt(TURBO_KEY, defaults.turboKeyCode()),
                preferences.getBoolean(TURBO_TOGGLE_MODE, defaults.turboToggleMode()),
                preferences.getBoolean(XBRZ_FILTERING, defaults.xBrzFiltering()),
                preferences.getBoolean(SUPER_GAME_BOY_BORDERS_ENABLED, defaults.superGameBoyBordersEnabled()),
                preferences.get(DEFAULT_BIOS_PATH, defaults.defaultBiosPath()),
                clamp(preferences.getInt(RTC_OFFSET_HOURS, defaults.rtcOffsetHours()), -9999, 9999),
                clamp(preferences.getInt(RTC_OFFSET_MINUTES, defaults.rtcOffsetMinutes()), -59, 59),
                clamp(preferences.getInt(RTC_OFFSET_SECONDS, defaults.rtcOffsetSeconds()), -59, 59),
                preferences.getBoolean(MULTIPLAYER_TCP_MODE, defaults.multiplayerTcpMode()),
                preferences.getBoolean(MULTIPLAYER_HOST_MODE, defaults.multiplayerHostMode()),
                preferences.get(MULTIPLAYER_LOCAL_PATH, defaults.multiplayerLocalPath()),
                preferences.get(MULTIPLAYER_TCP_HOST, defaults.multiplayerTcpHost()),
                clamp(preferences.getInt(MULTIPLAYER_TCP_PORT, defaults.multiplayerTcpPort()), 1, 65535)
        );
    }

    public static void save(Preferences preferences, AppSettings settings) {
        preferences.putInt(SCREEN_SCALE, settings.screenScale());
        preferences.putBoolean(SMOOTH_SCALING, settings.smoothScaling());
        preferences.putBoolean(XBRZ_FILTERING, settings.xBrzFiltering());
        preferences.putBoolean(FULLSCREEN, settings.fullscreen());
        preferences.putInt(REWIND_SECONDS, settings.rewindSeconds());
        preferences.putInt(REWIND_CAPTURE_INTERVAL_FRAMES, settings.rewindCaptureIntervalFrames());
        preferences.putInt(AUDIO_MASTER_VOLUME, settings.audioMasterVolume());
        preferences.putInt(AUDIO_LEFT_VOLUME, settings.audioLeftVolume());
        preferences.putInt(AUDIO_RIGHT_VOLUME, settings.audioRightVolume());
        for (int i = 0; i < settings.audioChannelVolumes().length; i++) {
            preferences.putInt(AUDIO_CHANNEL_VOLUME_PREFIX + (i + 1), settings.audioChannelVolumes()[i]);
            preferences.putBoolean(AUDIO_CHANNEL_MUTED_PREFIX + (i + 1), settings.audioChannelMuted()[i]);
        }
        AppSettings.AudioEnhancementConfig enhancement = settings.normalizedAudioEnhancement();
        preferences.put(AUDIO_DSP_PRESET, enhancement.dspPreset());
        preferences.putInt(AUDIO_DSP_INTENSITY, enhancement.dspIntensity());
        preferences.putInt(AUDIO_CHORUS_AMOUNT, enhancement.chorusAmount());
        preferences.putInt(AUDIO_REVERB_AMOUNT, enhancement.reverbAmount());
        preferences.put(AUDIO_SOUNDFONT_MODE, enhancement.soundFontMode());
        putOrRemove(preferences, AUDIO_SOUNDFONT_PATH, enhancement.soundFontPath());
        putOrRemove(preferences, AUDIO_OUTPUT_DEVICE, enhancement.outputDeviceName());
        preferences.putInt(AUDIO_OUTPUT_BUFFER_MILLIS, enhancement.outputBufferMillis());
        for (int i = 0; i < settings.controllerKeyCodes().length; i++) {
            preferences.putInt(CONTROLLER_KEY_PREFIX + AppSettings.CONTROLLER_BUTTON_NAMES[i], settings.controllerKeyCodes()[i]);
            preferences.putInt(PLAYER2_CONTROLLER_KEY_PREFIX + AppSettings.CONTROLLER_BUTTON_NAMES[i], settings.player2ControllerKeyCodes()[i]);
        }
        for (int player = 0; player < 2; player++) {
            AppSettings.GamepadConfig config = settings.gamepadConfig(player);
            preferences.putInt(GAMEPAD_DEVICE_PREFIX + (player + 1), config.deviceIndex());
            putOrRemove(preferences, GAMEPAD_DEVICE_NAME_PREFIX + (player + 1), config.deviceName());
            preferences.putInt(GAMEPAD_DEADZONE_PREFIX + (player + 1), config.deadzonePercent());
            for (int i = 0; i < AppSettings.CONTROLLER_BUTTON_NAMES.length; i++) {
                preferences.put(GAMEPAD_MAPPING_PREFIX + (player + 1) + AppSettings.CONTROLLER_BUTTON_NAMES[i], config.mappings()[i]);
            }
            saveGamepadProfile(preferences, config);
        }
        preferences.putInt(TURBO_MULTIPLIER, settings.turboMultiplier());
        preferences.putInt(TURBO_KEY, settings.turboKeyCode());
        preferences.putBoolean(TURBO_TOGGLE_MODE, settings.turboToggleMode());
        preferences.putBoolean(SUPER_GAME_BOY_BORDERS_ENABLED, settings.superGameBoyBordersEnabled());
        preferences.putInt(RTC_OFFSET_HOURS, settings.rtcOffsetHours());
        preferences.putInt(RTC_OFFSET_MINUTES, settings.rtcOffsetMinutes());
        preferences.putInt(RTC_OFFSET_SECONDS, settings.rtcOffsetSeconds());
        preferences.putBoolean(MULTIPLAYER_TCP_MODE, settings.multiplayerTcpMode());
        preferences.putBoolean(MULTIPLAYER_HOST_MODE, settings.multiplayerHostMode());
        preferences.put(MULTIPLAYER_LOCAL_PATH, settings.multiplayerLocalPath());
        preferences.put(MULTIPLAYER_TCP_HOST, settings.multiplayerTcpHost());
        preferences.putInt(MULTIPLAYER_TCP_PORT, settings.multiplayerTcpPort());
        putOrRemove(preferences, DEFAULT_BIOS_PATH, settings.defaultBiosPath());
    }

    private static void saveGamepadProfile(Preferences preferences, AppSettings.GamepadConfig config) {
        if (config.deviceName().isBlank()) {
            return;
        }
        String profileKey = gamepadProfileKey(config.deviceName());
        preferences.put(profileKey + ".name", config.deviceName());
        preferences.putInt(profileKey + ".deadzone", config.deadzonePercent());
        for (int i = 0; i < AppSettings.CONTROLLER_BUTTON_NAMES.length; i++) {
            preferences.put(profileKey + ".mapping." + AppSettings.CONTROLLER_BUTTON_NAMES[i], config.mappings()[i]);
        }
    }

    private static void putOrRemove(Preferences preferences, String key, String value) {
        if (value == null || value.isBlank()) {
            preferences.remove(key);
        } else {
            preferences.put(key, value);
        }
    }

    private static String gamepadProfileKey(String deviceName) {
        return GAMEPAD_PROFILE_PREFIX + Integer.toHexString(deviceName.hashCode());
    }

    private static int clampPercent(int value) {
        return clamp(value, 0, 100);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
