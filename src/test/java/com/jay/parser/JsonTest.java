package com.jay.parser;

import org.jay.parser.Result;
import org.jay.parser.impl.json.JsonParser;
import org.jay.parser.util.Buffer;
import org.junit.Test;

public class JsonTest {

    @Test
    public void test() {
        String source1 = "  {\"hello\":\"world\",\"array\":[\"a\",{\"b\":\"c\"},null,123.4],\"name\":\"jay\"}  ";
        Result result1 = new JsonParser().runParser(Buffer.builder()
                .data(source1.getBytes())
                .build());
        assert result1.isSuccess();

        String source2 = "{\"a\":\"b\"} a";
        Result result2 = new JsonParser().runParser(Buffer.builder()
                .data(source2.getBytes())
                .build());
        assert result2.isError();

        String source3 = "{\"a\":\"b\", \"c\":false}";
        Result result3 = new JsonParser().runParser(Buffer.builder()
                .data(source3.getBytes())
                .build());
        assert result2.isError();
    }

}
