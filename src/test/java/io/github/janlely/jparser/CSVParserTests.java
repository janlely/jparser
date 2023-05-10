package io.github.janlely.jparser;

import io.github.janlely.jparser.Result;
import io.github.janlely.jparser.impl.csv.CsvParser;
import io.github.janlely.jparser.util.Buffer;
import org.junit.Test;

public class CSVParserTests {

    @Test
    public void testFeild() {
        Result result1 = CsvParser.fieldCase1().runParser(Buffer.builder()
                        .data("\"hello\"\"world\"".getBytes())
                .build());
        assert result1.isSuccess();
        assert result1.<String>get(0).equals("hello\"world");

        Result result2 = CsvParser.fieldCase2().runParser(Buffer.builder()
                .data("hello\"\"world".getBytes())
                .build());
        assert result2.isSuccess();
        assert result2.<String>get(0).equals("hello\"\"world");

        Result result3 = CsvParser.field().runParser(Buffer.builder()
                .data("hello\"\"world".getBytes())
                .build());
        assert result3.isSuccess();
        assert result3.<String>get(0).equals("hello\"\"world");


        Result result4 = CsvParser.field().runParser(Buffer.builder()
                .data("\"hello\"\"world\"".getBytes())
                .build());
        assert result4.isSuccess();
        assert result4.<String>get(0).equals("hello\"world");

    }


    @Test
    public void testLine() {
        String src1 = "a,b";
        Result result1 = CsvParser.lineParser().runParser(Buffer.builder().data(src1.getBytes()).build());
        assert result1.isSuccess();
        String src2 = "\"hello\"\"world\",\"a.bd,dd\",field3,\"field4\"\"\"";
        Result result2 = CsvParser.lineParser().runParser(Buffer.builder().data(src2.getBytes()).build());
        assert result2.isSuccess();
        assert result2.<String>get(0).equals("hello\"world");
        assert result2.<String>get(1).equals("a.bd,dd");
        assert result2.<String>get(2).equals("field3");
        assert result2.<String>get(3).equals("field4\"");
    }
}
