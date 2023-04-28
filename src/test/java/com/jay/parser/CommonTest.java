package com.jay.parser;

import org.jay.parser.Combinator;
import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.impl.json.JsonParser;
import org.jay.parser.parsers.CharParsers;
import org.jay.parser.parsers.NumberParsers;
import org.jay.parser.parsers.StringParsers;
import org.jay.parser.util.AnyChar;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.CharUtil;
import org.jay.parser.util.Mapper;
import org.jay.parser.util.Separator;
import org.junit.Test;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CommonTest {


    @Test
    public void testChar() throws Exception {
        byte[] bytes = "嗨𠀀".getBytes(StandardCharsets.UTF_8);
        AnyChar ch = CharUtil.read(bytes, StandardCharsets.UTF_8);
        System.out.println(CharUtil.length(ch));
    }

    @Test
    public void testChar2() throws CharacterCodingException {
        int len = String.valueOf('你').getBytes().length;
        System.out.println(len);
    }


    @Test
    public void testSkipChar() throws CharacterCodingException {
//        ByteBuffer bf = ByteBuffer.wrap("需要注意的是，如果使用".getBytes());
//        CoderResult result = StandardCharsets.UTF_8.newDecoder().decode(bf, CharBuffer.allocate(1), true);
//        while (!result.isError()) {
//            System.out.println(bf.remaining());
//            result = StandardCharsets.UTF_8.newDecoder().decode(bf, CharBuffer.allocate(1), true);
//        }
//        System.out.println("hello");
        List<AnyChar> result = CharUtil.readN("字符或字节".getBytes(), 6, StandardCharsets.UTF_8);
        System.out.println("sdfsd");
    }

    @Test
    public void testParser() {
        Parser wordParser = CharParsers.satisfy(c -> c >= 'A' && c <= 'z').many().map(Mapper.toStr());
        Parser sampleParser = wordParser.sepBy(Separator.character(',')).map(args ->
                Sample.builder()
                        .a((String) args.get(0))
                        .b((String) args.get(1))
                        .c((String) args.get(2))
                        .d((String) args.get(3))
                        .e((String) args.get(4))
                        .build());
        Parser csvFileParser = sampleParser.sepBy(Separator.character('\n'));
        Result result = csvFileParser.runParser(Context.builder()
                .buffer(Buffer.builder()
                        .pos(0)
                        .data("hello,world,abc,dfe,asdfasdf\nhello,world,abc,dfe,asdfasdf".getBytes())
                        .build())
                .build());
        System.out.println(result.getResult().get(0));
    }

    @Test
    public void testChoose() {
        Parser numberParser = NumberParsers.intStr(100);
        Parser stringParser = StringParsers.string("hello");
        Result result = Combinator.choose(numberParser, stringParser)
                .runParser(Context.builder()
                        .buffer(Buffer.builder()
                                .data("hello100hello".getBytes())
                                .build())
                        .build());
        System.out.println("hello");
    }

    @Test
    public void testJson() {
        String jsonStr = "  {\"hello\":\"world\",\"array\":[\"a\",{\"b\":\"c\"},null,123.4],\"name\":\"jay\"}  ";
        Result result = new JsonParser().runParser(Context.builder()
                .buffer(Buffer.builder()
                        .data(jsonStr.getBytes())
                        .build())
                .build());
        System.out.println("hello");
    }
}
