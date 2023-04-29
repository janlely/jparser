package org.jay.parser.parsers;

import org.jay.parser.Combinator;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.CharUtil;
import org.jay.parser.util.ErrorUtil;
import org.jay.parser.util.Mapper;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TextParsers {

    public static Parser empty() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                return Result.builder()
                        .length(0)
                        .result(new ArrayList(0))
                        .build();
            }
        };
    }

    public static Parser one(char ch) {
        return one(ch, StandardCharsets.UTF_8);
    }

    public static Parser one(char ch, boolean ignoreCase) {
        if (ignoreCase) {
            return Combinator.choose(
                    one(Character.toLowerCase(ch), StandardCharsets.UTF_8),
                    one(Character.toUpperCase(ch), StandardCharsets.UTF_8)
            ).map(Mapper.replace(ch));
        }
        return one(ch, StandardCharsets.UTF_8);
    }

    public static Parser one(char ch, Charset charset) {
        return ByteParsers.bytes(String.valueOf(ch).getBytes(charset))
                .map(Mapper.toChar(charset));
    }

    public static Parser satisfy(Predicate<Character> p) {
        return satisfy(p, StandardCharsets.UTF_8);
    }

    public static Parser satisfy(Predicate<Character> p, Charset charset) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte[] bytes = buffer.headN(4);
                try {
                    char ch = CharUtil.read2(bytes, charset);
                    if (p.test(ch)) {
                        int len = String.valueOf(ch).getBytes(charset).length;
                        buffer.forward(len);
                        return Result.builder()
                                .result(List.of(ch))
                                .length(len)
                                .build();
                    }
                } catch (CharacterCodingException e) {
                    //Do Nothing
                }
                return Result.builder()
                        .errorMsg(ErrorUtil.error(buffer.getPos()))
                        .build();
            }
        };
    }

    public static Parser string(String value) {
        return string(value, StandardCharsets.UTF_8);
    }


    public static Parser string(String value, boolean ignoreCase) {
        if (!ignoreCase) {
            return string(value, StandardCharsets.UTF_8);
        }
        Parser result = empty();
        for(int i = 0; i < value.length(); i++) {
            result = result.connect(one(value.charAt(i), true));
        }
        return result.map(Mapper.replace(value));
    }

    public static Parser string(String value, Charset charset) {
        Parser result = empty();
        for(int i = 0; i < value.length(); i++) {
            result = result.connect(one(value.charAt(i), charset));
        }
        return result.map(Mapper.toStr());
    }

    public static Parser any() {
        return any(StandardCharsets.UTF_8);
    }

    public static Parser any(Charset charset) {
        return satisfy(__ -> true, charset);
    }

    public static Parser take(int n, Charset charset) {
        return any(charset).repeat(n).map(Mapper.toStr());
    }

    public static Parser take(int n) {
        return take(n, StandardCharsets.UTF_8);
    }

    public static Parser skip(int n, Charset charset) {
        return take(n, charset).ignore();
    }

    public static Parser skip(int n) {
        return skip(n, StandardCharsets.UTF_8);
    }

    public static Parser white() {
        return satisfy(c -> Character.isWhitespace(c)).ignore();
    }

    public static Parser blank() {
        return white().many();
    }

    public static Parser eof() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                if (buffer.remaining() > 0) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer.getPos()))
                            .build();
                }
                return Result.empty();
            }
        };
    }

}
