package org.jay.parser.parsers;

import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class StringParsers {

    public static Parser string(String s) {
        return string(s, StandardCharsets.UTF_8);
    }
    public static Parser string(String s, Charset charset) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                byte[] bs = s.getBytes(charset);
                byte[] bytes = context.readN(bs.length);
                if (Arrays.equals(bs, bytes)) {
                    return Result.builder()
                            .length(bs.length)
                            .result(List.of(s)).build();
                }
                return Result.builder()
                        .errorMsg("Unexpected character at: " + context.getPos() + ". expect: " + s)
                        .build();
            }
        };
    }
}
