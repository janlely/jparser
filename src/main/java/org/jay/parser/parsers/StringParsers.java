package org.jay.parser.parsers;

import org.apache.commons.lang3.StringUtils;
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
                int pos = context.getPos();
                byte[] bs = s.getBytes(charset);
                byte[] bytes = context.readN(bs.length);
                if (Arrays.equals(bs, bytes)) {
                    return Result.builder()
                            .length(bs.length)
                            .result(List.of(s)).build();
                }
                int curPos = context.getPos();
                context.jump(pos);
                return Result.builder()
                        .errorMsg("Unexpected character at: " + curPos + ". expect: " + s)
                        .build();
            }
        };
    }

    public static Parser string(String s, boolean ignoreCase) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                int pos = context.getPos();
                byte[] bytes = context.readN(s.length());
                if (ignoreCase) {
                    if(StringUtils.equalsIgnoreCase(s, new String(bytes))) {
                        return Result.builder()
                                .length(s.length())
                                .result(List.of(new String(bytes))).build();
                    }
                }else {
                    if(StringUtils.equals(s, new String(bytes))) {
                        return Result.builder()
                                .length(s.length())
                                .result(List.of(new String(bytes))).build();
                    }
                }
                int curPos = context.getPos();
                context.jump(pos);
                return Result.builder()
                        .errorMsg("Unexpected character at: " + curPos + ". expect: " + s)
                        .build();
            }
        };
    }

}
