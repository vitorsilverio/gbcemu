package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.debug.DebugImageSink;
import dev.vitorsilverio.gbcemu.util.DebugJson;
import dev.vitorsilverio.gbcemu.util.RawImage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public final class PngDebugImageSink implements DebugImageSink {
    @Override
    public boolean writePng(String filename, RawImage image) {
        if (filename == null || filename.isBlank() || image == null) {
            return false;
        }
        File target = DebugJson.debugDirectory();
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            ImageIO.write(SwingImages.toBufferedImage(image), "png", new File(target, filename));
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write debug image " + filename, e);
        }
    }
}
