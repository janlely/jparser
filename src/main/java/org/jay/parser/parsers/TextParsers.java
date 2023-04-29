package org.jay.parser.parsers;

import org.jay.parser.Combinator;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.CharUtil;
import org.jay.parser.util.ErrorUtil;
import org.jay.parser.util.Mapper;

import java.awt.image.MultiPixelPackedSampleModel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TextParsers {

    /**
     * Do nothing but return success
     * @return
     */
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

    /**
     * Parse a character using UTF-8 encoding.
     * @param ch
     * @return
     */
    public static Parser one(char ch) {
        return one(ch, StandardCharsets.UTF_8);
    }

    /**
     * Parse a character, ignoring case.
     * @param ch
     * @param ignoreCase
     * @return
     */
    public static Parser one(char ch, boolean ignoreCase) {
        if (ignoreCase) {
            return Combinator.choose(
                    one(Character.toLowerCase(ch), StandardCharsets.UTF_8),
                    one(Character.toUpperCase(ch), StandardCharsets.UTF_8)
            ).map(Mapper.replace(ch));
        }
        return one(ch, StandardCharsets.UTF_8);
    }

    /**
     * Parse a character according to the given encoding
     * @param ch
     * @param charset
     * @return
     */
    public static Parser one(char ch, Charset charset) {
        return ByteParsers.bytes(String.valueOf(ch).getBytes(charset))
                .map(Mapper.toChar(charset));
    }

    /**
     * Parse a character that satisfies a condition , using UTF-8 encoding.
     * @param p
     * @return
     */
    public static Parser satisfy(Predicate<Character> p) {
        return satisfy(p, StandardCharsets.UTF_8);
    }

    /**
     * Parse a character that satisfies a condition according to the given encoding.
     * @param p
     * @param charset
     * @return
     */
    public static Parser satisfy(Predicate<Character> p, Charset charset) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte[] bytes = buffer.headN(4);
                try {
                    char ch = CharUtil.read(bytes, charset);
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

    /**
     * Parse a given string.
     * @param value
     * @return
     */
    public static Parser string(String value) {
        return string(value, StandardCharsets.UTF_8);
    }


    /**
     * Parse a given string, ignoring case.
     * @param value
     * @param ignoreCase
     * @return
     */
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

    /**
     * Parse a given string according to the given encoding.
     * @param value
     * @param charset
     * @return
     */
    public static Parser string(String value, Charset charset) {
        Parser result = empty();
        for(int i = 0; i < value.length(); i++) {
            result = result.connect(one(value.charAt(i), charset));
        }
        return result.map(Mapper.toStr());
    }

    /**
     * Parse any character.
     * @return
     */
    public static Parser any() {
        return any(StandardCharsets.UTF_8);
    }

    /**
     * Parse any UTF-8 character.
     * @param charset
     * @return
     */
    public static Parser any(Charset charset) {
        return satisfy(__ -> true, charset);
    }

    /**
     * Parse any n characters according to the specified encoding.
     * @param n
     * @param charset
     * @return
     */
    public static Parser take(int n, Charset charset) {
        return any(charset).repeat(n).map(Mapper.toStr());
    }

    /**
     * Parse n UTF-8 characters.
     * @param n
     * @return
     */
    public static Parser take(int n) {
        return take(n, StandardCharsets.UTF_8);
    }

    /**
     * Skip n characters of the given encoding.
     * @param n
     * @param charset
     * @return
     */
    public static Parser skip(int n, Charset charset) {
        return take(n, charset).ignore();
    }

    /**
     * Skip n UTF-8 characters.
     * @param n
     * @return
     */
    public static Parser skip(int n) {
        return skip(n, StandardCharsets.UTF_8);
    }


    /**
     * Parse characters that satisfy a condition and return a string, using UTF-8
     * @param p
     * @return
     */
    public static Parser takeWhile(Predicate<Character> p) {
        return takeWhile(p, StandardCharsets.UTF_8);
    }

    /**
     * Parse characters that satisfy a condition according to the given encoding and return a string.
     * @param p
     * @return
     */
    public static Parser takeWhile(Predicate<Character> p, Charset charset) {
        return satisfy(p, charset).many().map(Mapper.toStr());
    }

    /**
     * Skip characters that satisfy a condition according to the given encoding
     * @param p
     * @return
     */
    public static Parser skipWhile(Predicate<Character> p) {
        return takeWhile(p, StandardCharsets.UTF_8).ignore();
    }

    /**
     * Skip characters that satisfy a condition and UTF-8 characters.
     * @param p
     * @param charset
     * @return
     */
    public static Parser skipWhile(Predicate<Character> p, Charset charset) {
        return takeWhile(p, charset).ignore();
    }

    /**
     * Parse a whitespace character, including spaces, tabs, line feeds, etc.
     * @return
     */
    public static Parser white() {
        return satisfy(c -> Character.isWhitespace(c)).ignore();
    }

    /**
     * Parse a sequence of whitespace.
     * @return
     */
    public static Parser blank() {
        return white().many();
    }

    /**
     * Parse EOF (end-of-file).
     * @return
     */
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
