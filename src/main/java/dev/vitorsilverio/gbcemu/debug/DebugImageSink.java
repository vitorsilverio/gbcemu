package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.util.RawImage;

public interface DebugImageSink {
    DebugImageSink NOOP = (filename, image) -> false;

    boolean writePng(String filename, RawImage image);
}
