package dev.vitorsilverio.gbcemu.audio;

import javax.sound.sampled.SourceDataLine;

final class SourceDataLineSink implements AudioSink {
    private static final int FRAME_SIZE = 4;
    private static final int BUFFER_SIZE = 131072;
    private static final int PREBUFFER_BYTES = 16384;
    private static final int WRITE_CHUNK_SIZE = 2048;

    private final SourceDataLine line;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private int readPosition;
    private int writePosition;
    private int size;
    private boolean primed;
    private boolean closed;

    SourceDataLineSink(SourceDataLine line) {
        this.line = line;
        Thread thread = new Thread(this::run, "gbcemu-audio");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public synchronized void write(byte[] source, int length) {
        if (closed) {
            return;
        }
        int written = 0;
        while (written < length) {
            while (!closed && size == BUFFER_SIZE) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (closed) {
                return;
            }

            int writable = Math.min(length - written, BUFFER_SIZE - size);
            int firstCopy = Math.min(writable, BUFFER_SIZE - writePosition);
            System.arraycopy(source, written, buffer, writePosition, firstCopy);
            int secondCopy = writable - firstCopy;
            if (secondCopy > 0) {
                System.arraycopy(source, written + firstCopy, buffer, 0, secondCopy);
            }
            writePosition = (writePosition + writable) % BUFFER_SIZE;
            size += writable;
            written += writable;
            notifyAll();
        }
    }

    private synchronized int read(byte[] destination) throws InterruptedException {
        while (!closed && !primed && size < PREBUFFER_BYTES) {
            wait();
        }
        if (closed) {
            return -1;
        }
        primed = true;

        int length = Math.min(destination.length, size);
        length -= length % FRAME_SIZE;
        if (length == 0) {
            if (size == 0) {
                primed = false;
            }
            return 0;
        }

        int firstCopy = Math.min(length, BUFFER_SIZE - readPosition);
        System.arraycopy(buffer, readPosition, destination, 0, firstCopy);
        int secondCopy = length - firstCopy;
        if (secondCopy > 0) {
            System.arraycopy(buffer, 0, destination, firstCopy, secondCopy);
        }
        readPosition = (readPosition + length) % BUFFER_SIZE;
        size -= length;
        notifyAll();
        return length;
    }

    @Override
    public synchronized void close() {
        closed = true;
        size = 0;
        readPosition = 0;
        writePosition = 0;
        primed = false;
        line.stop();
        line.flush();
        line.close();
        notifyAll();
    }

    private void run() {
        byte[] chunk = new byte[WRITE_CHUNK_SIZE];
        while (true) {
            try {
                int length = read(chunk);
                if (length < 0) {
                    return;
                }
                if (length > 0) {
                    line.write(chunk, 0, length);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
