package com.jay.parser;

import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.impl.regex2.CharType;
import org.jay.parser.impl.regex2.GroupResult;
import org.jay.parser.impl.regex2.RParser;
import org.jay.parser.impl.regex2.RegexParser;
import org.jay.parser.impl.regex2.RepeatType;
import org.jay.parser.impl.regex2.ResultParser;
import org.jay.parser.impl.regex2.Token;
import org.jay.parser.impl.regex2.TokenType;
import org.jay.parser.parsers.NumberParsers;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.F;
import org.jay.parser.util.Mapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class RegexParser2Tests {

    @Test
    public void testToken() {
        //char
        Result result = new RegexParser().validToken().runParser(Buffer.builder().data("a".getBytes()).build());
        assert result.isSuccess();
        assert result.<RParser>get(0).getType() == RParser.ParserType.PARSER;
        //escape
        result = new RegexParser().validToken().runParser(Buffer.builder().data("\\+".getBytes()).build());
        assert result.isSuccess();
        assert result.<RParser>get(0).getType() == RParser.ParserType.PARSER;
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("+".getBytes()).build()).isSuccess();
        result = new RegexParser().validToken().runParser(Buffer.builder().data("\\?".getBytes()).build());
        assert result.isSuccess();
        assert result.<RParser>get(0).getType() == RParser.ParserType.PARSER;
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("?".getBytes()).build()).isSuccess();
        // [A-z]
        result = new RegexParser().validToken().runParser(Buffer.builder().data("[A-z]".getBytes()).build());
        assert result.isSuccess();
        assert result.<RParser>get(0).getType() == RParser.ParserType.PARSER;
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("A".getBytes()).build()).isSuccess();
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("B".getBytes()).build()).isSuccess();
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("C".getBytes()).build()).isSuccess();
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("Z".getBytes()).build()).isSuccess();
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("a".getBytes()).build()).isSuccess();
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("b".getBytes()).build()).isSuccess();
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("c".getBytes()).build()).isSuccess();
        assert result.<RParser>get(0).getParser().runParser(Buffer.builder().data("z".getBytes()).build()).isSuccess();
        // \n
        result = new RegexParser().validToken().runParser(Buffer.builder().data("\\1".getBytes()).build());
        assert result.isSuccess();
        assert result.<RParser>get(0).getType() == RParser.ParserType.QUOTE;
        assert result.<RParser>get(0).getQuoteId() == 1;
        //invalid
        result = new RegexParser().validToken().runParser(Buffer.builder().data("+".getBytes()).build());
        assert result.isError();
        // group
        result = new RegexParser().validToken().runParser(Buffer.builder().data("(a+)".getBytes()).build());
        assert result.isSuccess();
        assert result.<RParser>get(0).getType() == RParser.ParserType.GROUP;
        result = result.<RParser>get(0).getParser().runParser(Buffer.builder().data("aaaa".getBytes()).build());
        assert result.isSuccess();

    }

    @Test
    public void commonTest() {
        System.out.println(List.of(1,2,3).subList(3,3).size());
    }
}
