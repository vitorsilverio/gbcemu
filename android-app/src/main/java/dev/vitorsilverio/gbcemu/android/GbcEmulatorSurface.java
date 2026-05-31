package dev.vitorsilverio.gbcemu.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Process;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.core.ConsoleDisplay;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoy;

public class GbcEmulatorSurface extends SurfaceView implements SurfaceHolder.Callback, Runnable, ConsoleDisplay {
    private static final int GB_WIDTH = 160;
    private static final int GB_HEIGHT = 144;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statusBackgroundPaint = new Paint();
    private final Rect destination = new Rect();
    private final Object renderSignal = new Object();
    private final Object frameLock = new Object();
    private Bitmap frame = Bitmap.createBitmap(GB_WIDTH, GB_HEIGHT, Bitmap.Config.ARGB_8888);
    private Thread renderThread;
    private volatile boolean running;
    private boolean dirty = true;
    private boolean pendingFrame;
    private volatile String statusText = "Select a ROM";
    private volatile String performanceText = "";
    private volatile String performanceDetailText = "";
    private volatile String performanceSuffix = "";
    private int frameWidth = GB_WIDTH;
    private int frameHeight = GB_HEIGHT;
    private int[] emulatorFramePixels = new int[GB_WIDTH * GB_HEIGHT];
    private int[] pendingFramePixels = new int[GB_WIDTH * GB_HEIGHT];
    private int[] renderFramePixels = new int[GB_WIDTH * GB_HEIGHT];

    public GbcEmulatorSurface(Context context) {
        super(context);
        getHolder().addCallback(this);
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        statusPaint.setColor(Color.WHITE);
        statusPaint.setTextSize(28.0f);
        statsPaint.setColor(0xCCF5F7FA);
        statsPaint.setTextSize(22.0f);
        statsPaint.setTextAlign(Paint.Align.RIGHT);
        statusBackgroundPaint.setColor(0x99000000);
        drawBootPlaceholder();
    }

    public void setFrame(int[] argb, int width, int height) {
        if (argb == null || width <= 0 || height <= 0) {
            return;
        }
        ensureFrameBuffers(width, height);
        System.arraycopy(argb, 0, emulatorFramePixels, 0, Math.min(argb.length, emulatorFramePixels.length));
        publishFrame(width, height);
    }

    private void ensureFrameBuffers(int width, int height) {
        int length = width * height;
        if (emulatorFramePixels.length == length
                && pendingFramePixels.length == length
                && renderFramePixels.length == length) {
            return;
        }
        emulatorFramePixels = new int[length];
        pendingFramePixels = new int[length];
        renderFramePixels = new int[length];
    }

    private void publishFrame(int width, int height) {
        synchronized (frameLock) {
            int[] oldPending = pendingFramePixels;
            pendingFramePixels = emulatorFramePixels;
            emulatorFramePixels = oldPending;
            frameWidth = width;
            frameHeight = height;
            pendingFrame = true;
        }
        requestRender();
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText == null || statusText.trim().isEmpty() ? "" : statusText;
        requestRender();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        requestRender();
        renderThread = new Thread(this, "gbcemu-android-render");
        renderThread.start();
    }

    @Override
    public void attach(Ppu ppu, SuperGameBoy superGameBoy) {
        setStatusText("");
        renderFrame(ppu);
    }

    @Override
    public void detach(Ppu ppu) {
    }

    @Override
    public void renderFrame(Ppu ppu) {
        if (ppu == null) {
            return;
        }
        ensureFrameBuffers(GB_WIDTH, GB_HEIGHT);
        ppu.copyFrameBufferTo(emulatorFramePixels);
        publishFrame(GB_WIDTH, GB_HEIGHT);
    }

    @Override
    public void updatePerformanceStats(double fps, double speedPercent) {
        performanceText = String.format("%.1f FPS (%.0f%%)%s", fps, speedPercent, performanceSuffix);
        requestRender();
    }

    @Override
    public void updatePerformanceDetails(String details) {
        performanceDetailText = details == null ? "" : details;
        requestRender();
    }

    public void setPerformanceSuffix(String performanceSuffix) {
        this.performanceSuffix = performanceSuffix == null || performanceSuffix.isBlank() ? "" : " " + performanceSuffix;
        requestRender();
    }

    @Override
    public void applySettings(AppSettings settings) {
    }

    @Override
    public void show() {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void dispose() {
        stop();
    }

    public void stop() {
        running = false;
        requestRender();
        Thread thread = renderThread;
        if (thread != null) {
            try {
                thread.join(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        renderThread = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateDestination(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        while (running) {
            waitForRenderRequest();
            if (!running) {
                return;
            }
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                try {
                    drawFrame(canvas);
                } finally {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void requestRender() {
        synchronized (renderSignal) {
            dirty = true;
            renderSignal.notifyAll();
        }
    }

    private void waitForRenderRequest() {
        synchronized (renderSignal) {
            while (running && !dirty) {
                try {
                    renderSignal.wait(250L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                    return;
                }
            }
            dirty = false;
        }
    }

    private void drawFrame(Canvas canvas) {
        if (destination.isEmpty()) {
            updateDestination(canvas.getWidth(), canvas.getHeight());
        }
        updateBitmapFromPendingFrame();
        canvas.drawColor(0xFF101418);
        canvas.drawBitmap(frame, null, destination, paint);
        drawStatus(canvas);
        drawPerformanceStats(canvas);
    }

    private void updateBitmapFromPendingFrame() {
        int width;
        int height;
        boolean hasFrame;
        synchronized (frameLock) {
            hasFrame = pendingFrame;
            if (!hasFrame) {
                return;
            }
            int[] oldRender = renderFramePixels;
            renderFramePixels = pendingFramePixels;
            pendingFramePixels = oldRender;
            width = frameWidth;
            height = frameHeight;
            pendingFrame = false;
        }
        if (frame.getWidth() != width || frame.getHeight() != height) {
            frame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            updateDestination(getWidth(), getHeight());
        }
        frame.setPixels(renderFramePixels, 0, width, 0, 0, width, height);
    }

    private void drawStatus(Canvas canvas) {
        String text = statusText;
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        float padding = 14.0f;
        float textWidth = statusPaint.measureText(text);
        float left = padding;
        float top = padding;
        float right = Math.min(canvas.getWidth() - padding, left + textWidth + padding * 2.0f);
        float bottom = top + 42.0f;
        canvas.drawRect(left, top, right, bottom, statusBackgroundPaint);
        canvas.drawText(text, left + padding, bottom - 12.0f, statusPaint);
    }

    private void drawPerformanceStats(Canvas canvas) {
        String text = performanceText;
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        float padding = 14.0f;
        float y = canvas.getHeight() - padding;
        String details = performanceDetailText;
        if (details != null && !details.trim().isEmpty()) {
            canvas.drawText(details, canvas.getWidth() - padding, y - 28.0f, statsPaint);
        }
        canvas.drawText(text, canvas.getWidth() - padding, y, statsPaint);
    }

    private void updateDestination(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int frameWidth;
        int frameHeight;
        synchronized (frameLock) {
            frameWidth = frame.getWidth();
            frameHeight = frame.getHeight();
        }
        if (frameWidth <= 0 || frameHeight <= 0) {
            return;
        }
        int scale = Math.max(1, Math.min(width / frameWidth, height / frameHeight));
        int scaledWidth = frameWidth * scale;
        int scaledHeight = frameHeight * scale;
        int left = (width - scaledWidth) / 2;
        int top = (height - scaledHeight) / 2;
        destination.set(left, top, left + scaledWidth, top + scaledHeight);
    }

    private void drawBootPlaceholder() {
        int[] pixels = new int[GB_WIDTH * GB_HEIGHT];
        for (int y = 0; y < GB_HEIGHT; y++) {
            for (int x = 0; x < GB_WIDTH; x++) {
                boolean grid = (x / 8 + y / 8) % 2 == 0;
                pixels[y * GB_WIDTH + x] = grid ? 0xFFE8F0D8 : 0xFFC7D9AF;
            }
        }
        frame.setPixels(pixels, 0, GB_WIDTH, 0, 0, GB_WIDTH, GB_HEIGHT);
    }
}
