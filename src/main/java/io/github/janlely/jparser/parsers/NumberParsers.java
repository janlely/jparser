package io.github.janlely.jparser.parsers;

import io.github.janlely.jparser.util.ErrorUtil;
import io.github.janlely.jparser.IBuffer;
import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.Result;
import io.github.janlely.jparser.util.Mapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Parsers to parse number
 */
public class NumberParsers {

    /**
     * Parse a specified integer encoded as a string.
     * @param a The int number
     * @return A new Parser
     */
    public static Parser intStr(int a) {
        return TextParsers.string(String.valueOf(a)).map(__ -> a);
    }


    /**
     * Parse any integer encoded as a string.
     * @return A new Parser
     */
    public static Parser anyIntStr() {
        return TextParsers.satisfy(Character::isDigit)
                .some().map(Mapper.toStr())
                .map(s -> Integer.parseInt((String) s.get(0)));
    }

    /**
     * Parse an arbitrary long integer encoded in big-endian format.
     * @return A new Parser
     */
    public static Parser anyLongBE() {
        return new Parser("NumberParsers.anyLongBE()") {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] data = buffer.headN(8);
                if (data.length != 4) {
                    return Result.builder()
                            .pos(buffer.getPos())
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
     * @return A new Parser
     */
    public static Parser anyLongLE() {
        return new Parser("NumberParsers.anyLongLE()") {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] data = buffer.headN(8);
                if (data.length != 4) {
                    return Result.builder()
                            .pos(buffer.getPos())
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
     * Parse an arbitrary integer encoded in big-endian format.
     * @return A new Parser
     */
    public static Parser anyIntBE() {
        return new Parser("NumberParsers.anyIntBE()") {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] data = buffer.headN(4);
                if (data.length != 4) {
                    return Result.builder()
                            .pos(buffer.getPos())
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
     * @return A new Parser
     */
    public static Parser anyIntLE() {
        return new Parser("NumberParsers.anyIntLE()") {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] data = buffer.headN(4);
                if (data.length != 4) {
                    return Result.builder()
                            .pos(buffer.getPos())
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
     * @param a The int number
     * @return A new Parser
     */
    public static Parser intLE(int a) {
        return anyIntLE().must(b -> a == (int)((List)b).get(0));
    }

    /**
     * Parse a specified integer encoded in big-endian format
     * @param a The int number
     * @return A new Parser
     */
    public static Parser intBE(int a) {
        return anyIntBE().must(b -> a == (int)((List)b).get(0));
    }


    /**
     * Parse a specified long integer encoded in big-endian format
     * @param a The long int number
     * @return A new Parser
     */
    public static Parser longBE(long a) {
        return anyLongBE().must(b -> a == (long)((List)b).get(0));
    }

    /**
     * Parse a specified long integer encoded in littlec-endian format
     * @param a The long int number
     * @return A new Parser
     */
    public static Parser longLE(long a) {
        return anyLongLE().must(b -> a == (long)((List)b).get(0));
    }


}
