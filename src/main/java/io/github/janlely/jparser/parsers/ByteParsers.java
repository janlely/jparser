package io.github.janlely.jparser.parsers;

import io.github.janlely.jparser.IBuffer;
import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.Result;
import io.github.janlely.jparser.util.ErrorUtil;
import io.github.janlely.jparser.util.Mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Parsers to parse bytes
 */
public class ByteParsers {

    /**
     * Parse a specified character array.
     * @param data bytes to be parsed
     * @return A Parser
     */
    public static Parser bytes(byte[] data) {
        return bytes(data, "");
    }

    /**
     * Parse a specified character array.
     * @param data bytes to be parsed
     * @param desc the description
     * @return A Parser
     */
    public static Parser bytes(byte[] data, String desc) {
        return new Parser() {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] bs = buffer.headN(data.length);
                if (!Arrays.equals(data, bs)) {
                    return Result.builder()
                            .pos(buffer.getPos())
                            .errorMsg(ErrorUtil.error(buffer))
                            .build();
                }
                buffer.forward(bs.length);
                return Result.builder()
                        .length(data.length)
                        .result(List.of(data))
                        .build();
            }
        };
    }

    /**
     *
     * Parse a byte that satisfies a condition.
     * @param predicate the predicate
     * @return A new Parser
     */
    public static Parser satisfy(Predicate<Byte> predicate) {
        return new Parser() {
            @Override
            public Result parse(IBuffer buffer) {
                Optional<Byte> b = buffer.head();
                if (b.isEmpty() || !predicate.test(b.get())) {
                    return Result.builder()
                            .pos(buffer.getPos())
                            .errorMsg(ErrorUtil.error(buffer))
                            .build();
                }
                buffer.forward(1);
                return Result.builder()
                        .result(List.of(b))
                        .length(1)
                        .build();
            }
        };
    }

    /**
     * Parse any byte.
     * @return A new Parser
     */
    public static Parser any() {
        return satisfy(__ -> true);
    }

    /**
     * Parse a given byte.
     * @param b The byte value
     * @return A new Parser
     */
    public static Parser one(byte b) {
        return satisfy(a -> a == b);
    }


    /**
     * Parse n arbitrary bytes.
     * @param n byte counts
     * @return A new Parser
     */
    public static Parser take(int n) {
        return any().repeat(n).map(Mapper.toBytes());
    }

    /**
     * @param predicate The predicate
     * @return A new Parser
     */
    public static Parser takeWhile(Predicate<Byte> predicate) {
        return satisfy(predicate).many().map(Mapper.toBytes());
    }


    /**
     * Skip n arbitrary bytes.
     * @param n byte counts
     * @return A new Parser
     */
    public static Parser skip(int n) {
        return take(n).ignore();
    }

    /**
     * @param predicate The predicate
     * @return A new Parser
     */
    public static Parser skipWhile(Predicate<Byte> predicate) {
        return satisfy(predicate).many().ignore();
    }
}
