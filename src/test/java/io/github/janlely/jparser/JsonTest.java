package io.github.janlely.jparser;

import io.github.janlely.jparser.impl.json.JsonParser;
import io.github.janlely.jparser.util.Buffer;
import org.junit.Test;

public class JsonTest {

    @Test
    public void testKeyParser() {
        String src = "\"hello\"";
        Result result1 = JsonParser.keyParser().trim(true).runParser(Buffer.builder().data(src.getBytes()).build());
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

    @Test
    public void testPerformance() {
        String src = "[\n" +
                "\t{\n" +
                "\t\t\"id\": \"0001\",\n" +
                "\t\t\"type\": \"donut\",\n" +
                "\t\t\"name\": \"Cake\",\n" +
                "\t\t\"ppu\": 0.55,\n" +
                "\t\t\"batters\":\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"batter\":\n" +
                "\t\t\t\t\t[\n" +
                "\t\t\t\t\t\t{ \"id\": \"1001\", \"type\": \"Regular\" },\n" +
                "\t\t\t\t\t\t{ \"id\": \"1002\", \"type\": \"Chocolate\" },\n" +
                "\t\t\t\t\t\t{ \"id\": \"1003\", \"type\": \"Blueberry\" },\n" +
                "\t\t\t\t\t\t{ \"id\": \"1004\", \"type\": \"Devil's Food\" }\n" +
                "\t\t\t\t\t]\n" +
                "\t\t\t},\n" +
                "\t\t\"topping\":\n" +
                "\t\t\t[\n" +
                "\t\t\t\t{ \"id\": \"5001\", \"type\": \"None\" },\n" +
                "\t\t\t\t{ \"id\": \"5002\", \"type\": \"Glazed\" },\n" +
                "\t\t\t\t{ \"id\": \"5005\", \"type\": \"Sugar\" },\n" +
                "\t\t\t\t{ \"id\": \"5007\", \"type\": \"Powdered Sugar\" },\n" +
                "\t\t\t\t{ \"id\": \"5006\", \"type\": \"Chocolate with Sprinkles\" },\n" +
                "\t\t\t\t{ \"id\": \"5003\", \"type\": \"Chocolate\" },\n" +
                "\t\t\t\t{ \"id\": \"5004\", \"type\": \"Maple\" }\n" +
                "\t\t\t]\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"id\": \"0002\",\n" +
                "\t\t\"type\": \"donut\",\n" +
                "\t\t\"name\": \"Raised\",\n" +
                "\t\t\"ppu\": 0.55,\n" +
                "\t\t\"batters\":\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"batter\":\n" +
                "\t\t\t\t\t[\n" +
                "\t\t\t\t\t\t{ \"id\": \"1001\", \"type\": \"Regular\" }\n" +
                "\t\t\t\t\t]\n" +
                "\t\t\t},\n" +
                "\t\t\"topping\":\n" +
                "\t\t\t[\n" +
                "\t\t\t\t{ \"id\": \"5001\", \"type\": \"None\" },\n" +
                "\t\t\t\t{ \"id\": \"5002\", \"type\": \"Glazed\" },\n" +
                "\t\t\t\t{ \"id\": \"5005\", \"type\": \"Sugar\" },\n" +
                "\t\t\t\t{ \"id\": \"5003\", \"type\": \"Chocolate\" },\n" +
                "\t\t\t\t{ \"id\": \"5004\", \"type\": \"Maple\" }\n" +
                "\t\t\t]\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"id\": \"0003\",\n" +
                "\t\t\"type\": \"donut\",\n" +
                "\t\t\"name\": \"Old Fashioned\",\n" +
                "\t\t\"ppu\": 0.55,\n" +
                "\t\t\"batters\":\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"batter\":\n" +
                "\t\t\t\t\t[\n" +
                "\t\t\t\t\t\t{ \"id\": \"1001\", \"type\": \"Regular\" },\n" +
                "\t\t\t\t\t\t{ \"id\": \"1002\", \"type\": \"Chocolate\" }\n" +
                "\t\t\t\t\t]\n" +
                "\t\t\t},\n" +
                "\t\t\"topping\":\n" +
                "\t\t\t[\n" +
                "\t\t\t\t{ \"id\": \"5001\", \"type\": \"None\" },\n" +
                "\t\t\t\t{ \"id\": \"5002\", \"type\": \"Glazed\" },\n" +
                "\t\t\t\t{ \"id\": \"5003\", \"type\": \"Chocolate\" },\n" +
                "\t\t\t\t{ \"id\": \"5004\", \"type\": \"Maple\" }\n" +
                "\t\t\t]\n" +
                "\t}\n" +
                "]";
        Parser parser = JsonParser.parser();
        long start = System.currentTimeMillis();
        for(int i = 0; i < 10000; i++) {
            Result result = parser.runParser(Buffer.builder().data(src.getBytes()).build());
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);

    }

}
