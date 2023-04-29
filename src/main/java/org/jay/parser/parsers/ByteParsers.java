package org.jay.parser.parsers;

import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.ErrorUtil;
import org.jay.parser.util.Mapper;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ByteParsers {

    /**
     * Parse a specified character array.
     * @param data
     * @return
     */
    public static Parser bytes(byte[] data) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte[] bs = buffer.headN(data.length);
                if (!Arrays.equals(data, bs)) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer.getPos()))
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
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte b = buffer.head();
                if (!p.test(b)) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer.getPos()))
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
