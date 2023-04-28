package org.jay.parser;

import lombok.Builder;
import org.jay.parser.util.Buffer;

@Builder
public class Context {
    private Buffer buffer;

    public byte[] readN(int n) {
        return buffer.readN(n);
    }

    public void backward(int n) {
        if (n <= 0) {
            return;
        }
        buffer.backward(n);
    }
    public int getPos() {
        return buffer.getPos();
    }

    public byte[] copyOf(int pos, int length) {
        return buffer.copyOf(pos, length);
    }
}
