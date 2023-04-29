package com.jay.parser;

import org.jay.parser.Result;
import org.jay.parser.impl.csv.CsvParser;
import org.jay.parser.util.Buffer;
import org.junit.Test;

public class CSVParserTests {

    @Test
    public void testLine() {
        String filed = "\"hello\"\"world\"";
        Result result1 = CsvParser.fieldParser().runParser(Buffer.builder().data(filed.getBytes()).build());
        assert result1.isSuccess();
        assert result1.<String>get(0).equals("hello\"world");
        String src = "\"hello\"\"world\",\"a.bd,dd\",field3,\"field4\"\"\"";
        Result result2 = CsvParser.lineParser().runParser(Buffer.builder().data(src.getBytes()).build());
        assert result2.isSuccess();
        assert result2.<String>get(0).equals("hello\"world");
        assert result2.<String>get(1).equals("a.bd,dd");
        assert result2.<String>get(2).equals("field3");
        assert result2.<String>get(3).equals("field4\"");
    }
}
