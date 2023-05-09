package com.jay.parser;

import org.jay.parser.Result;
import org.jay.parser.impl.regex.RParser;
import org.jay.parser.impl.regex.RegexParser;
import org.jay.parser.util.Buffer;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class RegexParserTests {

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
    public void testMatch() {
        RegexParser regexParser = new RegexParser();
        regexParser.compile("(a+)(b+)c?$");
        Optional<String> result = regexParser.match("aaaabbbbc");
        assert result.isPresent();
        result = regexParser.match("ab");
        assert result.isPresent();
        result = regexParser.match("aabbc");
        assert result.isPresent();
        result = regexParser.match("aabbcc");
        assert result.isEmpty();

        regexParser.compile("^a+b+c$");
        result = regexParser.match("aabbc");
        assert result.isPresent();
        result = regexParser.match("aabbcc");
        assert result.isEmpty();
        result = regexParser.match("abc");
        assert result.isPresent();
        result = regexParser.match("aaaabc");
        assert result.isPresent();

        regexParser.compile(".*abc$");
        result = regexParser.match("abcabc");
        assert result.isPresent();
        result = regexParser.match("abc");
        assert result.isPresent();
        result = regexParser.match("abcd");
        assert result.isEmpty();

        regexParser.compile(".*(abc)+d");
        result = regexParser.match("aaaabcabcd");
        assert result.isPresent();
        result = regexParser.match("aaaabcabce");
        assert result.isEmpty();
    }

    @Test
    public void testGroup() {
        RegexParser regexParser = new RegexParser();
        regexParser.compile("(a+)b\\1");
        Optional<String> result1 = regexParser.match("aabaa");
        assert result1.isPresent();
        List<String> searchResult1 = regexParser.search("aaabaaa");
        assert searchResult1.size() == 2;


        regexParser.compile("(a+)(b+)\\1\\2");
        Optional<String> result2 = regexParser.match("aabbaabb");
        assert result2.isPresent();
        List<String> searchResult2 = regexParser.search("aabbaabb");
        assert searchResult2.size() == 3;
        assert searchResult2.get(0).equals("aabbaabb");
        assert searchResult2.get(1).equals("aa");
        assert searchResult2.get(2).equals("bb");

    }

    @Test
    public void testQuote() {

        RegexParser regexParser = new RegexParser();
        regexParser.compile("(a+)(b+)\\1+\\2");
        Optional<String> result = regexParser.match("aabbaaaaaabb");
        assert result.isPresent();
    }

}
