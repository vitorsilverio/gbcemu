package dev.vitorsilverio.gbcemu.audio;

import javax.sound.sampled.SourceDataLine;

final class SourceDataLineSink implements AudioSink {
    private static final int BUFFER_SIZE = 65536;
    private static final int PREBUFFER_BYTES = 4096;
    private static final int WRITE_CHUNK_SIZE = 512;

    private final SourceDataLine line;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private int readPosition;
    private int writePosition;
    private int size;
    private boolean primed;

    SourceDataLineSink(SourceDataLine line) {
        this.line = line;
        Thread thread = new Thread(this::run, "gbcemu-audio");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public synchronized void write(byte[] source, int length) {
        int written = 0;
        while (written < length) {
            while (size == BUFFER_SIZE) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            int writable = Math.min(length - written, BUFFER_SIZE - size);
            for (int i = 0; i < writable; i++) {
                buffer[writePosition] = source[written + i];
                writePosition = (writePosition + 1) % BUFFER_SIZE;
            }
            size += writable;
            written += writable;
            notifyAll();
        }
    }

    private synchronized int read(byte[] destination) throws InterruptedException {
        while (!primed && size < PREBUFFER_BYTES) {
            wait();
        }
        primed = true;

        int length = Math.min(destination.length, Math.min(size, line.available()));
        if (length <= 0) {
            if (size == 0) {
                primed = false;
            }
            return 0;
        }

        for (int i = 0; i < length; i++) {
            destination[i] = buffer[readPosition];
            readPosition = (readPosition + 1) % BUFFER_SIZE;
        }
        size -= length;
        notifyAll();
        return length;
    }

    private void run() {
        byte[] chunk = new byte[WRITE_CHUNK_SIZE];
        while (true) {
            try {
                int length = read(chunk);
                if (length > 0) {
                    line.write(chunk, 0, length);
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
