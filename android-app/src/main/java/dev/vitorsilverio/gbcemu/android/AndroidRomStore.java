package dev.vitorsilverio.gbcemu.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

public class AndroidRomStore {
    private static final String PREFS = "gbcemu_android";
    private static final String LAST_ROM = "last_rom";

    private final Context context;
    private final SharedPreferences preferences;

    public AndroidRomStore(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public File importRom(Uri uri) throws IOException {
        String displayName = sanitizeDisplayName(displayName(uri));
        File romDirectory = romDirectory();
        File destination = new File(romDirectory, displayName);
        if (destination.isFile()) {
            preferences.edit().putString(LAST_ROM, destination.getAbsolutePath()).apply();
            return destination;
        }
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) {
                throw new IOException("Could not open selected ROM");
            }
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
        }
        preferences.edit().putString(LAST_ROM, destination.getAbsolutePath()).apply();
        return destination;
    }

    public void selectRom(File romFile) {
        if (romFile == null) {
            return;
        }
        preferences.edit().putString(LAST_ROM, romFile.getAbsolutePath()).apply();
    }

    public File lastRom() {
        String path = preferences.getString(LAST_ROM, null);
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        File file = new File(path);
        return file.isFile() ? file : null;
    }

    public File[] importedRoms() {
        File[] files = romDirectory().listFiles(file -> file.isFile() && isRomFile(file.getName()));
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return files;
    }

    private File romDirectory() {
        File romDirectory = new File(context.getFilesDir(), "roms");
        if (!romDirectory.exists()) {
            if (!romDirectory.mkdirs()) {
                throw new IllegalStateException("Could not create ROM directory: " + romDirectory);
            }
        }
        return romDirectory;
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) {
                        return value;
                    }
                }
            }
        }
        String segment = uri.getLastPathSegment();
        return segment == null || segment.trim().isEmpty() ? "rom.gb" : segment;
    }

    private static String sanitizeDisplayName(String displayName) {
        String sanitized = displayName.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (sanitized.trim().isEmpty()) {
            return "rom.gb";
        }
        return sanitized;
    }

    private static boolean isRomFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".gb") || lower.endsWith(".gbc") || lower.endsWith(".sgb");
    }
}
