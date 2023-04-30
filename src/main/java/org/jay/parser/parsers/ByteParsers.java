package org.jay.parser.parsers;

import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.ErrorUtil;
import org.jay.parser.util.Mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ByteParsers {

    public static Parser bytes(byte[] data) {
        return bytes(data, "");
    }

    /**
     * Parse a specified character array.
     * @param data
     * @return
     */
    public static Parser bytes(byte[] data, String desc) {
        return new Parser(String.format("ByteParser.bytes<%s>", desc)) {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] bs = buffer.headN(data.length);
                if (!Arrays.equals(data, bs)) {
                    return Result.builder()
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
     * @param p
     * @return
     */
    public static Parser satisfy(Predicate<Byte> p) {
        return new Parser("ByteParser.satisfy()") {
            @Override
            public Result parse(IBuffer buffer) {
                Optional<Byte> b = buffer.head();
                if (b.isEmpty() || !p.test(b.get())) {
                    return Result.builder()
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
     * @return
     */
    public static Parser any() {
        return satisfy(__ -> true);
    }

    /**
     * Parse a given byte.
     * @param b
     * @return
     */
    public static Parser one(byte b) {
        return satisfy(a -> a == b);
    }


    /**
     * Parse n arbitrary bytes.
     * @param n
     * @return
     */
    public static Parser take(int n) {
        return any().repeat(n).map(Mapper.toBytes());
    }


    /**
     *
     * Skip n arbitrary bytes.
     * @param n
     * @return
     */
    public static Parser skip(int n) {
        return take(n).ignore();
    }
}
