package dev.vitorsilverio.gbcemu.link;

import java.util.Optional;
import java.util.function.Consumer;

final class LinkCableFrameParser {

    private static final int PARSE_BUFFER_CAPACITY = 512;

    private final byte[] parseBuffer = new byte[PARSE_BUFFER_CAPACITY];
    private int parseLength;

    void append(byte[] data, Consumer<LinkCableFrame.ParsedFrame> frameConsumer) {
        if (data == null || data.length == 0) {
            return;
        }
        if (data.length > parseBuffer.length) {
            parseLength = 0;
            return;
        }
        if (parseLength + data.length > parseBuffer.length) {
            parseLength = 0;
        }
        System.arraycopy(data, 0, parseBuffer, parseLength, data.length);
        parseLength += data.length;
        drain(frameConsumer);
    }

    void reset() {
        parseLength = 0;
    }

    private void drain(Consumer<LinkCableFrame.ParsedFrame> frameConsumer) {
        while (parseLength > 0) {
            Optional<LinkCableFrame.ParsedFrame> linkFrame = LinkCableFrame.tryParse(parseBuffer, parseLength);
            if (linkFrame.isPresent()) {
                LinkCableFrame.ParsedFrame frame = linkFrame.get();
                if (frame.frameLength() > parseLength) {
                    parseLength = 0;
                    return;
                }
                frameConsumer.accept(frame);
                discard(frame.frameLength());
                continue;
            }

            if (parseBuffer[0] != LinkCableFrame.MAGIC || LinkCableFrame.hasCompleteFrameHeader(parseBuffer, parseLength)) {
                discard(1);
                continue;
            }

            return;
        }
    }

    private void discard(int length) {
        int remaining = parseLength - length;
        if (remaining > 0) {
            System.arraycopy(parseBuffer, length, parseBuffer, 0, remaining);
        }
        parseLength = Math.max(remaining, 0);
    }
}
