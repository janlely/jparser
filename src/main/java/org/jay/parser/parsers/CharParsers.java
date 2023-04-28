package org.jay.parser.parsers;

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

public class CharParsers {


    public Parser character(AnyChar ch) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                byte[] encoded = ch.getCharacter();
                if (Arrays.equals(encoded, context.readN(encoded.length))) {
                    return Result.builder()
                            .length(encoded.length)
                            .result(List.of(ch)).build();
                }
                return Result.builder().errorMsg("Unexpected character at: " + context.getPos()).build();
            }
            @Override
            public boolean isIgnore() {
                return true;
            }
        };
    }

    public static Parser anyChar() {
        return anyChar(StandardCharsets.UTF_8);
    }

    public static Parser anyChar(Charset charset) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                try {
                    int maxLen = Math.round(charset.newEncoder().maxBytesPerChar());
                    AnyChar ch = CharUtil.read(context.readN(maxLen), charset);
                    return Result.builder()
                            .length(CharUtil.length(ch))
                            .result(List.of(ch)).build();
                } catch (CharacterCodingException e) {
                    return Result.builder().errorMsg("Unexpected character at: " + context.getPos()).build();
                }
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
                    return Result.builder().errorMsg("Unexpected character at: " + context.getPos()).build();
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
                    return Result.builder().errorMsg("Unexpected character at: " + context.getPos()).build();
                }
            }
        };
    }
}
