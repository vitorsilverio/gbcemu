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
        File romDirectory = new File(context.getFilesDir(), "roms");
        if (!romDirectory.exists() && !romDirectory.mkdirs()) {
            throw new IOException("Could not create ROM directory: " + romDirectory);
        }

        File destination = uniqueFile(romDirectory, displayName);
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

    public File lastRom() {
        String path = preferences.getString(LAST_ROM, null);
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        File file = new File(path);
        return file.isFile() ? file : null;
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

    private static File uniqueFile(File directory, String displayName) {
        File destination = new File(directory, displayName);
        if (!destination.exists()) {
            return destination;
        }

        int dot = displayName.lastIndexOf('.');
        String base = dot > 0 ? displayName.substring(0, dot) : displayName;
        String extension = dot > 0 ? displayName.substring(dot) : "";
        for (int i = 1; ; i++) {
            destination = new File(directory, base + "-" + i + extension);
            if (!destination.exists()) {
                return destination;
            }
        }
    }
}
