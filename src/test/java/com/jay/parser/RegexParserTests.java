package com.jay.parser;

import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.impl.regex.CharType;
import org.jay.parser.impl.regex.RegexParser;
import org.jay.parser.impl.regex.RepeatType;
import org.jay.parser.impl.regex.Token;
import org.jay.parser.impl.regex.TokenType;
import org.jay.parser.util.Buffer;
import org.junit.Test;

import java.util.Optional;

public class RegexParserTests {

    @Test
    public void testToken() {
        //char
        Result result = RegexParser.token().runParser(Buffer.builder().data("a".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.VALID_CHAR;
        //DOT
        result = RegexParser.token().runParser(Buffer.builder().data(".".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.VALID_CHAR;
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('a');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('c');
        //+
        result = RegexParser.token().runParser(Buffer.builder().data("+".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.REPEAT;
        assert result.<Token>get(0).<Token.RepeatToken>getValue().getType() == RepeatType.SOME;
        //*
        result = RegexParser.token().runParser(Buffer.builder().data("*".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.REPEAT;
        assert result.<Token>get(0).<Token.RepeatToken>getValue().getType() == RepeatType.MANY;
        // {m,n}
        result = RegexParser.token().runParser(Buffer.builder().data("{1,2}".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.REPEAT;
        assert result.<Token>get(0).<Token.RepeatToken>getValue().getType() == RepeatType.RANGE;
        assert result.<Token>get(0).<Token.RepeatToken>getValue().<int []>getValue()[0] == 1;
        assert result.<Token>get(0).<Token.RepeatToken>getValue().<int []>getValue()[1] == 2;
        // {n}
        result = RegexParser.token().runParser(Buffer.builder().data("{3}".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.REPEAT;
        assert result.<Token>get(0).<Token.RepeatToken>getValue().getType() == RepeatType.REPEAT;
        assert result.<Token>get(0).<Token.RepeatToken>getValue().<Integer>getValue() == 3;
        // ?
        result = RegexParser.token().runParser(Buffer.builder().data("?".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.REPEAT;
        assert result.<Token>get(0).<Token.RepeatToken>getValue().getType() == RepeatType.OPTIONAL;
        // [A-z]
        result = RegexParser.token().runParser(Buffer.builder().data("[A-z]".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.VALID_CHAR;
        assert result.<Token>get(0).<Token.CharToken>getValue().getType() == CharType.SELECT;
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('A');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('B');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('Q');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('Z');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('a');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('b');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('q');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('z');
        // \n
        result = RegexParser.token().runParser(Buffer.builder().data("\\1".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.QUOTE;
        assert result.<Token>get(0).<Integer>getValue() == 1;
        // |
        result = RegexParser.token().runParser(Buffer.builder().data("|".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.OR;
    }

    @Test
    public void testTokenParser() {
        Result result = RegexParser.tokenParsers().runParser(Buffer.builder().data(".*a+b*".getBytes()).build());
        assert result.isSuccess();
        assert result.<Token>get(0).getType() == TokenType.VALID_CHAR;
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('a');
        assert result.<Token>get(0).<Token.CharToken>getValue().getPredicate().test('c');
        assert result.<Token>get(1).getType() == TokenType.REPEAT;
        assert result.<Token>get(1).<Token.RepeatToken>getValue().getType() == RepeatType.MANY;
        assert result.<Token>get(2).getType() == TokenType.VALID_CHAR;
        assert result.<Token>get(3).getType() == TokenType.REPEAT;
        assert result.<Token>get(3).<Token.RepeatToken>getValue().getType() == RepeatType.SOME;
        assert result.<Token>get(4).getType() == TokenType.VALID_CHAR;
        assert result.<Token>get(5).getType() == TokenType.REPEAT;
        assert result.<Token>get(5).<Token.RepeatToken>getValue().getType() == RepeatType.MANY;
    }

    @Test
    public void testCompile() {
        Parser parser = RegexParser.compile("^.*a+b*$");
        Optional<String> result = RegexParser.match(parser, "caacb");
        assert result.isPresent();
    }
}
