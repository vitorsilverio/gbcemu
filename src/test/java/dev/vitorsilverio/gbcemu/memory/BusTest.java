package dev.vitorsilverio.gbcemu.memory;

import dev.vitorsilverio.gbcemu.misc.Key1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class BusTest {

    @Test
    void canFindRegisteredMemorySpaceByType() {
        Bus bus = new Bus();
        Key1 key1 = new Key1();

        bus.addMemorySpace(key1);

        assertSame(key1, bus.findMemorySpace(Key1.class).orElseThrow());
    }
}
