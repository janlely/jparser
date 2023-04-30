package com.jay.parser;

import org.jay.parser.Result;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.Mapper;
import org.junit.Test;

public class BaseTests {

    @Test
    public void testMust() {
        Result result1 = TextParsers.one('c')
                .must(r -> r.<Character>get(0) == 'c')
                .runParser(Buffer.builder().data("cab".getBytes()).build());
        assert result1.isSuccess();
        Result result2 = TextParsers.one('c')
                .must(r -> r.<Character>get(0) == 'b')
                .runParser(Buffer.builder().data("cab".getBytes()).build());
        assert result2.isError();
    }

    @Test
    public void testSome() {
        Result result1 = TextParsers.one('a').some().map(Mapper.toStr())
                .runParser(Buffer.builder().data("aaabcd".getBytes()).build());
        assert result1.<String>get(0).equals("aaa");
        Result result2 = TextParsers.one('a').some().map(Mapper.toStr())
                .runParser(Buffer.builder().data("bcd".getBytes()).build());
        assert result2.isError();
    }

    @Test
    public void testMany() {
//        Result result1 = TextParsers.one('a').many().map(Mapper.toStr())
//                .runParser(Buffer.builder().data("aaabcd".getBytes()).build());
//        assert result1.<String>get(0).equals("aaa");
//        Result result2 = TextParsers.one('a').many().map(Mapper.toStr())
//                .runParser(Buffer.builder().data("bcd".getBytes()).build());
//        assert result2.isSuccess();
//        assert result2.<String>get(0).equals("");

        Result result3 = TextParsers.satisfy(Character::isLetter, "Character::isLetter")
                .many().map(Mapper.toStr(), "List<Char>", "string")
                .sepBy(TextParsers.one(',').ignore())
//                .many()
                .runParser(Buffer.builder()
                        .data("a,b".getBytes())
                        .build());
        assert result3.isSuccess();
        assert result3.<String>get(0).equals("a");
        assert result3.<String>get(1).equals("b");
    }

    @Test
    public void testOptinal() {
        Result result1 = TextParsers.one('a').optional()
                .runParser(Buffer.builder().data("hello".getBytes()).build());
        assert result1.isSuccess();
        assert result1.isEmpty();
    }

    @Test
    public void testRange() {
        Result result1 = TextParsers.one('a').range(1,3)
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("aaaahello".getBytes()).build());
        assert result1.isSuccess();
        assert result1.<String>get(0).equals("aaa");
    }

    @Test
    public void testAttempt() {
        Result result1 = TextParsers.one('a').attempt(5)
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("aaaahello".getBytes()).build());
        assert result1.isSuccess();
        assert result1.<String>get(0).equals("aaaa");
    }

    @Test
    public void testRepeat() {
        Result result1 = TextParsers.one('a').repeat(5)
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("aaaahello".getBytes()).build());
        assert result1.isEmpty();
    }


}
