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
                    context.backward(data.length);
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
}
