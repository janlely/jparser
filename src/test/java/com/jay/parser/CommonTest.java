package com.jay.parser;

import org.jay.parser.Constructor;
import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.CharParsers;
import org.jay.parser.util.AnyChar;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.CharUtil;
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
    public void testParser() {
        Parser wordParser = CharParsers.satisfy(c -> c >= 'A' && c <= 'z');
        Result result = wordParser.sepBy(Separator.character(','))
                .pack(new Constructor() {
                    @Override
                    public Object construct(List args) {
                        return Sample.builder()
                                .a((String) args.get(0))
                                .b((String) args.get(1))
                                .c((String) args.get(2))
                                .d((String) args.get(3))
                                .e((String) args.get(4))
                                .build();
                    }
                    @Override
                    public String getDesc() {
                        return "pack csv to Java Object";
                    }
                })
                .runParser(Context.builder()
                .buffer(Buffer.builder()
                        .pos(0)
                        .data("hello,world,abc,dfe,asdfasdf".getBytes())
                        .build())
                .build());
        System.out.println(result.getResult().get(0));
    }
}
