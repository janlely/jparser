package org.jay.parser.util;

import lombok.Builder;
import lombok.Getter;

@Builder
public class Buffer {
    private byte[] data;
    @Getter
    private int pos;

    public void backward(int n) {
        if (this.pos < n) {
            throw new RuntimeException("unable to backward, hit top");
        }
        this.pos -= n;
    }

    public byte[] copyOf(int p, int length) {
        byte[] result = new byte[length];
        System.arraycopy(data, p, result, 0, length);
        return result;
    }

    public byte[] readN(int n) {
        if (n > this.data.length - this.pos) {
            n = this.data.length - this.pos;
        }
        byte[] result = new byte[n];
        System.arraycopy(data, this.pos, result, 0, n);
        this.pos += n;
        return result;
    }

    public String remaining() {
        return new String(data, pos, data.length - pos);
    }
}
