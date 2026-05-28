package dev.vitorsilverio.gbcemu.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int REQUEST_OPEN_ROM = 1001;

    private AndroidConsoleHost consoleHost;
    private AndroidRomStore romStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        romStore = new AndroidRomStore(this);
        GbcEmulatorSurface emulatorSurface = new GbcEmulatorSurface(this);
        OnScreenControlsView controlsView = new OnScreenControlsView(this);
        consoleHost = new AndroidConsoleHost(this, emulatorSurface);
        controlsView.setButtonListener(consoleHost::setButtonState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF101418);
        root.addView(emulatorSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(controlsView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        Button openRomButton = new Button(this);
        openRomButton.setText("ROM");
        openRomButton.setAlpha(0.78f);
        openRomButton.setOnClickListener(view -> openRomPicker());
        FrameLayout.LayoutParams openRomParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        openRomParams.leftMargin = 18;
        openRomParams.topMargin = 18;
        root.addView(openRomButton, openRomParams);
        setContentView(root);
        File lastRom = romStore.lastRom();
        if (lastRom != null) {
            consoleHost.setRomFile(lastRom);
        }
        hideSystemUi(root);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_ROM || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            if (flags != 0) {
                getContentResolver().takePersistableUriPermission(uri, flags);
            }
        } catch (SecurityException ignored) {
            // Some document providers do not offer persistable permissions; the imported copy is enough.
        }
        try {
            File romFile = romStore.importRom(uri);
            consoleHost.setRomFile(romFile);
            Toast.makeText(this, "ROM imported: " + romFile.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Could not import ROM: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (consoleHost != null) {
            consoleHost.resume();
        }
    }

    @Override
    protected void onPause() {
        if (consoleHost != null) {
            consoleHost.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (consoleHost != null) {
            consoleHost.stop();
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int button = mapAndroidButton(event.getKeyCode());
        if (button < 0 || consoleHost == null) {
            return super.dispatchKeyEvent(event);
        }
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP) {
            consoleHost.setButtonState(button, action == KeyEvent.ACTION_DOWN);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void openRomPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_OPEN_ROM);
    }

    private static int mapAndroidButton(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return AndroidInputState.BUTTON_RIGHT;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return AndroidInputState.BUTTON_LEFT;
            case KeyEvent.KEYCODE_DPAD_UP:
                return AndroidInputState.BUTTON_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return AndroidInputState.BUTTON_DOWN;
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_X:
                return AndroidInputState.BUTTON_A;
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_Z:
                return AndroidInputState.BUTTON_B;
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_ENTER:
                return AndroidInputState.BUTTON_START;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_SPACE:
                return AndroidInputState.BUTTON_SELECT;
            default:
                return -1;
        }
    }

    private static void hideSystemUi(View root) {
        root.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = root.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }
}
