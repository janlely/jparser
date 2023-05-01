package com.jay.parser;

import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.junit.Test;

public class TextParsersTest {

    @Test
    public void testEmpty() {
        Result result = Parser.empty().runParser(Buffer.builder()
                .data("hello".getBytes())
                .build());
        assert  result.getResult().isEmpty();
    }

    @Test
    public void testOne() {
        Result result1 = TextParsers.one('a').runParser(Buffer.builder()
                .data("abc".getBytes())
                .build());
        Result result2 = TextParsers.one('b').runParser(Buffer.builder()
                .data("bc".getBytes())
                .build());
        Result result3 = TextParsers.one('a', true).runParser(Buffer.builder()
                .data("Abc".getBytes())
                .build());
        assert result1.<Character>get(0) == 'a';
        assert result2.<Character>get(0) == 'b';
        assert result3.<Character>get(0) == 'a';
    }


    @Test
    public void testSatisfy() {
        Result result1 = TextParsers.satisfy(c -> c >= 'A' && c <= 'z').runParser(Buffer.builder()
                .data("abc".getBytes())
                .build());
        Result result2 = TextParsers.satisfy(c -> c >= 'A' && c <= 'z').runParser(Buffer.builder()
                .data("1bc".getBytes())
                .build());
        Result result3 = TextParsers.satisfy(c -> c == '你').runParser(Buffer.builder()
                .data("你bc".getBytes())
                .build());
        assert result1.isSuccess();
        assert result1.<Character>get(0) == 'a';
        assert result2.isError();
        assert result3.isSuccess();
    }

    @Test
    public void testString() {
        Result result1 = TextParsers.string("hello").runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        Result result2 = TextParsers.string("hello", true).runParser(Buffer.builder()
                .data("Hello world".getBytes())
                .build());
        Result result3 = TextParsers.string("hello", true).runParser(Buffer.builder()
                .data("HeLLo world".getBytes())
                .build());
        assert result1.isSuccess();
        assert result1.<String>get(0).equals("hello");
        assert result2.isSuccess();
        assert result2.<String>get(0).equals("hello");
        assert result3.<String>get(0).equals("hello");
    }

    @Test
    public void testAny() {
        Result result1 = TextParsers.any().runParser(Buffer.builder()
                .data("hello".getBytes())
                .build());
        Result result2 = TextParsers.any().runParser(Buffer.builder()
                .data("".getBytes())
                .build());
        assert result1.isSuccess();
        assert result1.<Character>get(0) == 'h';
        assert result2.isError();
    }

    @Test
    public void testTake() {
        Result result1 = TextParsers.take(5).runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        Result result2 = TextParsers.take(1).runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        Result result3 = TextParsers.take(0).runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        Result result4 = TextParsers.take(20).runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        assert result1.isSuccess();
        assert result1.<String>get(0).equals("hello");
        assert result2.<String>get(0).equals("h");
        assert result3.isSuccess();
        assert result3.<String>get(0).equals("");
        assert result4.isError();
    }

    @Test
    public void testSkip() {
        Result result1 = TextParsers.skip(5).runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        Result result2 = TextParsers.skip(1).runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        Result result3 = TextParsers.skip(0).runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        Result result4 = TextParsers.skip(20).runParser(Buffer.builder()
                .data("hello world".getBytes())
                .build());
        assert result1.isSuccess();
        assert result1.isEmpty();
        assert result2.isEmpty();
        assert result3.isSuccess();
        assert result3.isEmpty();
        assert result4.isError();
    }

    @Test
    public void testSpace() {
        Result result1 = TextParsers.space().runParser(Buffer.builder()
                .data(" abdf".getBytes())
                .build());
        Result result2 = TextParsers.spaces().connect(() -> TextParsers.string("hello"))
                .runParser(Buffer.builder()
                        .data("    hello".getBytes())
                        .build());
        assert result1.isSuccess();
        assert result1.isEmpty();
        assert result2.isSuccess();
        assert result2.<String>get(0).equals("hello");
    }

    @Test
    public void testTrim() {
        Result result1 = TextParsers.string("hello")
                .trim(true).runParser(Buffer.builder()
                        .data("helloadd".getBytes())
                        .build());
        assert result1.isSuccess();
    }

    @Test
    public void testBlank() {
        Result result1 = TextParsers.spaces()
                .runParser(Buffer.builder()
                        .data("".getBytes())
                        .build());
        assert result1.isSuccess();
    }
}
