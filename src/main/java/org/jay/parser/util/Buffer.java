package org.jay.parser.util;

import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

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

    public int offset(int n) {
        if (n > this.data.length - this.pos) {
            return this.data.length;
        }
        return this.pos + n;
    }

    public int remaining() {
        return data.length - pos;
    }

    public void jump(int pos) {
        this.pos = pos;
    }

    public Optional<Byte> head() {
        if (this.pos < this.data.length) {
            return Optional.of(this.data[this.pos]);
        }
        return Optional.empty();
    }

    public byte[] content() {
        byte[] bytes = new byte[data.length - pos];
        System.arraycopy(data, pos, bytes, 0, bytes.length);
        return bytes;
    }

    public byte[] headN(int n) {
        int len = offset(n) - this.pos;
        byte[] bytes = new byte[len];
        System.arraycopy(data, this.pos, bytes, 0, len);
        return bytes;
    }

    public void forward(int n) {
        this.pos += n;
    }
}
