package org.jay.parser.parsers;

import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.CharUtil;
import org.jay.parser.util.ErrorUtil;
import org.jay.parser.util.Mapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Parsers to parse text
 */
public class TextParsers {


    /**
     * Parse a character using UTF-8 encoding.
     * @param ch The char
     * @return A new Parser
     */
    public static Parser one(char ch) {
        return one(ch, StandardCharsets.UTF_8);
    }

    /**
     * Parse a character, ignoring case.
     * @param ch The char
     * @param ignoreCase ignore case or not
     * @return A new Parser
     */
    public static Parser one(char ch, boolean ignoreCase) {
        if (ignoreCase) {
            return one(Character.toLowerCase(ch), StandardCharsets.UTF_8).or(() ->
                    one(Character.toUpperCase(ch), StandardCharsets.UTF_8)
            ).map(Mapper.replace(ch));
        }
        return one(ch, StandardCharsets.UTF_8);
    }

    /**
     * Parse a character according to the given encoding
     * @param ch The char
     * @param charset The charset
     * @return A new Parser
     */
    public static Parser one(char ch, Charset charset) {
        return ByteParsers.bytes(String.valueOf(ch).getBytes(charset), String.format("== '%c'", ch))
                .map(Mapper.toChar(charset), "byte[]", "char");
    }

    /**
     * Parse a character that satisfies a condition , using UTF-8 encoding.
     * @param predicate The predicate
     * @return A new Parser
     */
    public static Parser satisfy(Predicate<Character> predicate) {
        return satisfy(predicate, StandardCharsets.UTF_8, "");
    }

    /**
     * Parse a character that satisfies a condition , using UTF-8 encoding.
     * @param predicate The predicate
     * @param desc  The description
     * @return A new Parser
     */
    public static Parser satisfy(Predicate<Character> predicate, String desc) {
        return satisfy(predicate, StandardCharsets.UTF_8, desc);
    }

    /**
     * Parse a character that satisfies a condition according to the given encoding.
     * @param predicate The predicate
     * @param charset The charset
     * @param desc The description
     * @return A new Parser
     */
    public static Parser satisfy(Predicate<Character> predicate, Charset charset, String desc) {
        return new Parser(String.format("TextParser.satisfy<%s>", desc)) {
            @Override
            public Result parse(IBuffer buffer) {
                byte[] bytes = buffer.headN(4);
                Optional<Character> ch = CharUtil.read(bytes, charset);
                if (ch.isPresent() && predicate.test(ch.get())) {
                    int len = String.valueOf(ch.get()).getBytes(charset).length;
                    buffer.forward(len);
                    return Result.builder()
                            .result(Collections.singletonList(ch.get()))
                            .length(len)
                            .build();
                }
                return Result.builder()
                        .errorMsg(ErrorUtil.error(buffer))
                        .build();
            }
        };
    }

    /**
     * Parse a given string.
     * @param value The string value
     * @return A new Parser
     */
    public static Parser string(String value) {
        return string(value, StandardCharsets.UTF_8);
    }


    /**
     * Parse a given string, ignoring case.
     * @param value The string value
     * @param ignoreCase ignore case or not
     * @return A new Parser
     */
    public static Parser string(String value, boolean ignoreCase) {
        if (!ignoreCase) {
            return string(value, StandardCharsets.UTF_8);
        }
        Parser result = Parser.empty();
        for(int i = 0; i < value.length(); i++) {
            int idx = i;
            result = result.chain(() -> one(value.charAt(idx), true));
        }
        return result.map(Mapper.replace(value));
    }

    /**
     * Parse a given string according to the given encoding.
     * @param value The string value
     * @param charset The charset
     * @return A new Parser
     */
    public static Parser string(String value, Charset charset) {
        Parser result = Parser.empty();
        for(int i = 0; i < value.length(); i++) {
            int idx = i;
            result = result.chain(() -> one(value.charAt(idx), charset));
        }
        return result.map(Mapper.toStr());
    }

    /**
     * Parse any character.
     * @return A new Parser
     */
    public static Parser any() {
        return any(StandardCharsets.UTF_8);
    }

    /**
     * Parse any UTF-8 character.
     * @param charset The charset
     * @return A new Parser
     */
    public static Parser any(Charset charset) {
        return satisfy(__ -> true, charset, String.format("any char of %s", charset.name()));
    }

    /**
     * Parse any n characters according to the specified encoding.
     * @param n The char count
     * @param charset The charset
     * @return A new Parser
     */
    public static Parser take(int n, Charset charset) {
        return any(charset).repeat(n).map(Mapper.toStr());
    }

    /**
     * Parse n UTF-8 characters.
     * @param n The char count
     * @return A new Parser
     */
    public static Parser take(int n) {
        return take(n, StandardCharsets.UTF_8);
    }

    /**
     * Skip n characters of the given encoding.
     * @param n The char count
     * @param charset The charset
     * @return A new Parser
     */
    public static Parser skip(int n, Charset charset) {
        return take(n, charset).ignore();
    }

    /**
     * Skip n UTF-8 characters.
     * @param n The char count
     * @return A new Parser
     */
    public static Parser skip(int n) {
        return skip(n, StandardCharsets.UTF_8);
    }


    /**
     * Parse characters that satisfy a condition and return a string, using UTF-8
     * @param predicate The predicate
     * @return A new Parser
     */
    public static Parser takeWhile(Predicate<Character> predicate) {
        return takeWhile(predicate, "");
    }

    /**
     * @param predicate The predicate
     * @param desc The description
     * @return A new Parser
     */
    public static Parser takeWhile(Predicate<Character> predicate, String desc) {
        return takeWhile(predicate, StandardCharsets.UTF_8, desc);
    }

    /**
     * Parse characters that satisfy a condition according to the given encoding and return a string.
     * @param predicate the predicate
     * @param charset the charset
     * @param desc the description
     * @return A new Parser
     */
    public static Parser takeWhile(Predicate<Character> predicate, Charset charset, String desc) {
        return satisfy(predicate, charset, desc).many().map(Mapper.toStr());
    }

    /**
     * Skip characters that satisfy a condition according to the given encoding
     * @param predicate The predicate
     * @return A new Parser
     */
    public static Parser skipWhile(Predicate<Character> predicate) {
        return takeWhile(predicate, StandardCharsets.UTF_8, "").ignore();
    }

    /**
     * Skip characters that satisfy a condition according to the given encoding
     * @param predicate The predicate
     * @param desc the description
     * @return A new Parser
     */
    public static Parser skipWhile(Predicate<Character> predicate, String desc) {
        return takeWhile(predicate, StandardCharsets.UTF_8, desc).ignore();
    }
    /**
     * Skip characters that satisfy a condition and UTF-8 characters.
     * @param predicate The predicate
     * @param charset The charset
     * @return A new Parser
     */
    public static Parser skipWhile(Predicate<Character> predicate, Charset charset) {
        return takeWhile(predicate, charset, "").ignore();
    }

    /**
     * Skip characters that satisfy a condition and UTF-8 characters.
     * @param predicate The predicate
     * @param charset The charset
     * @param desc the description
     * @return A new Parser
     */
    public static Parser skipWhile(Predicate<Character> predicate, Charset charset, String desc) {
        return takeWhile(predicate, charset, desc).ignore();
    }

    /**
     * Parse whitespace characters, excluding newline characters.
     * @return A new Parser
     */
    public static Parser space() {
        return satisfy(Character::isSpaceChar, "space exclude newline").ignore();
    }

    /**
     * Parse a sequence of whitespace.
     * @return A new Parser
     */
    public static Parser spaces() {
        return space().many();
    }

    /**
     * Parse a whitespace include newline
     * @return A new Parser
     */
    public static Parser white() {
        return satisfy(Character::isWhitespace, "all white").ignore();
    }

    /**
     * Parse a sequence of whitespace include newline
     * @return A new Parser
     */
    public static Parser whites() {
        return white().many();
    }

    /**
     * Parse EOF (end-of-file).
     * @return A new Parser
     */
    public static Parser eof() {
        return new Parser("TextParser.eof()") {
            @Override
            public Result parse(IBuffer buffer) {
                if (buffer.remaining() > 0) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer))
                            .build();
                }
                return Result.empty();
            }
        };
    }

}
