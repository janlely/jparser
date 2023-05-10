package io.github.janlely.jparser.util;

import lombok.Builder;
import lombok.Getter;
import io.github.janlely.jparser.IBuffer;

import java.util.Optional;

/**
 * byte array Buffer
 */
@Builder
public class Buffer implements IBuffer {
    private byte[] data;
    @Getter
    private int pos;

    @Override
    public void backward(int n) {
        if (this.pos < n) {
            throw new RuntimeException("unable to backward, hit top");
        }
        this.pos -= n;
    }

    @Override
    public int remaining() {
        return data.length - pos;
    }

    @Override
    public Optional<Byte> head() {
        if (this.pos < this.data.length) {
            return Optional.of(this.data[this.pos]);
        }
        return Optional.empty();
    }

    @Override
    public byte[] remainContent() {
        if (remaining() <= 0) {
            return new byte[0];
        }
        byte[] bytes = new byte[data.length - pos];
        System.arraycopy(data, pos, bytes, 0, bytes.length);
        return bytes;
    }

    @Override
    public byte[] headN(int n) {
        int len = remaining() < n ? remaining() : n;
        byte[] bytes = new byte[len];
        System.arraycopy(data, this.pos, bytes, 0, len);
        return bytes;
    }

    @Override
    public void forward(int n) {
        this.pos += n;
    }

    @Override
    public IBuffer[] splitAt(int idx) {
        if (idx < 0 || idx > remaining()) {
            throw new IndexOutOfBoundsException();
        }
        return new IBuffer[] {
                new SubBuffer(this.pos, this.pos + idx - 1, this.data),
                new SubBuffer(this.pos + idx, this.data.length - 1, this.data)
        };
    }

    /**
     * ths sub-buffer
     */
    public static class SubBuffer implements IBuffer {

        int start;
        int end;
        int p;
        byte[] data;

        /**
         * @param start start position
         * @param end ent position
         * @param data the data
         */
        public SubBuffer(int start, int end, byte[] data) {
            this.data = data;
            this.start = start;
            this.end = end;
            this.p = start;
        }

        @Override
        public void backward(int n) {
            if (this.p - n < start) {
                throw new RuntimeException("unable to backward, hit top");
            }
            this.p -= n;
        }

        @Override
        public int remaining() {
            return this.end - this.p + 1;
        }

        @Override
        public Optional<Byte> head() {
            if (this.p <= this.end) {
                return Optional.of(this.data[this.p]);
            }
            return Optional.empty();
        }

        @Override
        public byte[] headN(int n) {
            if (remaining() <= 0) {
                return new byte[0];
            }
            int len = remaining() < n ? remaining() : n;
            byte[] bytes = new byte[len];
            System.arraycopy(data, this.p, bytes, 0, len);
            return bytes;
        }

        @Override
        public void forward(int n) {
            this.p += n;
        }


        @Override
        public int getPos() {
            return this.p;
        }

        @Override
        public IBuffer[] splitAt(int idx) {
            if (idx < 0 || idx > remaining()) {
                throw new IndexOutOfBoundsException();
            }
            return new SubBuffer[] {
                    new SubBuffer(this.p, this.p + idx - 1, this.data),
                    new SubBuffer(this.p + idx, this.end, this.data)
            };
        }

        @Override
        public byte[] remainContent() {
            if (remaining() <= 0) {
                return new byte[0];
            }
            byte[] bytes = new byte[this.end - this.p + 1];
            System.arraycopy(data, this.p, bytes, 0, bytes.length);
            return bytes;
        }
    }
}
