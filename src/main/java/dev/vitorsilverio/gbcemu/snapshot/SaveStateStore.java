package dev.vitorsilverio.gbcemu.snapshot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class SaveStateStore {
    private static final Pattern UNSAFE_FILE_CHARS = Pattern.compile("[^A-Za-z0-9._ -]");

    public List<Slot> list(File romFile) {
        File directory = directory(romFile);
        String baseName = baseName(romFile);
        File[] files = directory.listFiles((dir, name) -> name.matches(Pattern.quote(baseName) + "\\.sa\\d+"));
        List<Slot> slots = new ArrayList<>();
        if (files == null) {
            return slots;
        }
        for (File file : files) {
            Optional<Integer> index = slotIndex(baseName, file);
            if (index.isEmpty()) {
                continue;
            }
            read(file).ifPresent(saveState -> slots.add(new Slot(index.get(), file, saveState)));
        }
        slots.sort(Comparator.comparingInt(Slot::index));
        return slots;
    }

    public Slot save(File romFile, int slotIndex, SaveStateFile saveStateFile) throws IOException {
        File file = fileForSlot(romFile, slotIndex);
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(saveStateFile);
        }
        return new Slot(slotIndex, file, saveStateFile);
    }

    public Optional<Slot> load(File romFile, int slotIndex) {
        File file = fileForSlot(romFile, slotIndex);
        if (!file.isFile()) {
            return Optional.empty();
        }
        return read(file).map(saveState -> new Slot(slotIndex, file, saveState));
    }

    public int nextSlotIndex(File romFile) {
        return list(romFile).stream()
                .mapToInt(Slot::index)
                .max()
                .stream()
                .map(index -> index + 1)
                .findFirst()
                .orElse(0);
    }

    private Optional<SaveStateFile> read(File file) {
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            Object object = input.readObject();
            if (object instanceof SaveStateFile saveStateFile) {
                return Optional.of(saveStateFile);
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<Integer> slotIndex(String baseName, File file) {
        String prefix = baseName + ".sa";
        String name = file.getName();
        if (!name.startsWith(prefix)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(name.substring(prefix.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private File fileForSlot(File romFile, int slotIndex) {
        return new File(directory(romFile), baseName(romFile) + ".sa" + slotIndex);
    }

    private File directory(File romFile) {
        File parent = romFile == null ? null : romFile.getParentFile();
        return parent == null ? new File(System.getProperty("user.dir")) : parent;
    }

    private String baseName(File romFile) {
        String name = romFile == null ? "savestate" : romFile.getName();
        int dot = name.lastIndexOf('.');
        String withoutExtension = dot >= 0 ? name.substring(0, dot) : name;
        String sanitized = UNSAFE_FILE_CHARS.matcher(withoutExtension).replaceAll("_").trim();
        return sanitized.isBlank() ? "savestate" : sanitized;
    }

    public record Slot(int index, File file, SaveStateFile saveStateFile) {
    }
}
