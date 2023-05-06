# jparser

# Parser Combinater implemented in Java

Probably the most user-friendly parser in the Java language at present.
Unlike [java-petitparser](https://github.com/petitparser/java-petitparser), jparser is capable of parsing bytes and having more features".

## basic parsers
```java
ByteParsers::satisfy //Parse a byte based on a condition.
ByteParsers::any //Parse any byte
ByteParsers::one //Parse a specified byte
ByteParsers::take //Parse N arbitrary bytes.
ByteParsers::skip //skip N arbitrary bytes.
ByteParsers::takeWhile //Parse any number of bytes that meet a certain condition.
ByteParsers::skipWhile //skip any number of bytes that meet a certain condition.

TextParsers::satisfy //Parse a character that satisfies a condition according to the given encoding.
TextParsers::one //Parse a character according to the given encoding
TextParsers::string //Parse a given string according to the given encoding.
TextParsers::any //Parse any n characters according to the specified encoding.
TextParsers::take //Parse any n characters according to the specified encoding.
TextParsers::takeWhile //Parse characters that satisfy a condition according to the given encoding and return a string.
TextParsers::skip //Skip n characters of the given encoding.
TextParsers::skipWhile //Skip characters that satisfy a condition according to the given encoding
TextParsers::eof //end of line
TextParsers::space //Parse one whitespace character, excluding newline characters.
TextParsers::spaces //Parse whitespace characters, excluding newline characters.
TextParsers::white //Parse one whitespace character
TextParsers::whites //Parse whitespace characters

NumberParsers::intStr //Parse a specified integer encoded as a string.
NumberParsers::anyIntStr //Parse a any integer encoded as a string.
NumberParsers::intLE //Parse a specified integer encoded in little-endian format
NumberParsers::intBE //Parse a specified integer encoded in big-endian format
NumberParsers::longLE //Parse a specified long integer encoded in little-endian format
NumberParsers::longBE //Parse a specified long integer encoded in big-endian format
NumberParsers::anyIntLE //Parse any integer encoded in little-endian format
NumberParsers::anyIntBE //Parse any integer encoded in big-endian format
NumberParsers::anyLongLE //Parse any long integer encoded in little-endian format
NumberParsers::andLongBE //Parse any long integer encoded in big-endian format
```
## usefull combinaters
```java
Parser::concat //concatenate another parser
Parser::btConcat //concatenate another parser with backtrace enabled
Parser::concatWith //concatenate another parser generator(res -> Parser)
Parser::or //if this parser failed than try another
Parser::ignore //do parse, but ignore result
Parser::map //do parse and map the result
Parser::sepBy //parse multiply times seperate by another parser
Parser::scan //Continuously reduce the input and attempt to parse it.
Parser::optinal //make this parser optional
Parser::trim //Ignore leading and trailing whitespace and attempt to parse.
Parser::many //parse zero or more times
Parser::some //parse one or more times
Parser::repeat //parse n times
Parser::range //parse bettwen m and n times
Parser::attempt //parse zero to n times
Parser::must //Add a predicate on the result.
```


## hello world
```java
    @Test
    public void testHelloWorld() {
        String date = "2023-05-01";
        Parser dateParser = NumberParsers.anyIntStr()
                .concat(() -> TextParsers.one('-').ignore())
                .concat(() -> NumberParsers.anyIntStr())
                .concat(() -> TextParsers.one('-').ignore())
                .concat(() -> NumberParsers.anyIntStr())
                .concat(() -> TextParsers.eof());
        Result result = dateParser.runParser(Buffer.builder().data(date.getBytes()).build());
        assert result.<Integer>get(0) == 2023;
        assert result.<Integer>get(1) == 5;
        assert result.<Integer>get(2) == 1;
    }
```

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
see [CSVParser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/csv/CsvParser.java)

## Sample Usage: implement a Json parser
```java
    String source1 = "  {\"hello\":\"world\",\"array\":[\"a\",{\"b\":\"c\"},null,123.4],\"name\":\"jay\"}  ";
    Result result1 = JsonParser.parser().runParser(Buffer.builder()
            .data(source1.getBytes())
            .build());
    assert result1.isSuccess();
```
see [JsonPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/json/JsonParser.java)


## Sample Usage: implement a XML parser
```java
    @Test
    public void testNode() {
        String src = "<note hello=\"world\">\n" +
                       "<to>Tove</to>\n" +
                       "<from>Jani</from>\n" +
                       "<heading>Reminder</heading>\n" +
                       "<body>Don't forget me this weekend!</body>\n" +
                     "</note>";
        Result result1 = XmlParser.parser().runParser(Buffer.builder()
                .data(src.getBytes())
                .build());
        assert result1.isSuccess();
    }
```
see [XmlPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/xml/XmlParser.java)


## Sample Usage: implement a Regex parser
```java

    @Test
    public void testMatch() {
        RegexParser regexParser = new RegexParser();
        regexParser.compile("(a+)(b+)c?$");
        Optional<String> result = regexParser.match("aaaabbbbc");
        assert result.isPresent();
    }

    @Test
    public void testGroup() {
        RegexParser regexParser = new RegexParser();
        regexParser.compile("(a+)b\\1");
        Optional<String> result = regexParser.match("aabaa");
        assert result.isPresent();
        List<String> searchResult = regexParser.search("aaabaaa");
        assert searchResult.size() == 2;
    }
```
see [RegexPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/regex/RegexParser.java)


