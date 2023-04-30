# jparser

# Parser Combinator written by java

Probably the most user-friendly parser in the Java language at present.
Unlike [java-petitparser](https://github.com/petitparser/java-petitparser), jparser is capable of parsing bytes.

## Sample Usage: implement a CSV parser
* Parser a simple csv line:
```java
    @Test
    public void testSimpleCsv() {
        Parser csvLineParser = TextParsers.satisfy(Character::isLetterOrDigit)
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
```java
    public static Parser jsonParser() {
        return stringParser()
                .or(() -> objectParser().trim(true))
                .or(() -> arrayParser().trim(true))
                .or(() -> nullParser().trim(true))
                .or(() -> boolParser().trim(true))
                .or(() -> numberParser().trim(true))
                .trim(true);
    }
```
see [CSVParser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/csv/CsvParser.java)

## Sample Usage: implement a Json parser
see [JsonPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/json/JsonParser.java)


## Sample Usage: implement a XML parser
see [XmlPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/xml/XmlParser.java)


## Sample Usage: implement a Regex parser
//TODO


