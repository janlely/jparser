package org.jay.parser.parsers;

import org.apache.commons.lang3.StringUtils;
import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.AnyChar;
import org.jay.parser.util.CharUtil;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

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

    public static Parser satisfy(Predicate<Character> cond) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                int pos = context.getPos();
                Charset charset = StandardCharsets.UTF_8;
                try {
                    byte[] bytes = context.readN(CharUtil.CHAR_MAN_LENGTH);
                    int maxLen = bytes.length == CharUtil.CHAR_MAN_LENGTH ? CharUtil.CHAR_MAN_LENGTH: bytes.length;
                    AnyChar ch = CharUtil.read(bytes, charset);
                    while(!ch.isEmpty() && cond.test(charset.decode(ByteBuffer.wrap(ch.getCharacter())).charAt(0))) {
                        context.backward(maxLen - CharUtil.length(ch));
                        bytes = context.readN(CharUtil.CHAR_MAN_LENGTH);
                        maxLen = bytes.length == CharUtil.CHAR_MAN_LENGTH ? CharUtil.CHAR_MAN_LENGTH: bytes.length;
                        ch = CharUtil.read(bytes, charset);
                    }
                    context.backward(maxLen);
                    int len = context.getPos() - pos;
                    return Result.builder()
                            .length(len)
                            .result(List.of(new String(context.copyOf(pos,len), charset))).build();
                } catch (CharacterCodingException e) {
                    int curPos = context.getPos();
                    context.jump(pos);
                    return Result.builder().errorMsg("Unexpected character at: " + curPos).build();
                }
            }
        };
    }

    public static Parser satisfy(Predicate<AnyChar> cond, Charset charset) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                int pos = context.getPos();
                try {
                    AnyChar ch = CharUtil.read(context.readN(CharUtil.CHAR_MAN_LENGTH), charset);
                    while(cond.test(ch)) {
                        context.backward(CharUtil.CHAR_MAN_LENGTH - CharUtil.length(ch));
                        ch = CharUtil.read(context.readN(4), charset);
                    }
                    context.backward(CharUtil.CHAR_MAN_LENGTH);
                    int len = context.getPos() - pos;
                    return Result.builder()
                            .length(len)
                            .result(List.of(new String(context.copyOf(pos,len), charset))).build();
                } catch (CharacterCodingException e) {
                    int curPos = context.getPos();
                    context.jump(pos);
                    return Result.builder().errorMsg("Unexpected character at: " + curPos).build();
                }
            }
        };
    }
}
