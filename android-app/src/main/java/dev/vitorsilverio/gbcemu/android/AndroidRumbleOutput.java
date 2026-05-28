package dev.vitorsilverio.gbcemu.android;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class AndroidRumbleOutput {
    private final Vibrator vibrator;

    public AndroidRumbleOutput(Context context) {
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void rumble(double strength) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (strength <= 0) {
            vibrator.cancel();
            return;
        }
        int amplitude = Math.max(1, Math.min(255, (int) Math.round(strength * 255.0)));
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, amplitude));
        } else {
            vibrator.vibrate(40);
        }
    }

    public void stop() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}
