package dev.vitorsilverio.gbcemu.util;

import java.util.Arrays;

public final class RawImage {
    private final int width;
    private final int height;
    private final int[] argb;

    public RawImage(int width, int height) {
        this(width, height, new int[width * height], false);
    }

    public RawImage(int width, int height, int[] argb) {
        this(width, height, argb, true);
    }

    private RawImage(int width, int height, int[] argb, boolean copy) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        if (argb.length != width * height) {
            throw new IllegalArgumentException("Pixel buffer size does not match image dimensions");
        }
        this.width = width;
        this.height = height;
        this.argb = copy ? argb.clone() : argb;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int getArgb(int x, int y) {
        return argb[y * width + x];
    }

    public void setArgb(int x, int y, int color) {
        argb[y * width + x] = color;
    }

    public void fill(int color) {
        Arrays.fill(argb, color);
    }

    public int[] copyArgb() {
        return argb.clone();
    }

    public RawImage copy() {
        return new RawImage(width, height, argb);
    }

    public static RawImage wrapCopy(int width, int height, int[] argb) {
        return new RawImage(width, height, argb);
    }
}
