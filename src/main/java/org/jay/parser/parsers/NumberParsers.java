package org.jay.parser.parsers;

import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.ErrorUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class NumberParsers {

    /**
     * Parse a specified integer encoded as a string.
     * @param a
     * @return
     */
    public static Parser intStr(int a) {
        return TextParsers.string(String.valueOf(a)).map(__ -> a);
    }

    /**
     * Parse an arbitrary long integer encoded in big-endian format.
     * @return
     */
    public static Parser anyLongBE() {
        return new Parser("NumberParsers.anyLongBE()") {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] data = buffer.headN(8);
                if (data.length != 4) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer))
                            .build();
                }
                buffer.forward(8);
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.order(ByteOrder.BIG_ENDIAN);
                return Result.builder()
                        .length(8)
                        .result(List.of(bf.getLong())).build();
            }
        };
    }

    /**
     * Parse an arbitrary long integer encoded in little-endian format.
     * @return
     */
    public static Parser anyLongLE() {
        return new Parser("NumberParsers.anyLongLE()") {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] data = buffer.headN(8);
                if (data.length != 4) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer))
                            .build();
                }
                buffer.forward(8);
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.order(ByteOrder.LITTLE_ENDIAN);
                return Result.builder()
                        .length(8)
                        .result(List.of(bf.getLong())).build();
            }
        };
    }

    /**
     *
     * Parse an arbitrary integer encoded in big-endian format.
     * @return
     */
    public static Parser anyIntBE() {
        return new Parser("NumberParsers.anyIntBE()") {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] data = buffer.headN(4);
                if (data.length != 4) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer))
                            .build();
                }
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.order(ByteOrder.BIG_ENDIAN);
                buffer.forward(4);
                return Result.builder()
                        .length(4)
                        .result(List.of(bf.getInt())).build();
            }
        };
    }

    /**
     * Parse an arbitrary integer encoded in little-endian format.
     * @return
     */
    public static Parser anyIntLE() {
        return new Parser("NumberParsers.anyIntLE()") {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] data = buffer.headN(4);
                if (data.length != 4) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer))
                            .build();
                }
                buffer.forward(4);
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.order(ByteOrder.LITTLE_ENDIAN);
                return Result.builder()
                        .length(4)
                        .result(List.of(bf.getInt())).build();
            }
        };
    }

    /**
     * Parse a specified integer encoded in little-endian format
     * @param a
     * @return
     */
    public static Parser intLE(int a) {
        return anyIntLE().must(b -> a == (int)((List)b).get(0));
    }

    /**
     * Parse a specified integer encoded in big-endian format
     * @param a
     * @return
     */
    public static Parser intBE(int a) {
        return anyIntBE().must(b -> a == (int)((List)b).get(0));
    }


    /**
     * Parse a specified long integer encoded in big-endian format
     * @param a
     * @return
     */
    public static Parser longBE(long a) {
        return anyLongBE().must(b -> a == (long)((List)b).get(0));
    }

    /**
     * Parse a specified long integer encoded in littlec-endian format
     * @param a
     * @return
     */
    public static Parser longLE(long a) {
        return anyLongLE().must(b -> a == (long)((List)b).get(0));
    }


}
