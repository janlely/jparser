package org.jay.parser.parsers;

import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.ErrorUtil;

import java.util.Arrays;
import java.util.List;

public class ByteParsers {

    public static Parser bytes(byte[] data) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                int pos = context.getPos();
                byte[] bs = context.readN(data.length);
                if (!Arrays.equals(data, bs)) {
                    context.jump(pos);
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(pos))
                            .build();
                }
                return Result.builder()
                        .length(data.length)
                        .result(List.of(data))
                        .build();
            }
        };
    }

    public static Parser skip(int n) {
        return take(n).ignore();
    }
    public static Parser take(int n) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                if (context.remaining() < n) {
                    return Result.builder()
                            .errorMsg("Not enough bytes to take")
                            .build();
                }
                return Result.builder().result(List.of(context.readN(n))).build();
            }
        };
    }
}
