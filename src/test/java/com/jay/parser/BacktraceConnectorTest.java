package com.jay.parser;

import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.comb.BacktraceParser;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.Mapper;
import org.junit.Test;

public class BacktraceConnectorTest {

    @Test
    public void testNonGreedy() {
        Parser parser = new BacktraceParser(false, () -> TextParsers.any().many(), () -> TextParsers.one('a'))
                .map(Mapper.toStr());
        Result result = parser.runParser(Buffer.builder().data("abcda".getBytes()).build());
        assert result.isSuccess();
        assert result.<String>get(0).equals("a");
    }

    @Test
    public void testGreedy() {
        Parser parser = new BacktraceParser(true, () -> TextParsers.string("aa").many(), () -> TextParsers.one('a'))
                .map(Mapper.toStr());
        Result result = parser.runParser(Buffer.builder().data("aaaaa".getBytes()).build());
        assert result.isSuccess();
        assert result.<String>get(0).equals("aaaaa");
    }
}
