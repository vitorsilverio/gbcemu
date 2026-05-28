package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.util.RawImage;

import java.awt.image.BufferedImage;

final class SwingImages {
    private SwingImages() {
    }

    static BufferedImage toBufferedImage(RawImage image) {
        if (image == null) {
            return null;
        }
        BufferedImage bufferedImage = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, image.width(), image.height(), image.copyArgb(), 0, image.width());
        return bufferedImage;
    }
}
