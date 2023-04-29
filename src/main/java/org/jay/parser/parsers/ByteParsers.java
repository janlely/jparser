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

    public static Parser any() {
        return satisfy(__ -> true);
    }

    public static Parser one(byte b) {
        return satisfy(a -> a == b);
    }

    public static Parser take(int n) {
        return any().repeat(n).map(Mapper.toBytes());
    }

    public static Parser skip(int n) {
        return take(n).ignore();
    }
}
