package com.jay.parser;

import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.impl.json.JsonParser;
import org.jay.parser.parsers.NumberParsers;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.Mapper;
import org.junit.Test;

import java.nio.charset.CharacterCodingException;

public class CommonTest {


    @Test
    public void testChar2() throws CharacterCodingException {
        int len = String.valueOf('你').getBytes().length;
        System.out.println(len);
    }


    @Test
    public void testParser() {
        Parser wordParser = TextParsers.satisfy(c -> c >= 'A' && c <= 'z').many().map(Mapper.toStr());
        Parser sampleParser = wordParser.sepBy(TextParsers.one(',').ignore()).map(args ->
                Sample.builder()
                        .a((String) args.get(0))
                        .b((String) args.get(1))
                        .c((String) args.get(2))
                        .d((String) args.get(3))
                        .e((String) args.get(4))
                        .build());
        Parser csvFileParser = sampleParser.sepBy(TextParsers.one('\n').ignore());
        Result result = csvFileParser.runParser(Buffer.builder()
                        .pos(0)
                        .data("hello,world,abc,dfe,asdfasdf\nhello,world,abc,dfe,asdfasdf".getBytes())
                        .build());
        System.out.println(result.getResult().get(0));
    }

    @Test
    public void testChoose() {
        Parser numberParser = NumberParsers.intStr(100);
        Parser stringParser = TextParsers.string("hello");
        Result result = numberParser.or(() -> stringParser)
                .runParser(Buffer.builder()
                                .data("hello100hello".getBytes())
                                .build());
        System.out.println("hello");
    }

    @Test
    public void testJson() {
        String jsonStr = "  {\"hello\":\"world\",\"array\":[\"a\",{\"b\":\"c\"},null,123.4],\"name\":\"jay\"}  ";
        Result result = new JsonParser().runParser(Buffer.builder()
                        .data(jsonStr.getBytes())
                        .build());
        System.out.println("hello");
    }

    @Test
    public void testSimpleCsv() {
        Parser csvLineParser = TextParsers.satisfy(c -> Character.isLetterOrDigit(c))
                .many().map(Mapper.toStr())
                .sepBy(TextParsers.one(',').ignore());
        Result result = csvLineParser.runParser(Buffer.builder()
                .data("field1,field2,field3,field4".getBytes())
                .build());
        assert result.<String>get(0).equals("field1");
        assert result.<String>get(1).equals("field2");
        assert result.<String>get(2).equals("field3");
        assert result.<String>get(3).equals("field4");
    }
}
