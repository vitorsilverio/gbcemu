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
        int turboMultiplier,
        int turboKeyCode,
        boolean xBrzFiltering
) {
    public static final String[] CONTROLLER_BUTTON_NAMES = {"A", "B", "Start", "Select", "Up", "Down", "Left", "Right"};
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
    private static final String TURBO_MULTIPLIER = "turboMultiplier";
    private static final String TURBO_KEY = "turboKey";
    private static final String XBRZ_FILTERING = "xbrzFiltering";

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
                3,
                KeyEvent.VK_TAB,
                false
        );
    }

    public static AppSettings load(Preferences preferences) {
        AppSettings defaults = defaults();
        int[] channelVolumes = new int[4];
        boolean[] channelMuted = new boolean[4];
        int[] controllerKeyCodes = new int[CONTROLLER_BUTTON_NAMES.length];
        for (int i = 0; i < channelVolumes.length; i++) {
            channelVolumes[i] = clampPercent(preferences.getInt(AUDIO_CHANNEL_VOLUME_PREFIX + (i + 1), defaults.audioChannelVolumes[i]));
            channelMuted[i] = preferences.getBoolean(AUDIO_CHANNEL_MUTED_PREFIX + (i + 1), defaults.audioChannelMuted[i]);
        }
        for (int i = 0; i < controllerKeyCodes.length; i++) {
            controllerKeyCodes[i] = preferences.getInt(CONTROLLER_KEY_PREFIX + CONTROLLER_BUTTON_NAMES[i], defaults.controllerKeyCodes[i]);
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
                clamp(preferences.getInt(TURBO_MULTIPLIER, defaults.turboMultiplier), 1, 10),
                preferences.getInt(TURBO_KEY, defaults.turboKeyCode),
                preferences.getBoolean(XBRZ_FILTERING, defaults.xBrzFiltering)
        );
    }

    public void save(Preferences preferences) {
        preferences.putInt(SCREEN_SCALE, screenScale);
        preferences.putBoolean(SMOOTH_SCALING, smoothScaling);
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
        }
        preferences.putInt(TURBO_MULTIPLIER, turboMultiplier);
        preferences.putInt(TURBO_KEY, turboKeyCode);
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

    public AppSettings withAudioMasterVolume(int value) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, clampPercent(value), audioLeftVolume, audioRightVolume, audioChannelVolumes, audioChannelMuted, controllerKeyCodes, turboMultiplier, turboKeyCode, xBrzFiltering);
    }

    public AppSettings withAudioLeftVolume(int value) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, clampPercent(value), audioRightVolume, audioChannelVolumes, audioChannelMuted, controllerKeyCodes, turboMultiplier, turboKeyCode, xBrzFiltering);
    }

    public AppSettings withAudioRightVolume(int value) {
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, audioLeftVolume, clampPercent(value), audioChannelVolumes, audioChannelMuted, controllerKeyCodes, turboMultiplier, turboKeyCode, xBrzFiltering);
    }

    public AppSettings withAudioChannelVolume(int channel, int value) {
        int[] copy = audioChannelVolumes.clone();
        copy[channel - 1] = clampPercent(value);
        return new AppSettings(screenScale, smoothScaling, fullscreen, rewindSeconds, rewindCaptureIntervalFrames, audioMasterVolume, audioLeftVolume, audioRightVolume, copy, audioChannelMuted, controllerKeyCodes, turboMultiplier, turboKeyCode, xBrzFiltering);
    }

    public AppSettings normalized() {
        int[] volumes = Arrays.copyOf(audioChannelVolumes, 4);
        boolean[] muted = Arrays.copyOf(audioChannelMuted, 4);
        int[] keys = Arrays.copyOf(controllerKeyCodes, CONTROLLER_BUTTON_NAMES.length);
        for (int i = 0; i < volumes.length; i++) {
            volumes[i] = clampPercent(volumes[i]);
        }
        int[] defaults = defaultControllerKeyCodes();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] <= 0) {
                keys[i] = defaults[i];
            }
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
                clamp(turboMultiplier, 1, 10),
                turboKeyCode <= 0 ? KeyEvent.VK_TAB : turboKeyCode,
                xBrzFiltering
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

    private static int clampPercent(int value) {
        return clamp(value, 0, 100);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
