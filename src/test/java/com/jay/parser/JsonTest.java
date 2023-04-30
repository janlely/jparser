package com.jay.parser;

import org.jay.parser.Result;
import org.jay.parser.impl.json.JsonParser;
import org.jay.parser.util.Buffer;
import org.junit.Test;

public class JsonTest {

    @Test
    public void testKeyParser() {
        String src = "\"hello\"";
        Result result1 = JsonParser.keyParser().trim().runParser(Buffer.builder().data(src.getBytes()).build());
        assert result1.isSuccess();
    }

    @Test
    public void testStringParser() {
        String src = "\"hello\"";
        Result result1 = JsonParser.stringParser().runParser(Buffer.builder().data(src.getBytes()).build());
        assert result1.isSuccess();
    }

    @Test
    public void testMember() {
        String src = "  \"hello\":\"world\"  ";
        Result result1 = JsonParser.memberParser().runParser(Buffer.builder()
                .data(src.getBytes())
                .build());
        assert result1.isSuccess();
    }

    @Test
    public void testMembers() {
        String src = "  \"hello\":\"world\"  ";
        Result result1 = JsonParser.membersParser().runParser(Buffer.builder()
                .data(src.getBytes())
                .build());
        assert result1.isSuccess();
    }

    @Test
    public void testObject() {
        String source1 = "{\"h\":\"w\"}";
//        String source1 = "\"a\"";
        Result result1 = JsonParser.objectParser()
                .runParser(Buffer.builder()
                .data(source1.getBytes())
                .build());
        assert result1.isSuccess();
    }


    @Test
    public void test() {


        String source1 = "  {\"hello\":\"world\",\"array\":[\"a\",{\"b\":\"c\"},null,123.4],\"name\":\"jay\"}  ";
        Result result1 = JsonParser.parser().runParser(Buffer.builder()
                .data(source1.getBytes())
                .build());
        assert result1.isSuccess();

        String source2 = "{\"a\":\"b\"} a";
        Result result2 = JsonParser.parser().runParser(Buffer.builder()
                .data(source2.getBytes())
                .build());
        assert result2.isError();

        String source3 = "{\"a\":\"b\", \"c\":false}";
        Result result3 = JsonParser.parser().runParser(Buffer.builder()
                .data(source3.getBytes())
                .build());
        assert result2.isError();
    }

}
