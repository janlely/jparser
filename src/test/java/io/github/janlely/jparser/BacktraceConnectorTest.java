package io.github.janlely.jparser;

import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.Result;
import io.github.janlely.jparser.comb.BacktraceParser;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.Buffer;
import io.github.janlely.jparser.util.Mapper;
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
