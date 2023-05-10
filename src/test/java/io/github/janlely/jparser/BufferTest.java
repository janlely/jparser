package io.github.janlely.jparser;

import io.github.janlely.jparser.util.Buffer;
import org.junit.Test;

public class BufferTest {

    @Test
    public void testSplitAt() {
        Buffer buffer = Buffer.builder()
                .data("12345678".getBytes()).build();
        assert  buffer.splitAt(0)[0].remaining() == 0;
        assert  buffer.splitAt(0)[1].remaining() == 8;

        assert  buffer.splitAt(1)[0].remaining() == 1;
        assert  buffer.splitAt(1)[0].head().get() == '1';
        assert  buffer.splitAt(1)[1].remaining() == 7;

        assert  buffer.splitAt(2)[0].remaining() == 2;
        assert  buffer.splitAt(2)[0].headN(2)[0] == '1';
        assert  buffer.splitAt(2)[0].headN(2)[1] == '2';
        assert  buffer.splitAt(2)[1].remaining() == 6;

        assert  buffer.splitAt(8)[0].remaining() == 8;
        assert  buffer.splitAt(8)[1].remaining() == 0;

        buffer = Buffer.builder().data("aaaa".getBytes()).build();
        IBuffer[] tmp = buffer.splitAt(0);
        System.out.println("dd");
    }
}
