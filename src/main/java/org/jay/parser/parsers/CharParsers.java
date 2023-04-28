package org.jay.parser.parsers;

import lombok.extern.slf4j.Slf4j;
import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.AnyChar;
import org.jay.parser.util.CharUtil;
import org.jay.parser.util.ErrorUtil;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
public class CharParsers {


    public static Parser character(AnyChar ch) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                int pos = context.getPos();
                byte[] encoded = ch.getCharacter();
                byte[] bytes = context.readN(encoded.length);
                if (Arrays.equals(encoded, bytes)) {
                    return Result.builder()
                            .length(encoded.length)
                            .result(List.of(ch)).build();
                }
                int curPos = context.getPos();
                context.jump(pos);
                return Result.builder().errorMsg("Unexpected character at: " + curPos).build();
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
                int pos = context.getPos();
                try {
                    int maxLen = Math.round(charset.newEncoder().maxBytesPerChar());
                    byte[] bytes = context.readN(maxLen);
                    AnyChar ch = CharUtil.read(bytes, charset);
                    return Result.builder()
                            .length(CharUtil.length(ch))
                            .result(List.of(ch)).build();
                } catch (CharacterCodingException e) {
                    int curPos = context.getPos();
                    context.jump(pos);
                    return Result.builder().errorMsg("Unexpected character at: " + curPos).build();
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
                    AnyChar ch = CharUtil.read(bytes, charset);
                    if(!ch.isEmpty() && cond.test(charset.decode(ByteBuffer.wrap(ch.getCharacter())).charAt(0))) {
                        int len = ch.getCharacter().length;
                        context.backward(bytes.length - len);
                        return Result.builder()
                                .length(len)
                                .result(List.of(new String(context.copyOf(pos,len), charset))).build();
                    }
                } catch (CharacterCodingException e) {
                }
                int curPos = context.getPos();
                context.jump(pos);
                return Result.builder().errorMsg("Unexpected character at: " + curPos).build();
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

    public static Parser skip(int n, Charset charset) {
        return take(n, charset).ignore();
    }

    public static Parser take(int n, Charset charset) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                int pos = context.getPos();
                int maxLen = context.remaining() >= CharUtil.CHAR_MAN_LENGTH * n ? CharUtil.CHAR_MAN_LENGTH * n : context.remaining();
                try {
                    List<AnyChar> result = CharUtil.readN(context.readN(maxLen), n, charset);
                    Integer len = result.stream().map(AnyChar::getCharacter).map(c -> c.length).reduce(0, Integer::sum);
                    if (result.size() == n) {
                        byte[] bytes = new byte[len];
                        int i = 0;
                        for (AnyChar anyChar : result) {
                            System.arraycopy(anyChar.getCharacter(), 0, bytes, i, anyChar.getCharacter().length);
                            i += anyChar.getCharacter().length;
                        }
                        context.jump(pos + len);
                        return Result.builder()
                                .length(len)
                                .result(List.of(new String(bytes, charset))).build();
                    }
                    context.jump(pos);
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(pos + len))
                            .build();
                } catch (CharacterCodingException e) {
                    context.jump(pos);
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
