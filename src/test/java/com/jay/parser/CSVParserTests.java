package com.jay.parser;

import org.jay.parser.Result;
import org.jay.parser.impl.csv.CsvParser;
import org.jay.parser.util.Buffer;
import org.junit.Test;

public class CSVParserTests {

    @Test
    public void testLine() {
//        String filed = "\"hello\"\"world\"";
//        Result result1 = CsvParser.fieldParser().runParser(Buffer.builder().data(filed.getBytes()).build());
//        assert result1.isSuccess();
//        String src = "\"hello\"\"world\",\"a.bd,dd\",field3,\"filed4\"\"\"";
        String src = "\"filed4\"\"addh\"";
        Result result = CsvParser.lineParser().runParser(Buffer.builder().data(src.getBytes()).build());
        assert result.isSuccess();
    }
}
