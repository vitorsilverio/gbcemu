package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameSharkDevice implements MemorySpace {

    private static final Logger logger = LoggerFactory.getLogger(GameSharkDevice.class);

    private final Map<Integer, Cheat> cheats;


    public GameSharkDevice() {
        this.cheats = new HashMap<>();
    }

    public void setCheats(String cheats) {
        this.cheats.clear();
        for (var line: cheats.split("\n")){
            var cheat = Cheat.fromString(line.trim());
            if (cheat != null) {
                this.cheats.put(cheat.address, cheat);
            }
        }
    }

    public String getCheats() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Cheat cheat : cheats.values()) {
            if (!first) {
                builder.append('\n');
            }
            builder.append(cheat);
            first = false;
        }
        return builder.toString();
    }

    @Override
    public boolean contains(int address) {
        return cheats.containsKey(address) && cheats.get(address).active();
    }

    @Override
    public byte read(int address) {
        if(cheats.get(address).active()) {
            return cheats.get(address).value();
        }
        return 0;
    }

    @Override
    public void write(int address, byte value) {

    }

    private record Cheat(boolean active, int address, byte value) {

        @Override
        public String toString() {
            return String.format("%s01%02x%04x",active?"":"#", value, Integer.reverseBytes(address) >> 16);
        }

        public static Cheat fromString(String cheat) {
            var pattern = Pattern.compile("#?([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})");
            Matcher matcher = pattern.matcher(cheat);
            if (matcher.matches()) {
                if (!"01".equals(matcher.group(1))) {
                    logger.warn("No support for this cheat operation");
                    return null;
                }
                return new Cheat(!cheat.startsWith("#"),
                        Integer.parseInt(matcher.group(4)+matcher.group(3),16),
                        (byte)Integer.parseInt(matcher.group(2),16));
            }
            return null;
        }
    }
}
