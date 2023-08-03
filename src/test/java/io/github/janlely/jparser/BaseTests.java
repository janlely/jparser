package io.github.janlely.jparser;

import io.github.janlely.jparser.parsers.ByteParsers;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.Buffer;
import io.github.janlely.jparser.util.Mapper;
import org.apache.commons.lang3.ArrayUtils;
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


        Result result3 = TextParsers.satisfy(Character::isLetter)
                .some().map(Mapper.toStr())
                .sepBy(TextParsers.one(',').ignore())
                .runParser(Buffer.builder()
                        .data("".getBytes())
                        .build());
        assert result3.isSuccess();
        assert result3.<String>get(0).equals("a");
        assert result3.<String>get(1).equals("b");
    }

    @Test
    public void testManyTill() {
        Result result = TextParsers.any().manyTill(TextParsers.string("abc"))
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("adcbgdueabc".getBytes()).build());
        assert result.<String>get().equals("adcbgdue");

        result = TextParsers.any().manyTill(TextParsers.string("abc"))
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("adcbgdueab".getBytes()).build());
        assert result.isError();

        result = TextParsers.any().manyTill(TextParsers.string("abc"))
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("abcbgdueab".getBytes()).build());
        assert result.isSuccess();
        assert result.<String>get().equals("");
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


    @Test
    public void testConnect() {
        Parser parser = TextParsers.one('a')
                .chain(() -> TextParsers.one('b'))
                .chain(() -> TextParsers.one('c'));
        Result result = parser.runParser(Buffer.builder().data("abcd".getBytes()).build());
        assert result.isSuccess();
        assert result.<Character>get(0).equals('a');
        assert result.<Character>get(1).equals('b');
        assert result.<Character>get(2).equals('c');
    }

    @Test
    public void testBtConnect() {
        Parser parser = TextParsers.any().many()
                .btChain(true, () -> TextParsers.one('a'))
                .map(Mapper.toStr());
        Result result = parser.runParser(Buffer.builder()
                .data("abcdab".getBytes())
                .build());
        assert result.isSuccess();
        assert result.<String>get(0).equals("abcda");
    }

    @Test
    public void testRepeatTill() {
        Result result = TextParsers.any().manyTill(TextParsers.string("abc"))
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("xxxxabc".getBytes()).build());
        assert result.<String>get().equals("xxxx");

        result = TextParsers.any().manyTill(TextParsers.string("abc"))
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("abcxxxxabc".getBytes()).build());
        assert result.<String>get().equals("");


        result = TextParsers.any().manyTill(TextParsers.string("abc"))
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("xxxxabdc".getBytes()).build());
        assert result.isError();

        result = TextParsers.any().someTill(() -> TextParsers.string("abc"))
                .map(Mapper.toStr())
                .runParser(Buffer.builder().data("abcxxxxabc".getBytes()).build());
        assert result.<String>get().equals("abcxxxx");
    }

    @Test
    public void testBytesTake() {
        Result result = ByteParsers.take(3)
                .runParser(Buffer.builder().data("xxxxabc".getBytes()).build());
        assert "xxx".equals(new String(result.<byte[]>get()));
    }
}
