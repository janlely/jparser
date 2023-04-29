# jparser

# Parser Combinator written by java

Probably the most user-friendly parser in the Java language at present.

## Sample Usage: implement a CSV parser
* Parser a simple csv line:
```
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
```
* Parser a standard csv
see [CSVParser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/csv/CsvParser.java)

## Sample Usage: implement a Json parser
see [JsonPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/json/JsonParser.java)
