package dev.vitorsilverio.gbcemu.android;

import android.content.Context;
import android.os.Process;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.core.Console;
import dev.vitorsilverio.gbcemu.core.Emulator;

import java.io.File;

public class AndroidConsoleHost {
    private static final boolean THROTTLE_ENABLED = true;
    private static final boolean DIAGNOSTIC_SKIP_APU = false;
    private static final boolean DIAGNOSTIC_SKIP_PPU = false;
    private static final boolean DIAGNOSTIC_SKIP_RENDER = false;

    private final GbcEmulatorSurface display;
    private final AndroidInputState inputState = new AndroidInputState();
    private final AndroidAudioOutput audioOutput = new AndroidAudioOutput(48_000);
    private final AndroidRumbleOutput rumbleOutput;
    private File romFile;
    private Emulator emulator;
    private Thread emulatorThread;
    private volatile boolean pausedByLifecycle;

    public AndroidConsoleHost(Context context, GbcEmulatorSurface display) {
        this.display = display;
        this.display.setPerformanceSuffix(performanceSuffix());
        this.rumbleOutput = new AndroidRumbleOutput(context);
    }

    public void resume() {
        pausedByLifecycle = false;
        display.start();
        audioOutput.start();
        if (emulator != null) {
            emulator.resume();
        } else if (romFile != null) {
            startEmulator();
        }
    }

    public void pause() {
        pausedByLifecycle = true;
        if (emulator != null) {
            emulator.pause();
        }
        display.stop();
        audioOutput.stop();
        rumbleOutput.stop();
    }

    public void stop() {
        stopEmulator();
        display.stop();
        audioOutput.stop();
        rumbleOutput.stop();
    }

    public void setButtonState(int button, boolean down) {
        inputState.setButtonState(button, down);
    }

    public boolean isPressed(int button) {
        return inputState.isPressed(button);
    }

    public AndroidInputState inputState() {
        return inputState;
    }

    public AndroidAudioOutput audioOutput() {
        return audioOutput;
    }

    public void setRomFile(File romFile) {
        this.romFile = romFile;
        display.setStatusText(romFile == null ? "No ROM loaded" : romFile.getName());
        stopEmulator();
        if (romFile != null && !pausedByLifecycle) {
            startEmulator();
        }
    }

    public File romFile() {
        return romFile;
    }

    public void rumble(double strength) {
        rumbleOutput.rumble(strength);
    }

    private synchronized void startEmulator() {
        stopEmulator();
        if (romFile == null) {
            return;
        }
        File saveFile = defaultSaveFile(romFile);
        Console console = new Console(
                null,
                romFile,
                saveFile,
                false,
                display,
                AppSettings.defaults(),
                inputState,
                audioOutput,
                null,
                null,
                true,
                false
        );
        console.setDiagnosticBypassOptions(DIAGNOSTIC_SKIP_APU, DIAGNOSTIC_SKIP_PPU, DIAGNOSTIC_SKIP_RENDER);
        console.setRuntimeDebugCaptureEnabled(false);
        console.setAutoFrameSkipEnabled(true);
        emulator = new Emulator(console, THROTTLE_ENABLED);
        emulator.skipBios();
        emulatorThread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
            emulator.start();
        }, "gbcemu-android-runtime");
        emulatorThread.setPriority(Thread.MAX_PRIORITY);
        emulatorThread.start();
        display.setStatusText("");
    }

    private synchronized void stopEmulator() {
        if (emulator != null) {
            emulator.stop();
            emulator = null;
        }
        Thread thread = emulatorThread;
        emulatorThread = null;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static File defaultSaveFile(File romFile) {
        String name = romFile.getName();
        int dot = name.lastIndexOf('.');
        String baseName = dot > 0 ? name.substring(0, dot) : name;
        File parent = romFile.getParentFile();
        return new File(parent == null ? new File(".") : parent, baseName + ".sav");
    }

    private static String performanceSuffix() {
        StringBuilder builder = new StringBuilder();
        if (!THROTTLE_ENABLED) {
            builder.append("UNTHROTTLED");
        }
        if (DIAGNOSTIC_SKIP_APU) {
            appendSuffix(builder, "NO_APU");
        }
        if (DIAGNOSTIC_SKIP_PPU) {
            appendSuffix(builder, "NO_PPU");
        }
        if (DIAGNOSTIC_SKIP_RENDER) {
            appendSuffix(builder, "NO_RENDER");
        }
        return builder.toString();
    }

    private static void appendSuffix(StringBuilder builder, String value) {
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value);
    }
}
