package dev.vitorsilverio.gbcemu.util;

import dev.vitorsilverio.gbcemu.memory.MemoryBank;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class DebugJson {
    private static final String DEBUG_DIRECTORY = "debug";

    private DebugJson() {
    }

    public static File debugDirectory() {
        File debug = new File(DEBUG_DIRECTORY);
        if (!debug.exists()) {
            debug.mkdirs();
        }
        return debug;
    }

    public static void appendString(StringBuilder builder, String name, String value, boolean comma, int indent) {
        appendIndent(builder, indent);
        builder.append('"').append(name).append("\": \"").append(escape(value)).append('"');
        appendCommaAndNewline(builder, comma);
    }

    public static void appendNumber(StringBuilder builder, String name, int value, boolean comma, int indent) {
        appendIndent(builder, indent);
        builder.append('"').append(name).append("\": ").append(value);
        appendCommaAndNewline(builder, comma);
    }

    public static void appendLong(StringBuilder builder, String name, long value, boolean comma, int indent) {
        appendIndent(builder, indent);
        builder.append('"').append(name).append("\": ").append(value);
        appendCommaAndNewline(builder, comma);
    }

    public static void appendHex(StringBuilder builder, String name, int value, boolean comma, int indent, int digits) {
        int maskedValue = digits >= 8 ? value : value & ((1 << (digits * 4)) - 1);
        appendString(builder, name, String.format("%0" + digits + "X", maskedValue), comma, indent);
    }

    public static void appendBoolean(StringBuilder builder, String name, boolean value, boolean comma, int indent) {
        appendIndent(builder, indent);
        builder.append('"').append(name).append("\": ").append(value);
        appendCommaAndNewline(builder, comma);
    }

    public static void appendObject(StringBuilder builder, String name, Map<?, ?> values, boolean comma, int indent) {
        appendIndent(builder, indent);
        builder.append('"').append(name).append("\": {\n");
        int index = 0;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            appendIndent(builder, indent + 2);
            builder.append('"').append(escape(String.valueOf(entry.getKey()))).append("\": ");
            appendValue(builder, entry.getValue());
            if (index < values.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
            index++;
        }
        appendIndent(builder, indent);
        builder.append('}');
        appendCommaAndNewline(builder, comma);
    }

    public static String memoryBankSample(MemoryBank bank, int maxBytes) {
        if (bank.bankCount() <= 0 || bank.bankSize() <= 0) {
            return "";
        }
        int currentBank = Math.floorMod(bank.currentBank(), bank.bankCount());
        int length = Math.min(maxBytes, bank.bankSize());
        StringBuilder sample = new StringBuilder();
        for (int offset = 0; offset < length; offset++) {
            if (offset > 0) {
                sample.append(' ');
            }
            sample.append(String.format("%02X", bank.readBank(currentBank, offset) & 0xFF));
        }
        return sample.toString();
    }

    public static String bytesHex(byte[] values, int start, int length) {
        StringBuilder builder = new StringBuilder();
        int end = Math.min(values.length, start + length);
        for (int index = start; index < end; index++) {
            if (index > start) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", values[index] & 0xFF));
        }
        return builder.toString();
    }

    public static File writeTargetFile(String filename, String content, String errorMessage) {
        File target = debugDirectory();
        File file = new File(target, filename);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            writeTextFile(file.toPath(), content);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    public static void writeTextFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void appendIndent(StringBuilder builder, int indent) {
        for (int i = 0; i < indent; i++) {
            builder.append(' ');
        }
    }

    public static void appendCommaAndNewline(StringBuilder builder, boolean comma) {
        if (comma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    public static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static void appendValue(StringBuilder builder, Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }
        builder.append('"').append(escape(String.valueOf(value))).append('"');
    }
}
