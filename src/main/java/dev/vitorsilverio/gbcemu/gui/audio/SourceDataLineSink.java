package dev.vitorsilverio.gbcemu.gui.audio;

import javax.sound.sampled.SourceDataLine;

final class SourceDataLineSink implements AudioSink {
    private static final int FRAME_SIZE = 4;
    private static final int WRITE_CHUNK_SIZE = 2048;

    private final SourceDataLine line;
    private final byte[] buffer;
    private final int prebufferBytes;
    private int readPosition;
    private int writePosition;
    private int size;
    private boolean primed;
    private boolean closed;

    SourceDataLineSink(SourceDataLine line, int bufferSize) {
        this.line = line;
        int alignedSize = Math.max(FRAME_SIZE * 4096, bufferSize);
        alignedSize -= alignedSize % FRAME_SIZE;
        this.buffer = new byte[alignedSize];
        this.prebufferBytes = Math.max(FRAME_SIZE * 512, Math.min(alignedSize / 2, line.getBufferSize() / 4));
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
            while (!closed && size == buffer.length) {
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

            int writable = Math.min(length - written, buffer.length - size);
            int firstCopy = Math.min(writable, buffer.length - writePosition);
            System.arraycopy(source, written, buffer, writePosition, firstCopy);
            int secondCopy = writable - firstCopy;
            if (secondCopy > 0) {
                System.arraycopy(source, written + firstCopy, buffer, 0, secondCopy);
            }
            writePosition = (writePosition + writable) % buffer.length;
            size += writable;
            written += writable;
            notifyAll();
        }
    }

    private synchronized int read(byte[] destination) throws InterruptedException {
        while (!closed && !primed && size < prebufferBytes) {
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

        int firstCopy = Math.min(length, buffer.length - readPosition);
        System.arraycopy(buffer, readPosition, destination, 0, firstCopy);
        int secondCopy = length - firstCopy;
        if (secondCopy > 0) {
            System.arraycopy(buffer, 0, destination, firstCopy, secondCopy);
        }
        readPosition = (readPosition + length) % buffer.length;
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

    @Override
    public String debugDescription() {
        return line.getFormat() + " nativeBuffer=" + line.getBufferSize() + " queueBuffer=" + buffer.length + " prebuffer=" + prebufferBytes;
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
